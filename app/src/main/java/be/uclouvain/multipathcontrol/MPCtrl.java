/*
 * This file is part of MultipathControl.
 *
 * Copyright 2012 UCLouvain - Gregory Detal <first.last@uclouvain.be>
 * Copyright 2015 UCLouvain - Matthieu Baerts <first.last@student.uclouvain.be>
 *
 * MultipathControl is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package be.uclouvain.multipathcontrol;

import java.net.NetworkInterface;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.PowerManager;
import be.uclouvain.multipathcontrol.global.Config;
import be.uclouvain.multipathcontrol.ifaces.IPRoute;
import be.uclouvain.multipathcontrol.ifaces.MobileDataMgr;
import be.uclouvain.multipathcontrol.stats.SaveDataHandover;
import be.uclouvain.multipathcontrol.system.Cmd;
import be.uclouvain.multipathcontrol.system.IPRouteUtils;

public class MPCtrl {

	private final Context context;
	private final MobileDataMgr mobileDataMgr;
	private final Handler handler;
	private final IPRoute iproute;
	private static long lastTimeHandler;

	private BroadcastReceiver mConnReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            assert pm != null;
            if (pm.isScreenOn()) {
				mobileDataMgr.setMobileDataActive(Config.mEnabled);
			}

			if (iproute.monitorInterfaces()) {
				new SaveDataHandover(context);
			}
		}
	};

	public MPCtrl(Context context) {
		this.context = context;

		// to be sure that all connections will be managed by the proxy
		restartIFaces();

		Config.getDefaultConfig(context);
		mobileDataMgr = new MobileDataMgr(context);
		iproute = new IPRoute(mobileDataMgr);

		handler = new Handler();
		initHandler();

		/*
		 * mConnReceiver will be called each time a change of connectivity
		 * happen
		 */
		context.registerReceiver(mConnReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	public void destroy() {
		try {
			context.unregisterReceiver(mConnReceiver);
		} catch (IllegalArgumentException ignored) {}

		handler.getLooper().quit();
	}

	public boolean setStatus(boolean isChecked) {
		if (isChecked == Config.mEnabled)
			return false;

		Config.mEnabled = isChecked;
		Config.saveStatus(context);

		if (isChecked) {
			if (iproute.monitorInterfaces()) {
                new SaveDataHandover(context);
            }
		}

		return true;
	}

	private void restartIFaces() {
		List<NetworkInterface> activeIfaces = IPRouteUtils.getActiveIfaces();
		if (activeIfaces == null || activeIfaces.isEmpty())
			return;

		for (NetworkInterface iface : activeIfaces) {
			String ifaceName = iface.getName();
			if (ifaceName.contains("wlan0")) {
				continue;
			}
			try {
				Cmd.runAsRoot("ip link set " + ifaceName + " down").wait();
			} catch (Exception ignored) {}
			try {
				Cmd.runAsRoot("ip link set " + ifaceName + " up");
			} catch (Exception ignored) {}
		}
	}

	// Will not be executed in deep sleep, nice, no need to use both connections
	// in deep-sleep
	private void initHandler() {
		lastTimeHandler = System.currentTimeMillis();

		// First check
		handler.post(runnableSetMobileDataActive);
	}

	/*
	 * Ensures that the data interface and WiFi are connected at the same time.
	 */
	private Runnable runnableSetMobileDataActive = new Runnable() {
		@Override
		public void run() {
			long nowTime = System.currentTimeMillis();
			// do not try keep mobile data active in deep sleep mode
			if (Config.mEnabled && nowTime - lastTimeHandler < Config.mobileDataActiveTime * 2) {
                // to not disable cellular iface
                mobileDataMgr.setMobileDataActive(Config.mEnabled);
            }

			lastTimeHandler = nowTime;
			handler.postDelayed(this, Config.mobileDataActiveTime);
		}
	};
}
