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

package be.uclouvain.multipathcontrol.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.Toast;
import be.uclouvain.multipathcontrol.MPCtrl;
import be.uclouvain.multipathcontrol.R;
import be.uclouvain.multipathcontrol.global.Config;
import be.uclouvain.multipathcontrol.global.Manager;
import be.uclouvain.multipathcontrol.services.MainService;

public class MainActivity extends Activity {

	private MPCtrl mpctrl;
	private Switch multiIfaceSwitch;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		multiIfaceSwitch = (Switch) findViewById(R.id.switch_multiiface);

		mpctrl = Manager.create(getApplicationContext());
		if (mpctrl == null) {
			Toast.makeText(this, "It seems this is not a rooted device", Toast.LENGTH_LONG).show();
			moveTaskToBack(true);
			return;
		}

		// do that now, to avoid useless call to onCheckedChangeListerner
		setChecked();
		multiIfaceSwitch.setOnCheckedChangeListener(onCheckedChangeListernerMultiIface);

		// start a new service if needed
		startService(new Intent(this, MainService.class));
	}

	@Override
	protected void onResume() {
		super.onResume();
		setChecked();
	}

	protected void onDestroy() {
		super.onDestroy();
		Manager.destroy(getApplicationContext());
	}

	private void setChecked() {
		multiIfaceSwitch.setChecked(Config.mEnabled);
	}

	private OnCheckedChangeListener onCheckedChangeListernerMultiIface = new OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (mpctrl.setStatus(isChecked) && !isChecked) {
				Toast.makeText(MainActivity.this, "The second interface will be disabled in a few seconds", Toast.LENGTH_LONG).show();
			}
		}
	};
}
