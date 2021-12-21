package tsfat.yeshivathahesder.channel.repository

import retrofit2.Response
import timber.log.Timber
import tsfat.yeshivathahesder.channel.api.PlaylistItemsService
import tsfat.yeshivathahesder.channel.model.ItemBase
import tsfat.yeshivathahesder.channel.model.ItemList
import tsfat.yeshivathahesder.channel.paging.datasource.PLAYLIST_TYPE_AUDIO
import tsfat.yeshivathahesder.channel.paging.datasource.PLAYLIST_TYPE_VIDEO

class PlaylistVideosRepository(
    private val playlistItemsService: PlaylistItemsService,
    private val audioRepository: AudioRepository
) {

    suspend fun getPlaylistVideos(
        playlistId: String,
        playlistType: String,
        pageToken: String?
    ): Response<ItemList<ItemBase>>? {
        Timber.d("Get playlist. Id: $playlistId, type: $playlistType, page: $pageToken")
        if (playlistType == PLAYLIST_TYPE_VIDEO) {
//            val id = videoIdToBase(playlistId)
            val response = playlistItemsService.getPlaylistVideos(playlistId, pageToken)
            val body = response.body()
            val list = ItemList<ItemBase>(
                body?.nextPageToken,
                body?.prevPageToken,
                body?.items ?: emptyList()
            )
            Timber.d("Got video playlist. List: $list")
            if (response.isSuccessful) {
                return Response.success(list)
            } else return Response.error(response.errorBody(), response.raw())
        } else if (playlistType == PLAYLIST_TYPE_AUDIO) {
            val playlist = audioRepository.getAudioPlaylist(playlistId)
            val list = ItemList<ItemBase>(null, null, playlist)
            Timber.d("Got audio playlist. List: $list")
            return Response.success(list)
        }
        return null
    }
}