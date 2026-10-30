package dev.codespacemobile

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import dev.codespacemobile.ui.dashboard.DashboardScreen
import dev.codespacemobile.ui.dashboard.DashboardViewModel
import dev.codespacemobile.ui.editor.EditorScreen
import dev.codespacemobile.ui.theme.CodespaceTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { CodespaceTheme { MainNavigation() } }
    }
}

@Composable
private fun MainNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "dashboard") {
        composable("dashboard") {
            val model: DashboardViewModel = hiltViewModel()
            DashboardScreen(model, onOpen = { item ->
                val url = item.webUrl ?: return@DashboardScreen
                navController.navigate("editor?name=${Uri.encode(item.title)}&url=${Uri.encode(url)}")
            })
        }
        composable(
            "editor?name={name}&url={url}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType; defaultValue = "Codespace" },
                navArgument("url") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            EditorScreen(
                title = entry.arguments?.getString("name").orEmpty(),
                url = entry.arguments?.getString("url").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
