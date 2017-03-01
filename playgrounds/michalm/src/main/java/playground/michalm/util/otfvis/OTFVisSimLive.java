/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.michalm.util.otfvis;

import java.util.Arrays;

import org.matsim.contrib.otfvis.OTFVis;

public class OTFVisSimLive {
	public static void main(String[] args) {
		String dir;
		String cfgFile;

		if (args.length == 1 && args[0].equals("test")) {// for testing
			dir = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";

			cfgFile = "config-verB_with_vrp.xml";
		} else if (args.length == 2) {
			dir = args[0];
			cfgFile = args[1];
		} else {
			throw new IllegalArgumentException("Incorrect program arguments: " + Arrays.toString(args));
		}

		OTFVis.playConfig(dir + cfgFile);
	}
}
