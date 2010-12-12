/* *********************************************************************** *
 * project: org.matsim.*
 * RunSanral.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import playground.jjoubert.roadpricing.senozon.SanralControler;

public class RunSanralTolled {
	private static String configFilename;
	private final static Logger log = Logger.getLogger(RunSanralTolled.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 1){
			throw new RuntimeException("Must provide a config file.");
		} else{
			configFilename = args[0];
		}
		log.info("======================================================");
		log.info(" 	   Running the SANRAL project WITH the new toll.");
		log.info("------------------------------------------------------");
		log.info(" " + configFilename);
				
		Controler c = new SanralControler(configFilename);
		c.run();
	}

}
