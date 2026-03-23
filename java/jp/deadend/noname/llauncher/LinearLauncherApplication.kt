package jp.deadend.noname.llauncher

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader

class LinearLauncherApplication : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(AppIconFetcher.Factory(context))
            }
            .build()
    }
}