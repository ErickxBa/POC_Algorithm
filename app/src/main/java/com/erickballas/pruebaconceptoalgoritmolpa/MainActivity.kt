package com.erickballas.pruebaconceptoalgoritmolpa

import android.os.Bundle
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
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.IncidentsViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.MapViewModel
import com.erickballas.pruebaconceptoalgoritmolpa.viewmodel.RouteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppNavigation() }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "map") {

        composable(
            route = "map?lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("lat") { defaultValue = 0.0f },
                navArgument("lng") { defaultValue = 0.0f }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lng = backStackEntry.arguments?.getFloat("lng")?.toDouble() ?: 0.0
            val viewModel: MapViewModel = viewModel()

            // Si llegamos con coordenadas, calculamos ruta
            LaunchedEffect(lat, lng) {
                if (lat != 0.0 && lng != 0.0) viewModel.calculateRouteToDestination(lat, lng)
            }

            MapScreen(
                viewModel = viewModel,
                onSearchClick = {
                    val loc = viewModel.mapState.value.userLocation
                    navController.navigate("route_planning?lat=${loc?.latitude ?: 0.0}&lng=${loc?.longitude ?: 0.0}")
                },
                onNavigateToReport = { rLat, rLng, sId ->
                    navController.navigate("report/$rLat/$rLng")
                }
            )
        }

        composable(
            route = "route_planning?lat={lat}&lng={lng}",
            arguments = listOf(navArgument("lat") { defaultValue = "0.0" }, navArgument("lng") { defaultValue = "0.0" })
        ) {
            val viewModel: RouteViewModel = viewModel()
            RoutePlanningScreen(
                viewModel = viewModel,
                currentUserLat = it.arguments?.getString("lat")?.toDouble() ?: 0.0,
                currentUserLng = it.arguments?.getString("lng")?.toDouble() ?: 0.0,
                onBackClick = { navController.popBackStack() },
                onRouteCalculated = { dLat, dLng ->
                    // Volver al mapa con el destino
                    navController.navigate("map?lat=$dLat&lng=$dLng") {
                        popUpTo("map") { inclusive = true }
                    }
                }
            )
        }

        composable("report/{lat}/{lng}") {
            val viewModel: IncidentsViewModel = viewModel()
            ReportIncidentScreen(
                viewModel = viewModel,
                initialLat = it.arguments?.getString("lat")?.toDouble() ?: 0.0,
                initialLng = it.arguments?.getString("lng")?.toDouble() ?: 0.0,
                onBackClick = { navController.popBackStack() },
                onIncidentReported = { navController.popBackStack() }
            )
        }
    }
}