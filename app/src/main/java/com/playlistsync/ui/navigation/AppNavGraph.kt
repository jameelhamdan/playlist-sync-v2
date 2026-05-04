package com.playlistsync.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.playlistsync.ui.config.FormatConfigScreen
import com.playlistsync.ui.detail.PlaylistDetailScreen
import com.playlistsync.ui.playlist.AddPlaylistScreen
import com.playlistsync.ui.playlist.PlaylistListScreen
import com.playlistsync.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.PlaylistList.route
    ) {
        composable(Screen.PlaylistList.route) {
            PlaylistListScreen(
                onAddPlaylist   = { navController.navigate(Screen.AddPlaylist.route) },
                onPlaylistClick = { id -> navController.navigate(Screen.Detail.withId(id)) },
                onSettings      = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.AddPlaylist.route) {
            AddPlaylistScreen(
                onBack  = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument(Screen.Detail.ARG) { type = NavType.StringType })
        ) {
            PlaylistDetailScreen(
                onBack         = { navController.popBackStack() },
                onFormatConfig = { id -> navController.navigate(Screen.FormatConfig.withId(id)) }
            )
        }

        composable(
            route = Screen.FormatConfig.route,
            arguments = listOf(navArgument(Screen.FormatConfig.ARG) { type = NavType.StringType })
        ) {
            FormatConfigScreen(onBack = { navController.popBackStack() })
        }
    }
}
