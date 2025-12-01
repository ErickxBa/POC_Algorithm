package com.erickballas.pruebaconceptoalgoritmolpa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.erickballas.pruebaconceptoalgoritmolpa.view.GraphScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.HomeScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.MapScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.ReportIncidentScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.RoutePlanningScreen
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.GraphViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.IncidentsViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.RouteViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        // 1. Home
        composable("home") {
            HomeScreen(
                onNavigateToMap = { navController.navigate("map") },
                onNavigateToRoute = { navController.navigate("route_planning") },
                onNavigateToIncident = {
                    // Si navega desde el home, usa coordenadas por defecto (o 0.0)
                    navController.navigate("report_incident/0.0/0.0")
                },
                onNavigateToGraph = { navController.navigate("graph_view") }
            )
        }

        // 2. Mapa (Ahora puede enviar coordenadas)
        composable("map") {
            val viewModel: MapViewModel = viewModel()
            MapScreen(
                viewModel = viewModel,
                onNavigateToReport = { lat, lng ->
                    // NAVEGAMOS PASANDO LAS COORDENADAS REALES
                    navController.navigate("report_incident/$lat/$lng")
                }
            )
        }

        // 3. Ruta
        composable("route_planning") {
            val viewModel: RouteViewModel = viewModel()
            RoutePlanningScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 4. Reportar Incidente (RECIBE COORDENADAS)
        composable(
            route = "report_incident/{lat}/{lng}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0

            val viewModel: IncidentsViewModel = viewModel()
            ReportIncidentScreen(
                viewModel = viewModel,
                initialLat = lat,
                initialLng = lng,
                onBackClick = { navController.popBackStack() },
                onIncidentReported = { navController.popBackStack() }
            )
        }

        // 5. Gráfo
        composable("graph_view") {
            val viewModel: GraphViewModel = viewModel()
            GraphScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}