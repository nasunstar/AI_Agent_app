package com.example.database_project.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.example.database_project.data.db.AppDatabase
import com.example.database_project.data.entity.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).noteDao()   // OK

    val notes = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addNote(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            dao.insert(Note(title = title.trim()))
        }
    }


    companion object {
        val factory: ViewModelProvider.Factory = object : ViewModelProvider.AndroidViewModelFactory() {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val app = checkNotNull(extras[APPLICATION_KEY])     // 불필요한 qualifier 제거
                return NoteViewModel(app as Application) as T       // 필요한 캐스트만 유지
            }
        }
    }
}