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

import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.ConfigUtils;


public class MyControler1 {

	public static void main(final String[] args) throws IOException {

		Config config;
		if ( args.length==0 ) {
//			config = Gbl.createConfig(new String[] {"../studies/schweiz/6-9SepFmaZurichOnly_rad=26000m-hwh/config-10pct.xml"});
//			config = Gbl.createConfig(new String[] {"./examples/roundabout/config.xml"});
			config = ConfigUtils.loadConfig("./examples/equil/myconfig.xml");
//			config = Gbl.createConfig(new String[] {"../padang/dlr-network/pconfig.xml"});
//			config = Gbl.createConfig(new String[] {"/home/nagel/vsp-cvs/studies/ivtch-schweiz/plans/kaiconfig.xml"});
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}

		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
		controler.run();

//		// Visualize
//		String[] visargs = {"./output/ITERS/it.100/Snapshot"};
//		NetVis.main(visargs);

	}

}
