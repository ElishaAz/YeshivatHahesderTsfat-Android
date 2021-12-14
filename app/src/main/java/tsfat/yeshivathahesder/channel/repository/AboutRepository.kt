package tsfat.yeshivathahesder.channel.repository

import tsfat.yeshivathahesder.channel.api.ChannelInfoService

class AboutRepository(private val channelInfoService: ChannelInfoService) {

    suspend fun getChannelInfo(channelId: String) =
        channelInfoService.getChannelInfo(channelId)
}