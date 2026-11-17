package com.galibu.client.ui.util

import android.app.Application
import android.content.Context
import android.content.Intent

object BidPushHelper {
    const val EXTRA_OPEN_JOB_ID = "open_job_id"

    fun isAlreadyNotified(context: Context, bidId: String): Boolean = false
    fun markBidNotified(context: Context, bidId: String) {}
    fun showNewOfferPush(context: Context, title: String, body: String, jobId: String) {}
    fun showEventPush(context: Context, title: String, body: String, jobId: String?, activityClass: Class<*>) {}
}
