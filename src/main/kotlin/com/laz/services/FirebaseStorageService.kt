package com.laz.services

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Firebase Storage Service
 * Handles image uploads and downloads for the LAZ Store app
 */
class FirebaseStorageService {
    
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    /**
     * Upload product image to Firebase Storage
     * @param imageUri Local image URI to upload
     * @param productId Product ID for organizing images
     * @return Result containing the download URL or error
     */
    suspend fun uploadProductImage(imageUri: Uri, productId: Long): Result<String> {
        return try {
            println("DEBUG: Starting image upload for product $productId")
            println("DEBUG: Image URI: $imageUri")
            
            // Create a unique filename for the image
            val imageId = UUID.randomUUID().toString()
            val imagePath = "products/$productId/$imageId.jpg"
            val imageRef = storageRef.child(imagePath)
            
            println("DEBUG: Upload path: $imagePath")
            println("DEBUG: Storage reference: ${imageRef.path}")
            
            // Upload the image
            println("DEBUG: Starting file upload...")
            val uploadTask = imageRef.putFile(imageUri).await()
            println("DEBUG: Upload task completed: ${uploadTask.metadata?.path}")
            
            // Get the download URL
            println("DEBUG: Getting download URL...")
            val downloadUrl = imageRef.downloadUrl.await()
            println("DEBUG: Download URL obtained: $downloadUrl")
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            println("DEBUG: Upload failed with exception: ${e.message}")
            println("DEBUG: Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Upload product image with custom filename
     * @param imageUri Local image URI to upload
     * @param fileName Custom filename for the image
     * @return Result containing the download URL or error
     */
    suspend fun uploadProductImageWithName(imageUri: Uri, fileName: String): Result<String> {
        return try {
            val imageRef = storageRef.child("products/$fileName")
            
            // Upload the image
            val uploadTask = imageRef.putFile(imageUri).await()
            
            // Get the download URL
            val downloadUrl = imageRef.downloadUrl.await()
            
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete product image from Firebase Storage
     * @param imageUrl The download URL of the image to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteProductImage(imageUrl: String): Result<Unit> {
        return try {
            val imageRef = storage.getReferenceFromUrl(imageUrl)
            imageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get storage reference for a product image
     * @param productId Product ID
     * @param imageId Image ID
     * @return StorageReference for the image
     */
    fun getProductImageRef(productId: Long, imageId: String): StorageReference {
        return storageRef.child("products/$productId/$imageId.jpg")
    }
    
    /**
     * Generate a unique image filename
     * @param productId Product ID
     * @param originalName Original filename (optional)
     * @return Unique filename for the image
     */
    fun generateImageFileName(productId: Long, originalName: String? = null): String {
        val timestamp = System.currentTimeMillis()
        val extension = originalName?.substringAfterLast('.', "jpg") ?: "jpg"
        return "product_${productId}_${timestamp}.$extension"
    }
}
