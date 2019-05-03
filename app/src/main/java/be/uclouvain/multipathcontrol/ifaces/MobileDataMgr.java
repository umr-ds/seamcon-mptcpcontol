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

package be.uclouvain.multipathcontrol.ifaces;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;

public class MobileDataMgr {

	private final Context context;
	/*
	 * We need an existing domain that nobody uses in order to have a existing
	 * route assigned to the cellular connection. The goal is to not disable
	 * Cellular interface when switching to a new Wi-Fi. example.org domain is
	 * reserved by IANA, nobody will want to use it!
	 */
	private static final String DEFAULT_LOOKUP_HOST = "example.org";

	public MobileDataMgr(Context context) {
		this.context = context;
	}

	private boolean isWifiConnected() {
		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connManager != null;
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return mWifi.isConnectedOrConnecting();
	}

	/* Check whether Mobile Data has been disabled in the System Preferences */
	private boolean isMobileDataEnabled() {
		boolean mobileDataEnabled = false;
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		try {
            assert cm != null;
            Class<?> cmClass = Class.forName(cm.getClass().getName());
			Method method = cmClass.getDeclaredMethod("getMobileDataEnabled");
			method.setAccessible(true);
			mobileDataEnabled = (Boolean) method.invoke(cm);
		} catch (Exception ignored) {}

		return mobileDataEnabled;
	}

	/* Enable having WiFi and 3G/LTE enabled at the same time */
	public void setMobileDataActive(boolean mEnabled) {
		ConnectivityManager cManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cManager != null;

		if (isMobileDataEnabled() && isWifiConnected() && mEnabled) {
            cManager.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableHIPRI");
        } else {
            cManager.stopUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableHIPRI");
        }
	}

	private static int getAddr(InetAddress inetAddress) {
		byte[] addrBytes;
		int addr;

		addrBytes = inetAddress.getAddress();
		addr = ((addrBytes[3] & 0xff) << 24)
                | ((addrBytes[2] & 0xff) << 16)
				| ((addrBytes[1] & 0xff) << 8)
                | (addrBytes[0] & 0xff);

		return addr;
	}

	private static int lookupHost(String hostname) {
		InetAddress inetAddress;
		try {
			inetAddress = InetAddress.getByName(hostname);
		} catch (UnknownHostException e) {
			return -1;
		}
		return getAddr(inetAddress);
	}

	public void keepMobileConnectionAlive() {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (null == connectivityManager) {
			return;
		}

		// create a route for the specified address
		int hostAddress = lookupHost(DEFAULT_LOOKUP_HOST);
		if (-1 == hostAddress) {
			return;
		}
		// wait some time needed to connection manager for waking up
		try {
			for (int counter = 0; counter < 30; counter++) {
				State checkState = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI).getState();

				if (0 == checkState.compareTo(State.CONNECTED)) {
					break;
				}

				Thread.sleep(1000);
			}
		} catch (InterruptedException ignored) {}

		connectivityManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE_HIPRI, hostAddress);
	}
}
