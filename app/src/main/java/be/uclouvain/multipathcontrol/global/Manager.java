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

package be.uclouvain.multipathcontrol.global;

import java.io.File;

import android.content.Context;
import be.uclouvain.multipathcontrol.MPCtrl;

public class Manager {
	private static MPCtrl mpctrl = null;
	private static int instances = 0;
	private static Context usedContext;

	private static boolean checkRoot() {
		return new File("/system/xbin/su").canExecute();
	}

	public static MPCtrl create(Context context) {
		if (mpctrl == null) {
			if (!checkRoot()) {
				return null;
			}
			usedContext = context;
			mpctrl = new MPCtrl(context);
		}
		instances++;
		return mpctrl;
	}

	public static void destroy(Context context) {
		if (context != usedContext || mpctrl == null)
			return;
		instances--;
		if (instances != 0) {
			return;
		}
		mpctrl.destroy();
		mpctrl = null;
	}
}
