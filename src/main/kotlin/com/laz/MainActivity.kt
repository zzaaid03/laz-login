package com.laz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laz.database.LazDatabase
import com.laz.ui.LazStoreApp
import com.laz.ui.theme.LazTheme
import com.laz.viewmodels.*
import com.laz.ui.ChatScreen

class MainActivity : ComponentActivity() {
    
    private val database by lazy { LazDatabase.getDatabase(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            LazTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen() // <- add this line just to test it

                    // Create ViewModels with database DAOs
                    val userViewModel: UserViewModel = viewModel {
                        UserViewModel(database.userDao())
                    }
                    val productViewModel: ProductViewModel = viewModel {
                        ProductViewModel(database.productDao())
                    }
                    val salesViewModel: SalesViewModel = viewModel {
                        SalesViewModel(database.saleDao(), database.productDao())
                    }
                    val returnsViewModel: ReturnsViewModel = viewModel {
                        ReturnsViewModel(database.returnDao(), database.saleDao(), database.productDao())
                    }
                    
                    LazStoreApp(
                        userViewModel = userViewModel,
                        productViewModel = productViewModel,
                        salesViewModel = salesViewModel,
                        returnsViewModel = returnsViewModel
                    )
                }
            }
        }
    }

}
