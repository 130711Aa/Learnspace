package com.lernspace.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lernspace.app.data.local.entity.TaskEntity
import com.lernspace.app.data.repository.SubjectRepository
import com.lernspace.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val tasksDueToday: List<TaskEntity> = emptyList(),
    val overdueTasks: List<TaskEntity> = emptyList(),
    val isLoading: Boolean = false
)

class HomeViewModel(
    private val taskRepository: TaskRepository,
    private val subjectRepository: SubjectRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        taskRepository.getTasksDueToday(),
        taskRepository.getOverdueTasks()
    ) { dueToday, overdue ->
        HomeUiState(
            tasksDueToday = dueToday,
            overdueTasks = overdue,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    init {
        viewModelScope.launch {
            // Seed initial sample data if empty so the screen is immediately populated
            taskRepository.getAllTasks().firstOrNull()?.let { allTasks ->
                if (allTasks.isEmpty()) {
                    subjectRepository.seedInitialDataIfEmpty()
                    taskRepository.seedInitialTasksIfEmpty()
                }
            }
        }
    }

    fun quickAddTask(title: String) {
        viewModelScope.launch {
            taskRepository.createTask(title = title)
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
        }
    }

    companion object {
        fun provideFactory(
            taskRepository: TaskRepository,
            subjectRepository: SubjectRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(taskRepository, subjectRepository) as T
            }
        }
    }
}

