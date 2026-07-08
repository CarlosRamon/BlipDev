package br.com.bliqbrasil.totem

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import br.com.bliqbrasil.totem.ui.navigation.AppDestinations
import br.com.bliqbrasil.totem.ui.navigation.NavGraph
import br.com.bliqbrasil.totem.ui.theme.BliqTotemTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars()

        val app = application as BliqTotemApp

        setContent {
            BliqTotemTheme {
                val navController = rememberNavController()

                LaunchedEffect(Unit) {
                    app.networkClient.authInterceptor.unauthorizedEvent.collect {
                        navController.navigate(AppDestinations.Activation) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                NavGraph(
                    navController = navController,
                    app = app,
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as BliqTotemApp).bleManager.onDestroy()
    }
}
