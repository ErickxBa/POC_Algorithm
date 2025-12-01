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
import com.erickballas.pruebaconceptoalgoritmolpa.view.HomeScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.MapScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.ReportIncidentScreen
import com.erickballas.pruebaconceptoalgoritmolpa.view.RoutePlanningScreen
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.GraphViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.IncidentsViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.RouteViewModel

// ⚠️ ESTA ES LA CLASE QUE NO PUEDE FALTAR ⚠️
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Aquí llamamos a la navegación
            AppNavigation()
        }
    }
}

// Esta es la función de navegación (va fuera de la clase)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {

        // 1. HOME
        composable("home") {
            HomeScreen(
                onNavigateToMap = { navController.navigate("map") },
                onNavigateToRoute = { navController.navigate("route_planning") },
                onNavigateToIncident = { navController.navigate("report_incident/0.0/0.0") },
                onNavigateToGraph = { navController.navigate("graph_view") }
            )
        }

        // 2. MAPA
        composable("map") {
            val viewModel: MapViewModel = viewModel()
            MapScreen(
                viewModel = viewModel,
                // Ahora recibimos 3 parámetros: lat, lng, streetId
                onNavigateToReport = { lat, lng, streetId ->
                    // Por ahora no pasamos el streetId en la URL para simplificar,
                    // o puedes añadirlo a la ruta si quieres.
                    // El viewModel de Reporte puede calcularlo o lo ignoramos.
                    navController.navigate("report_incident/$lat/$lng")
                }
            )
        }

        // 3. RUTAS
        composable("route_planning") {
            val viewModel: RouteViewModel = viewModel()
            RoutePlanningScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        // 4. REPORTAR INCIDENTE
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

        // 5. GRAFO
        composable("graph_view") {
            val viewModel: GraphViewModel = viewModel()
            GraphScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}