/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.arvid_daniel.coopers.fromArvid;

import org.matsim.utils.vis.netvis.NetVis;
import org.matsim.withinday.coopers.CoopersControler;

public class MyControler2 {

	public static void main(final String[] args) {
		String[] conf = {"../studies/arvidDaniel/input/testExtended/config.xml"};
		CoopersControler controler = new CoopersControler(conf);
		controler.setOverwriteFiles(true);
		controler.run();

		// Visulize
		String[] visargs = {"../studies/arvidDaniel/output/ITERS/it.0/Snapshot"};
		NetVis.main(visargs);
	}

}
