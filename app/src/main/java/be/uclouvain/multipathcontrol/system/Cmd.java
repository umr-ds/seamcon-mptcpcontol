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

package be.uclouvain.multipathcontrol.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Cmd {
	private static Process runAsUser(String cmd) throws Exception {
		return Runtime.getRuntime().exec(cmd.split(" "));
	}

	public static Process runAsRoot(String cmd) throws Exception {
		return Runtime.getRuntime().exec(new String[] { "su", "-c", cmd });
	}

	public static void runAsRoot(String[] cmds) throws Exception {
		for (String cmd : cmds) {
			runAsRoot(cmd);
		}
	}

	static List<String> getAllLines(String cmd) {
		List<String> lines = new ArrayList<>();
		String line;
		Process p;
		BufferedReader in = null;
		try {
			p = runAsUser(cmd);

			in = new BufferedReader(new InputStreamReader(p.getInputStream()));

			while ((line = in.readLine()) != null) {
				lines.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException ignored) {}
		}

		return lines.size() > 0 ? lines : null;
	}

	public static String getAllLinesString(String cmd, char sep) {
		StringBuilder sBuffer = new StringBuilder();
		List<String> lines = getAllLines(cmd);

        assert lines != null;
        for (String line : lines) {
			sBuffer.append(line);
			sBuffer.append(sep);
		}

		return sBuffer.toString();
	}
}
