package com.erickballas.pruebaconceptoalgoritmolpa

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.erickballas.pruebaconceptoalgoritmolpa.view.MapScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.ReportIncidentScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.RoutePlanningScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.HomeScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.GraphScreen
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.IncidentsViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.RouteViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.GraphViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "map") {

        // 0. PANTALLA HOME
        composable(route = "home") {
            val mapViewModel: MapViewModel = viewModel()
            HomeScreen(
                onNavigateToMap = { navController.navigate("map") },
                onNavigateToRoute = {
                    val currentLoc = mapViewModel.mapState.value.userLocation
                    val lat = currentLoc?.latitude ?: 0.0
                    val lng = currentLoc?.longitude ?: 0.0
                    navController.navigate("route_planning?lat=$lat&lng=$lng")
                },
                onNavigateToIncident = { navController.navigate("report_incident/0.0/0.0") },
                onNavigateToGraph = { 
                    val currentLoc = mapViewModel.mapState.value.userLocation
                    val lat = currentLoc?.latitude ?: 0.0
                    val lng = currentLoc?.longitude ?: 0.0
                    navController.navigate("graph?lat=$lat&lng=$lng") 
                }
            )
        }

        // 1. MAPA (Ahora acepta parámetros de destino para calcular ruta automáticamente)
        composable(
            route = "map?targetLat={targetLat}&targetLng={targetLng}",
            arguments = listOf(
                navArgument("targetLat") { defaultValue = 0.0f },
                navArgument("targetLng") { defaultValue = 0.0f }
            )
        ) { backStackEntry ->
            val targetLat = backStackEntry.arguments?.getFloat("targetLat")?.toDouble() ?: 0.0
            val targetLng = backStackEntry.arguments?.getFloat("targetLng")?.toDouble() ?: 0.0

            Log.d("MainActivity", "📍 Argumentos recibidos en Mapa: $targetLat, $targetLng")
            val viewModel: MapViewModel = viewModel()

            // SI HAY DESTINO, CALCULAMOS AUTOMÁTICAMENTE
            LaunchedEffect(targetLat, targetLng) {
                if (targetLat != 0.0 && targetLng != 0.0) {
                    viewModel.calculateRouteToDestination(targetLat, targetLng)
                }
            }

            MapScreen(
                viewModel = viewModel,
                onSearchClick = {
                    val currentLoc = viewModel.mapState.value.userLocation
                    val lat = currentLoc?.latitude ?: 0.0
                    val lng = currentLoc?.longitude ?: 0.0
                    navController.navigate("route_planning?lat=$lat&lng=$lng")
                },
                onNavigateToReport = { lat, lng, streetId ->
                    navController.navigate("report_incident/$lat/$lng")
                }
            )
        }

        // 2. PLANIFICAR RUTA
        composable(
            route = "route_planning?lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("lat") { defaultValue = "0.0" },
                navArgument("lng") { defaultValue = "0.0" }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0

            val viewModel: RouteViewModel = viewModel()
            RoutePlanningScreen(
                viewModel = viewModel,
                currentUserLat = lat,
                currentUserLng = lng,
                onBackClick = { navController.popBackStack() },
                // CAMBIO: Al confirmar ruta, volvemos al mapa pasándole el destino
                onRouteCalculated = { destLat, destLng ->
                    // Navegamos al mapa (reseteando el stack para que sea la pantalla principal)
                    navController.navigate("map?targetLat=$destLat&targetLng=$destLng") {
                        popUpTo("map") { inclusive = true }
                    }
                }
            )
        }

        // 3. REPORTAR INCIDENTE
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

        // 4. VISUALIZADOR DE GRAFO
        composable(
            route = "graph?lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("lat") { defaultValue = "0.0" },
                navArgument("lng") { defaultValue = "0.0" }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0

            val viewModel: GraphViewModel = viewModel()
            GraphScreen(
                viewModel = viewModel,
                initialLat = lat,
                initialLng = lng,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}