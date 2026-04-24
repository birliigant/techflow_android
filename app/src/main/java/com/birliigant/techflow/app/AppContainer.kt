package com.birliigant.techflow.app

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.birliigant.techflow.data.local.TechFlowDatabase
import com.birliigant.techflow.data.network.ApiClientProvider
import com.birliigant.techflow.data.repository.ConfigRepository
import com.birliigant.techflow.data.repository.QuestionRepository
import com.birliigant.techflow.data.repository.SessionRepository
import com.birliigant.techflow.data.repository.SiteRepository
import com.birliigant.techflow.data.repository.TagRepository
import com.birliigant.techflow.data.repository.UserRepository
import com.google.gson.Gson
import com.tencent.mmkv.MMKV

class AppContainer(application: Application) {

    private val gson = Gson()
    private val storage: MMKV = MMKV.defaultMMKV()

    private val database: TechFlowDatabase = Room.databaseBuilder(
        application,
        TechFlowDatabase::class.java,
        "techflow.db",
    ).fallbackToDestructiveMigration().build()

    val configRepository = ConfigRepository(storage)
    val sessionRepository = SessionRepository(storage, gson)
    private val apiClientProvider = ApiClientProvider(
        configRepository = configRepository,
        sessionRepository = sessionRepository,
        gson = gson,
    )

    val siteRepository = SiteRepository(apiClientProvider)
    val tagRepository = TagRepository(apiClientProvider)
    val questionRepository = QuestionRepository(
        apiClientProvider = apiClientProvider,
        questionDao = database.questionDao(),
    )
    val userRepository = UserRepository(
        apiClientProvider = apiClientProvider,
        sessionRepository = sessionRepository,
    )
}

inline fun <reified T : ViewModel> appViewModelFactory(
    crossinline creator: () -> T,
): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
    }
}
