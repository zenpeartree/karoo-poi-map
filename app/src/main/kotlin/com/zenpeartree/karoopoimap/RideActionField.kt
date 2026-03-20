package com.zenpeartree.karoopoimap

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.ViewConfig

class RideActionField(
    typeId: String,
    private val titleResId: Int,
    private val subtitleResId: Int,
    private val activityClass: Class<*>,
) : DataTypeImpl("karoo-poi-map", typeId) {

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        emitter.updateView(
            RemoteViews(context.packageName, R.layout.ride_action_field).apply {
                setTextViewText(R.id.ride_field_title, context.getString(titleResId))
                setTextViewText(R.id.ride_field_subtitle, context.getString(subtitleResId))
                setOnClickPendingIntent(
                    R.id.ride_field_root,
                    PendingIntent.getActivity(
                        context,
                        typeId.hashCode(),
                        Intent(context, activityClass).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                )
            }
        )
    }
}
