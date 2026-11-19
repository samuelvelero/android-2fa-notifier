package com.samukis.otpcopy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CopyAgainReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        intent.getStringExtra(EXTRA_CODE)?.let { CodeHandler.copy(ctx, it) }
    }

    companion object {
        const val EXTRA_CODE = "code"
    }
}
