package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.api.PlaylistsService

class PlaylistsRepository(private val playlistsService: PlaylistsService) {

    suspend fun getPlaylists(channelId: String, pageToken: String?) =
        playlistsService.getPlaylists(channelId, pageToken)
}