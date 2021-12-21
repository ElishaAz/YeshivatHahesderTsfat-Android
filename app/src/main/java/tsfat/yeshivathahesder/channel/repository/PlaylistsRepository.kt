package tsfat.yeshivathahesder.channel.repository

import retrofit2.Response
import timber.log.Timber
import tsfat.yeshivathahesder.channel.api.PlaylistsService
import tsfat.yeshivathahesder.channel.di.audioModel
import tsfat.yeshivathahesder.channel.model.PlaylistBase
import tsfat.yeshivathahesder.channel.model.Playlists
import tsfat.yeshivathahesder.channel.model.SearchedList

class PlaylistsRepository(
    private val playlistsService: PlaylistsService,
    private val audioRepository: AudioRepository
) {

    private var lastIndex = 0

    suspend fun getPlaylists(
        channelId: String,
        pageToken: String?
    ): Response<Playlists<PlaylistBase>> {
        Timber.d("Getting playlists token $pageToken")
        if (pageToken == null) lastIndex = 0
        val response = playlistsService.getPlaylists(channelId, pageToken)

        val lastItem = response.body()?.items?.last()
        val nextPageToken = response.body()?.nextPageToken

        val audioPlaylists = audioRepository.getAudioPlaylists(
            pageToken,
            nextPageToken,
            lastItem?.publishedAt ?: ""
        )

        val items = response.body()?.items ?: emptyList()

        Timber.d(items.joinToString { it.publishedAt })

        val newList = (audioPlaylists + items).sortedByDescending { it.publishedAt }

//        Timber.d("Getting playlists. Token: $pageToken, return token: ${response.body()?.nextPageToken}")

        if (response.isSuccessful) {
            return Response.success(Playlists(newList, nextPageToken))
        } else return Response.error(response.errorBody()!!, response.raw())
    }
}