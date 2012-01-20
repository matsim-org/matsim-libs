/* *********************************************************************** *
 * project: org.matsim.*
 * Daganzo2012Run
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
package playground.dgrether.daganzo2012;

import org.matsim.core.controler.Controler;

import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class Daganzo2012Run {

	public static void main(String[] args) {
//		String config = args[0];
		String config = "/media/data/work/repos/shared-svn/studies/dgrether/daganzo2012/daganzo_2012_config.xml";
		Controler controler = new Controler(config);
		controler.setSignalsControllerListenerFactory(new DgSylviaControlerListenerFactory());
		controler.setOverwriteFiles(true);
		controler.run();

	}

}
