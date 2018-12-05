/*
 * Show Java - A java/apk decompiler for android
 * Copyright (c) 2018 Niranjan Rajendran
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.njlabs.showjava

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.multidex.MultiDexApplication
import androidx.work.WorkManager
import com.google.android.gms.ads.MobileAds
import com.njlabs.showjava.utils.logging.ProductionTree
import io.github.inflationx.calligraphy3.CalligraphyConfig
import io.github.inflationx.calligraphy3.CalligraphyInterceptor
import io.github.inflationx.viewpump.ViewPump
import timber.log.Timber


class MainApplication : MultiDexApplication() {

    /**
     * Setup fonts, plant the correct logging tree for Timber and init ads
     */
    override fun onCreate() {
        super.onCreate()
        ViewPump.init(
            ViewPump.builder()
                .addInterceptor(
                    CalligraphyInterceptor(
                        CalligraphyConfig.Builder()
                            .setDefaultFontPath("fonts/lato-light.ttf")
                            .setFontAttrId(R.attr.fontPath)
                            .build()
                    )
                )
                .build()
        )

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ProductionTree())
        }
        MobileAds.initialize(this, getString(R.string.admobAppId))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cleanStaleNotifications()
        }
    }

    /**
     * Clean any stale notifications not linked to any decompiler process
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun cleanStaleNotifications() {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val workManager = WorkManager.getInstance()
        manager.activeNotifications.forEach { notification ->
            val status = workManager.getStatusesForUniqueWorkLiveData(notification.tag)
                .value?.any { it.state.isFinished }
            if (status == null || status == true) {
                manager.cancel(notification.tag, notification.id)
            }
        }
    }
}
