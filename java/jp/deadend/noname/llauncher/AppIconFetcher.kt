package jp.deadend.noname.llauncher

import android.content.Context
import android.content.pm.PackageManager
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options

class AppIconFetcher(
    private val context: Context,
    private val data: AppIconData
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        return try {
            val pm = context.packageManager
            val icon = pm.getApplicationIcon(data.packageName)

            ImageFetchResult(
                image = icon.asImage(),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<AppIconData> {
        override fun create(
            data: AppIconData,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher {
            return AppIconFetcher(context, data)
        }
    }
}