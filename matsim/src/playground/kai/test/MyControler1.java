/* *********************************************************************** *
 * project: org.matsim.*
 * MyControler1.java
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

/* *********************************************************************** *
 *                                                                         *
 *                                                                         *
 *                          ---------------------                          *
 * copyright       : (C) 2007 by Michael Balmer, Marcel Rieser,            *
 *                   David Strippgen, Gunnar Flötteröd, Konrad Meister,    *
 *                   Kai Nagel, Kay W. Axhausen                            *
 *                   Technische Universitaet Berlin (TU-Berlin) and        *
 *                   Swiss Federal Institute of Technology Zurich (ETHZ)   *
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

import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;


public class MyControler1 extends Controler {

	public static void main(final String[] args) {

		if ( args.length==0 ) {
//			Gbl.createConfig(new String[] {"../studies/schweiz/6-9SepFmaZurichOnly_rad=26000m-hwh/config-10pct.xml"});
			Gbl.createConfig(new String[] {"./examples/roundabout/config.xml"});
		} else {
			Gbl.createConfig(args) ;
		}

		final MyControler1 controler = new MyControler1();
		controler.setOverwriteFiles(true) ;

		controler.run(null);

//		// Visualize
//		String[] visargs = {"./output/ITERS/it.0/Snapshot"};
//		NetVis.main(visargs);

	}

}
