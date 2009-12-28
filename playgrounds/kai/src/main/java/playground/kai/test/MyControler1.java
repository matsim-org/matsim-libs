/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.kai.test;
/*
 * $Id: MyControler1.java,v 1.1 2007/11/14 12:00:28 nagel Exp $
 */

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vis.netvis.NetVis;


public class MyControler1 {

	public static void main(final String[] args) {

		if ( args.length==0 ) {
//			Gbl.createConfig(new String[] {"../studies/schweiz/6-9SepFmaZurichOnly_rad=26000m-hwh/config-10pct.xml"});
//			Gbl.createConfig(new String[] {"./examples/roundabout/config.xml"});
			Gbl.createConfig(new String[] {"./examples/equil/myconfig.xml"});
//			Gbl.createConfig(new String[] {"../padang/dlr-network/pconfig.xml"});
//			Gbl.createConfig(new String[] {"/home/nagel/vsp-cvs/studies/ivtch-schweiz/plans/kaiconfig.xml"});
		} else {
			Gbl.createConfig(args) ;
		}

		final Controler controler = new Controler(Gbl.getConfig());
		controler.setOverwriteFiles(true);
		controler.run();

//		// Visualize
//		String[] visargs = {"./output/ITERS/it.100/Snapshot"};
//		NetVis.main(visargs);

	}

}
