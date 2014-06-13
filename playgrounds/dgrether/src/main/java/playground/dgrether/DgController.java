/* *********************************************************************** *
 * project: org.matsim.*
 * DgController
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
package playground.dgrether;

import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.dgrether.signalsystems.sylvia.controler.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class DgController {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler c = new Controler(args[0]);
		c.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		DgSylviaConfig sylviaConfig = new DgSylviaConfig();
		final DgSylviaControlerListenerFactory signalsFactory = new DgSylviaControlerListenerFactory(sylviaConfig);
		signalsFactory.setAlwaysSameMobsimSeed(false);
		c.setSignalsControllerListenerFactory(signalsFactory);
		c.setOverwriteFiles(true);
		c.run();
	}

}
