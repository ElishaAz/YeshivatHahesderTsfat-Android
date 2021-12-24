package tsfat.yeshivathahesder.channel.viewmodel

import tsfat.yeshivathahesder.channel.model.FavoritesEntry
import tsfat.yeshivathahesder.channel.repository.FavoritesRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FavoritesViewModel(private val favoritesRepository: FavoritesRepository) : ViewModel() {

    var favoriteVideosLiveData: LiveData<List<FavoritesEntry>> = liveData(Dispatchers.IO) {
        emit(favoritesRepository.getFavoriteVideosFromDb())
    }

    fun removeVideoFromFavorites(favoritesEntry: FavoritesEntry) {
        viewModelScope.launch {
            favoritesRepository.removeVideoFromFavorites(favoritesEntry)
        }
    }

    fun addVideoToFavorites(favoritesEntry: FavoritesEntry) {
        viewModelScope.launch {
            favoritesRepository.addVideoToFavorites(favoritesEntry)
        }
    }
}