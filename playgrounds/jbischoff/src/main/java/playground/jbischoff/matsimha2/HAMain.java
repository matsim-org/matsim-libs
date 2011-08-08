/* *********************************************************************** *
 * project: org.matsim.*
 * CottbusMain
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.jbischoff.matsimha2;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import playground.jbischoff.BAsignals.controler.JBSignalControllerListenerFactory;


/**
 * @author dgrether
 *
 */
public class HAMain {
	
	
	
	private static final Logger log = Logger.getLogger(HAMain.class);
	
	
	
	public void runCottbus(String c){
		log.info("Running HAMain with config: " + c);
		Controler controler = new Controler(c);
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
	

	

	/**
	 * @param args
	 */
	public static void main(String[] args)

	{
		 String config = "C:\\Users\\Joschka Bischoff\\workspace_new\\matsim\\examples\\two-routes\\config.xml";

		if (args == null || args.length == 0){
			new HAMain().runCottbus(config);
		}
		else if (args.length == 1){
			new HAMain().runCottbus(args[0]);
		}
		else {
			log.error("too many arguments!");
		}
		
	}


}
