import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

admin.initializeApp();

// Send notification when new order is created
export const notifyNewOrder = functions.database.ref('/orders/{orderId}')
  .onCreate(async (snapshot, context) => {
    const order = snapshot.val();
    const orderId = context.params.orderId;
    
    try {
      // Get admin and employee FCM tokens
      const tokensSnapshot = await admin.database()
        .ref('fcm_tokens')
        .orderByChild('role')
        .equalTo('ADMIN')
        .once('value');
      
      const employeeTokensSnapshot = await admin.database()
        .ref('fcm_tokens')
        .orderByChild('role')
        .equalTo('EMPLOYEE')
        .once('value');
      
      const tokens: string[] = [];
      
      // Collect admin tokens
      tokensSnapshot.forEach(child => {
        const tokenData = child.val();
        if (tokenData && tokenData.token) {
          tokens.push(tokenData.token);
        }
      });
      
      // Collect employee tokens
      employeeTokensSnapshot.forEach(child => {
        const tokenData = child.val();
        if (tokenData && tokenData.token) {
          tokens.push(tokenData.token);
        }
      });
      
      if (tokens.length > 0) {
        const message = {
          notification: {
            title: '🛒 New Order Received',
            body: `Order #${orderId} from ${order.customerUsername} - $${order.totalAmount}`
          },
          data: {
            type: 'NEW_ORDER',
            orderId: orderId,
            role: 'ADMIN,EMPLOYEE'
          }
        };
        
        await admin.messaging().sendToDevice(tokens, message);
        
        // Log notification
        await admin.database().ref('notification_logs').push({
          type: 'NEW_ORDER',
          orderId: orderId,
          recipientCount: tokens.length,
          timestamp: admin.database.ServerValue.TIMESTAMP,
          status: 'sent'
        });
      }
    } catch (error) {
      console.error('Error sending new order notification:', error);
    }
  });

// Send notification when order status is updated
export const notifyOrderStatusUpdate = functions.database.ref('/orders/{orderId}/status')
  .onUpdate(async (change, context) => {
    const newStatus = change.after.val();
    const orderId = context.params.orderId;
    
    try {
      // Get the full order to find customer
      const orderSnapshot = await admin.database()
        .ref(`/orders/${orderId}`)
        .once('value');
      
      const order = orderSnapshot.val();
      if (!order) return;
      
      // Get customer FCM token
      const customerTokenSnapshot = await admin.database()
        .ref('fcm_tokens')
        .orderByChild('userId')
        .equalTo(order.customerId.toString())
        .once('value');
      
      const tokens: string[] = [];
      customerTokenSnapshot.forEach(child => {
        const tokenData = child.val();
        if (tokenData && tokenData.token) {
          tokens.push(tokenData.token);
        }
      });
      
      if (tokens.length > 0) {
        const statusMessages: { [key: string]: string } = {
          'CONFIRMED': 'Your order has been confirmed! 🎉',
          'PROCESSING': 'Your order is being processed 📦',
          'SHIPPED': 'Your order has been shipped! 🚚',
          'DELIVERED': 'Your order has been delivered! ✅',
          'CANCELLED': 'Your order has been cancelled ❌',
          'RETURNED': 'Your return has been processed 🔄'
        };
        
        const message = {
          notification: {
            title: '📦 Order Update',
            body: statusMessages[newStatus] || `Order status: ${newStatus}`
          },
          data: {
            type: 'ORDER_STATUS_UPDATE',
            orderId: orderId,
            newStatus: newStatus,
            role: 'CUSTOMER'
          }
        };
        
        await admin.messaging().sendToDevice(tokens, message);
        
        // Log notification
        await admin.database().ref('notification_logs').push({
          type: 'ORDER_STATUS_UPDATE',
          orderId: orderId,
          customerId: order.customerId,
          newStatus: newStatus,
          recipientCount: tokens.length,
          timestamp: admin.database.ServerValue.TIMESTAMP,
          status: 'sent'
        });
      }
    } catch (error) {
      console.error('Error sending order status update notification:', error);
    }
  });

// Send notification when new support message is received
export const notifySupportMessage = functions.database.ref('/support_messages/{messageId}')
  .onCreate(async (snapshot, context) => {
    const message = snapshot.val();
    const messageId = context.params.messageId;
    
    try {
      let targetRole = '';
      let tokens: string[] = [];
      
      if (message.isFromCustomer) {
        // Customer sent message - notify employees
        targetRole = 'EMPLOYEE';
        const employeeTokensSnapshot = await admin.database()
          .ref('fcm_tokens')
          .orderByChild('role')
          .equalTo('EMPLOYEE')
          .once('value');
        
        employeeTokensSnapshot.forEach(child => {
          const tokenData = child.val();
          if (tokenData && tokenData.token) {
            tokens.push(tokenData.token);
          }
        });
      } else {
        // Employee sent message - notify customer
        targetRole = 'CUSTOMER';
        const customerTokenSnapshot = await admin.database()
          .ref('fcm_tokens')
          .orderByChild('userId')
          .equalTo(message.customerId.toString())
          .once('value');
        
        customerTokenSnapshot.forEach(child => {
          const tokenData = child.val();
          if (tokenData && tokenData.token) {
            tokens.push(tokenData.token);
          }
        });
      }
      
      if (tokens.length > 0) {
        const notificationMessage = {
          notification: {
            title: message.isFromCustomer ? 
              `💬 New message from ${message.customerName}` : 
              '💬 Support team replied',
            body: message.message.length > 100 ? 
              message.message.substring(0, 100) + '...' : 
              message.message
          },
          data: {
            type: message.isFromCustomer ? 'CUSTOMER_CHAT' : 'SUPPORT_CHAT',
            chatId: message.chatId,
            messageId: messageId,
            role: targetRole
          }
        };
        
        await admin.messaging().sendToDevice(tokens, notificationMessage);
        
        // Log notification
        await admin.database().ref('notification_logs').push({
          type: message.isFromCustomer ? 'CUSTOMER_CHAT' : 'SUPPORT_CHAT',
          chatId: message.chatId,
          messageId: messageId,
          recipientCount: tokens.length,
          timestamp: admin.database.ServerValue.TIMESTAMP,
          status: 'sent'
        });
      }
    } catch (error) {
      console.error('Error sending support message notification:', error);
    }
  });

// Check for low stock and notify admins (scheduled function)
export const checkLowStock = functions.pubsub.schedule('every 1 hours').onRun(async (context) => {
  try {
    const productsSnapshot = await admin.database().ref('/products').once('value');
    const products = productsSnapshot.val();
    
    if (!products) return;
    
    const lowStockProducts: any[] = [];
    Object.keys(products).forEach(productId => {
      const product = products[productId];
      if (product.quantity <= 5) { // Low stock threshold
        lowStockProducts.push({
          id: productId,
          name: product.name,
          quantity: product.quantity
        });
      }
    });
    
    if (lowStockProducts.length > 0) {
      // Get admin FCM tokens
      const adminTokensSnapshot = await admin.database()
        .ref('fcm_tokens')
        .orderByChild('role')
        .equalTo('ADMIN')
        .once('value');
      
      const tokens: string[] = [];
      adminTokensSnapshot.forEach(child => {
        const tokenData = child.val();
        if (tokenData && tokenData.token) {
          tokens.push(tokenData.token);
        }
      });
      
      if (tokens.length > 0) {
        const message = {
          notification: {
            title: '⚠️ Low Stock Alert',
            body: `${lowStockProducts.length} products are running low on stock`
          },
          data: {
            type: 'LOW_STOCK',
            productCount: lowStockProducts.length.toString(),
            role: 'ADMIN'
          }
        };
        
        await admin.messaging().sendToDevice(tokens, message);
        
        // Log notification
        await admin.database().ref('notification_logs').push({
          type: 'LOW_STOCK',
          productCount: lowStockProducts.length,
          products: lowStockProducts,
          recipientCount: tokens.length,
          timestamp: admin.database.ServerValue.TIMESTAMP,
          status: 'sent'
        });
      }
    }
  } catch (error) {
    console.error('Error checking low stock:', error);
  }
});
