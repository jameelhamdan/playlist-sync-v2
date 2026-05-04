package com.playlistsync.ui.navigation

sealed class Screen(val route: String) {
    object PlaylistList : Screen("playlist_list")
    object AddPlaylist  : Screen("add_playlist")

    object Detail : Screen("playlist_detail/{playlistId}") {
        fun withId(id: String) = "playlist_detail/$id"
        const val ARG = "playlistId"
    }

    object FormatConfig : Screen("format_config/{playlistId}") {
        fun withId(id: String) = "format_config/$id"
        const val ARG = "playlistId"
    }
}
