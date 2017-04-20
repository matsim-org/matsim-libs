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

public class OTFVisSim {

	public static void main(String[] args) {
		String dir;
		String mviFile;

		if (args.length == 1 && args[0].equals("test")) {// for testing
			// dir = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
			dir = "d:\\PP-rad\\taxi\\mielec-2-peaks\\";

			// mviFile = "output\\config-verB\\ITERS\\it.10\\10.otfvis.mvi";
			// mviFile = "output\\config-verB\\ITERS\\it.50\\50.otfvis.mvi";

			mviFile = "20.otfvis.mvi";
		} else if (args.length == 2) {
			dir = args[0];
			mviFile = args[1];
		} else {
			throw new IllegalArgumentException("Incorrect program arguments: " + Arrays.toString(args));
		}

		OTFVis.playMVI(dir + mviFile);
	}
}
