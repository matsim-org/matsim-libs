/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package robotaxi.run;

import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.run.RunRobotaxiExample;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.MultiModeTaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RunTaxiBatchScenario {

	public static void main(String[] args) {
		
		for (int i = 1000; i< 10000; i+=5000){
			//laedt eine "Basisconfig" inkl. der Nicht-Standard-Config-Gruppen für Taxis etc.
			Config config = ConfigUtils.loadConfig("config.xml", new MultiModeTaxiConfigGroup(),
					new TaxiFareConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
			
			
			//ueberschreibt den Wert für die Flottendatei

			TaxiConfigGroup tcg = (TaxiConfigGroup) config.getModules().get(TaxiConfigGroup.GROUP_NAME);
			tcg.setTaxisFile("taxis_"+i+".xml");
			String runId = "run"+i;
			config.controler().setRunId(runId);
			config.controler().setOutputDirectory("somewhere/output/"+runId);
			
			// erstellt den Controler aus dem Robotaxiexample auf basis der aktuellen Config
			Controler controler = RunRobotaxiExample.createControler(config, false);
			
			controler.run();
		}
	}
	}
