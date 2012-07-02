/* *********************************************************************** *
 * project: org.matsim.*
 * TaSingleCrossingMain
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
package playground.dgrether.signalsystems.tacontrol;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import playground.dgrether.signalsystems.tacontrol.controler.DgTaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class TaSingleCrossingMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		
		
		Controler controler = new Controler(config);
		controler.setSignalsControllerListenerFactory(new DgTaControlerListenerFactory());
		controler.setOverwriteFiles(true);
		controler.run();

	}

}
