package com.erickballas.pruebaconceptoalgoritmolpa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.erickballas.pruebaconceptoalgoritmolpa.view.GraphScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.MapScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.ReportIncidentScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.RoutePlanningScreen
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.GraphViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.IncidentsViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.RouteViewModel

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

    // CAMBIO 1: El destino inicial ahora es "map", saltándonos el menú antiguo
    NavHost(navController = navController, startDestination = "map") {

        // 1. MAPA (Pantalla Principal estilo Figma "home/inicio")
        composable("map") {
            val viewModel: MapViewModel = viewModel()
            MapScreen(
                viewModel = viewModel,
                // Al tocar la barra de búsqueda, vamos a planificar ruta ("inicioRuta")
                onSearchClick = {
                    navController.navigate("route_planning")
                },
                // Al reportar, vamos a la pantalla de reporte
                onNavigateToReport = { lat, lng, streetId ->
                    navController.navigate("report_incident/$lat/$lng")
                }
            )
        }

        // 2. PLANIFICAR RUTA (Pantalla "inicioRuta" del Figma)
        // CAMBIO: Definir ruta con parámetros opcionales para la ubicación actual
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
                onRouteCalculated = {
                    // Regresar al mapa (idealmente pasando la ruta, pero por ahora solo volvemos)
                    navController.popBackStack()
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

        // La pantalla de grafo queda accesible solo si la necesitas por debug,
        // pero ya no es parte del flujo principal.
    }
}