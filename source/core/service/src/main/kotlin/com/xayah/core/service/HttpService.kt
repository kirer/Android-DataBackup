package com.xayah.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.safframework.server.core.AndroidServer
import com.safframework.server.core.http.Response
import com.xayah.core.data.repository.AppsRepo
import com.xayah.core.data.repository.ListDataRepo
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.model.ContributorItem
import com.xayah.core.model.OpType
import com.xayah.core.util.DateUtil
import com.xayah.core.util.GsonUtil
import com.xayah.core.util.PathUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

data class BackupRequestParams(val package_name: String)

@AndroidEntryPoint
class HttpService : Service() {

    @Inject
    lateinit var backupServiceLocalImpl: com.xayah.core.service.packages.backup.ProcessingServiceProxyLocalImpl

    @Inject
    lateinit var restoreServiceLocalImpl: com.xayah.core.service.packages.restore.ProcessingServiceProxyLocalImpl

    @Inject
    lateinit var appsRepo: AppsRepo

    @Inject
    lateinit var listDataRepo: ListDataRepo

    @Inject
    lateinit var appsDao: PackageDao

    @Inject
    lateinit var gsonUtil: GsonUtil

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
                post("/backup") { request, response: Response ->
                    Log.e("KKK", "backup")
                    runBlocking {
                        launch {
                            try {
                                Log.e("KKK", "params: ${request.content()}")
                                val params:Map<String, String> = gsonUtil.fromJson(
                                    request.content(), object : TypeToken<Map<String, String>>() {}.type
                                )
                                val packageName = params["package_name"]
                                Log.e("KKK", "packageName: $packageName")
                                Log.e("KKK", "packageName: ${listDataRepo.getAppList()}")
                                listDataRepo.getAppList().map { appList ->
                                    appList.map {
                                        Log.e("KKK", "app: ${it}")
                                        val activated = it.packageName == packageName
                                        appsDao.activateById(it.id, activated)
                                        Log.e("KKK", "activated: ${it.packageName}")
                                    }
                                }
                                backupServiceLocalImpl.initialize()
                                backupServiceLocalImpl.preprocessing()
                                backupServiceLocalImpl.processing()
                                backupServiceLocalImpl.postProcessing()
                                backupServiceLocalImpl.destroyService()
                                appsDao.queryActivated(OpType.BACKUP).map {
                                    appsRepo.protectApp("", it)
                                    Log.e("KKK", "protectApp: ${it.packageName}")
                                }
                            }catch (e: Exception) {
                                Log.e("KKK", "exception > $e")
                                e.printStackTrace()
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