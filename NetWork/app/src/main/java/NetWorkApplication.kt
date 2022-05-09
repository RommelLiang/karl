import android.app.Application
import leakcanary.AppWatcher

class NetWorkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppWatcher.manualInstall(this)
    }
}