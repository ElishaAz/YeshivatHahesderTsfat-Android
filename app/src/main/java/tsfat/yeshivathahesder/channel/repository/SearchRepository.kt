package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.api.SearchVideoService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SearchRepository(
    private val searchVideoService: SearchVideoService,
    private val audioRepository: AudioRepository
) {

    suspend fun searchVideos(searchQuery: String, channelId: String, pageToken: String?) =
        withContext(Dispatchers.IO) {
            val defaultQueryMap = HashMap<String, String>()
            defaultQueryMap.apply {
                put("part", "id,snippet")
                put(
                    "fields",
                    "nextPageToken, items(id(videoId), snippet(publishedAt, thumbnails, title))"
                )
                put("order", "relevance")
                put("type", "video")
            }
            val response =
                searchVideoService.searchVideos(searchQuery, channelId, pageToken, defaultQueryMap)

            val audioItems = audioRepository.searchAudioItems(searchQuery)

            joinAudioAndVideoSearchResults(response, audioItems, searchQuery)
        }
}