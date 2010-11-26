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
package playground.jbischoff.BAsignals;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;

import playground.jbischoff.BAsignals.controler.JBSignalControllerListenerFactory;


/**
 * @author dgrether
 *
 */
public class CottbusMain {
	
	
	private static final Logger log = Logger.getLogger(CottbusMain.class);

//	private String config = JbBaPaths.BASIMH+"scenario-slv/cottbusConfig.xml";
//	private String config = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-lsa/cottbusConfig.xml";
	private String config = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-slv/dg_cottbus_config.xml";

	public void runCottbus(String c){
		log.info("Running CottbusMain with config: " + c);
		Controler controler = new Controler(c);
		JBSignalControllerListenerFactory fact = new JBSignalControllerListenerFactory();
		controler.setSignalsControllerListenerFactory(fact);
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
	private void runCottbus(){
		this.runCottbus(config);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args == null || args.length == 0){
			new CottbusMain().runCottbus();
		}
		else if (args.length == 1){
			new CottbusMain().runCottbus(args[0]);
		}
		else {
			log.error("too many arguments!");
		}
		
	}


}
