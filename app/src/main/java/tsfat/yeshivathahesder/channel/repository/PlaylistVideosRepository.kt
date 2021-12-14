package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.api.PlaylistItemsService

class PlaylistVideosRepository(private val playlistItemsService: PlaylistItemsService) {

    suspend fun getPlaylistVideos(playlistId: String, pageToken: String?) =
        playlistItemsService.getPlaylistVideos(playlistId, pageToken)
}