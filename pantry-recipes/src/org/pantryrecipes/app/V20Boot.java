package org.pantryrecipes.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** v2.0: re-arms the daily use-by reminder after a reboot. Does nothing else, whoever sends it. */
public final class V20Boot extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) V20Alarm.schedule(c);
    }
}
