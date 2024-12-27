package com.xayah.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.safframework.server.core.AndroidServer
import com.safframework.server.core.http.Response
import com.xayah.core.data.repository.AppsRepo
import com.xayah.core.data.repository.ListDataRepo
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.model.OpType
import com.xayah.core.util.DateUtil
import com.xayah.core.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class HttpService : Service() {

    @Inject
    private lateinit var backupServiceLocalImpl: com.xayah.core.service.packages.backup.ProcessingServiceProxyLocalImpl

    @Inject
    private lateinit var restoreServiceLocalImpl: com.xayah.core.service.packages.restore.ProcessingServiceProxyLocalImpl

    @Inject
    private lateinit var appsRepo: AppsRepo

    @Inject
    private lateinit var listDataRepo: ListDataRepo

    @Inject
    private lateinit var appsDao: PackageDao

    private var androidServer: AndroidServer? = null

    override fun onCreate() {
        super.onCreate()
        startHttpServer()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startHttpServer() {
        Log.e("KKK", "startHttpServer")
        try {
            androidServer = AndroidServer.Builder {
                port {
                    7777
                }
            }.build()
            androidServer?.apply {
                get("/hello") { _, response: Response ->
                    response.setBodyText("hello world")
                }
                get("/backup") { request, response: Response ->
                    runBlocking {
                        launch {
                            listDataRepo.getAppList().map { appList ->
                                appList.map {
                                    val activated = it.packageName == request.params()["package_name"]
                                    appsDao.activateById(it.id, activated)
                                }
                            }
                            backupServiceLocalImpl.initialize()
                            backupServiceLocalImpl.preprocessing()
                            backupServiceLocalImpl.processing()
                            backupServiceLocalImpl.postProcessing()
                            backupServiceLocalImpl.destroyService()
                            appsDao.queryActivated(OpType.BACKUP).map {
                                appsRepo.protectApp("", it)
                            }
                        }.join()
                        response.setBodyText("Backup started")
                    }
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("HttpService", "HttpServer exception: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        androidServer?.close()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}