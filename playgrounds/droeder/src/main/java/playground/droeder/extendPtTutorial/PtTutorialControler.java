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
package playground.droeder.extendPtTutorial;

import java.io.File;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

/**
 * @author droeder
 *
 */
class PtTutorialControler {

	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PtTutorialControler.class);

	public static void main(String[] args) {
		if(! new File("../../org.matsim/examples/pt-tutorial/configExtended.xml").exists()){
			ExtendPtTutorial.main(null);
		}
		Controler c = new Controler("../../org.matsim/examples/pt-tutorial/configExtended.xml");
		c.setOverwriteFiles(true);
		c.run();
	}
}

