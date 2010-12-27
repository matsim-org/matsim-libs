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
public class CottbusMain {
	
	
	public static double CURRENT_TT;
	public static double CURRENT_TTA;


	private static final Logger log = Logger.getLogger(CottbusMain.class);
	private String config = "/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/config_slv_l.xml";
//	private String config = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-lsa/cottbusConfig.xml";
//	private String config = "/media/data/work/repos/shared-svn/studies/dgrether/cottbus/Cottbus-BA/scenario-slv/dg_cottbus_config.xml";

	public void runCottbus(String c){
		log.info("Running CottbusMain with confikk: " + c);
		Controler controler = new Controler(c);
		controler.getConfig().controler().setOutputDirectory("/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/output-slv_ot100ii/");
		JBSignalControllerListenerFactory fact = new JBSignalControllerListenerFactory();
		controler.setSignalsControllerListenerFactory(fact);
		controler.setOverwriteFiles(true);
		controler.run();
	}
	
	
	public void runCottbusBatch(String c){
		log.info("Running CottbusMainBatch with config: " + c);
		String configlsa = "/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/config_lsa.xml";
		String configslv = "/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/config_slv.xml";
		int scale = 0;
		do {
			Controler controler = new Controler(configlsa);
			controler.getConfig().controler().setOutputDirectory("/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/output-lsa/"+scale);
			controler.getConfig().plans().setInputFile("/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/planswithfb/output_plans_"+scale+".xml.gz");
			JBSignalControllerListenerFactory fact = new JBSignalControllerListenerFactory();
			controler.setSignalsControllerListenerFactory(fact);
			controler.setOverwriteFiles(true);
			controler.run();
			
			controler = new Controler(configslv);
			controler.getConfig().controler().setOutputDirectory("/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/output-slv/x"+scale+"/");
			controler.getConfig().plans().setInputFile("/Users/JB/Desktop/BA-Arbeit/sim/scen/1211/planswithfb/output_plans_"+scale+".xml.gz");
			fact = new JBSignalControllerListenerFactory();
			controler.setSignalsControllerListenerFactory(fact);
			controler.setOverwriteFiles(true);
			controler.run();
			
			
			scale = scale + 5;

		}
		while (scale<=100);


	}
	
	private void runCottbus(){
		this.runCottbusBatch(config);
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
