/* *********************************************************************** *
 * project: org.matsim.*
 * PSSEventsFileBasedControler.java
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

package playground.wrashid.PSF2.tools.dumbCharging;

import java.util.LinkedList;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import playground.wrashid.PSF2.ParametersPSF2;
import playground.wrashid.PSF2.chargingSchemes.dumbCharging.PSSControlerDumbCharging;

/**
 * As a template config file, use:
 * test/input/playground/wrashid/PSF2/chargingSchemes
 * /dumbCharging/config-event-file-based.xml
 * 
 * adapt that file as follows: - adapt the plans, facilities and network file.
 * facilities file only needed, if used for scenario. - also set the property:
 * main.inputEventsForSimulationPath (path to events file) - actually the rest
 * of the content of this file is ignored (should be just run 1 iteraion! =>
 * else need to adapt it)
 * 
 * @author wrashid
 * 
 */
public class PSSEventsFileBasedControler {

	public static void main(String[] args) {

		
		
		
		
		String configFile=null;
		if (args.length>0){
			configFile = args[0];
		} else {
			System.err.println("config file not specified as argument!");
			
			System.exit(0);
			
			//configFile = "test/input/playground/wrashid/PSF2/chargingSchemes/dumbCharging/config-event-file-based.xml";
		}
		
		
		
		

		// TODO: here we could also add some filter later...

		PSSControlerDumbCharging pssControlerDumbCharging = new PSSControlerDumbCharging(configFile, null);

		pssControlerDumbCharging.prepareMATSimIterations();
		Controler controler = pssControlerDumbCharging.getControler();

		addControlerHandlerForReadingAllowedChargingLocationsParameter(controler);
		
		controler.setWriteEventsInterval(0);
		
		//addPHEVSampleFilter_Deprecated_use_EventsFilterSamplingInstead(controler);

		pssControlerDumbCharging.runControler();
	}

	private static void addControlerHandlerForReadingAllowedChargingLocationsParameter(Controler controler) {
		controler.addControlerListener(new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				LinkedList<String> allowedChargingLocations=new LinkedList<String>();
				
				
				String paramName="ParametersPSF2.allowedChargingLocations";
				try{
					String str=	event.getControler().getConfig().getParam("PSF", paramName);
					
					String[] activityLocations=str.split(",");
					
					for (String actLocation:activityLocations){
						actLocation=actLocation.trim();
						if (actLocation!=""){
							allowedChargingLocations.add(actLocation);
						}
					}
					
					if (allowedChargingLocations.size()>0){
						ParametersPSF2.setAllowedChargingLocations(allowedChargingLocations);
					} else {
						ParametersPSF2.setAllowedChargingLocations(null);
					}
					
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("config parameter missing in module PSF: " + paramName);
					System.exit(0);
				}
				
			}
		});
		
	}

	/*
	private static void addPHEVSampleFilter_Deprecated_use_EventsFilterSamplingInstead(Controler controler) {
		controler.addControlerListener(new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				double percentageOfPHEVs=100;
				String paramName="PHEVsAsPercentageOfWholePopulation";
				try{
					String str=	event.getControler().getConfig().getParam("PSF", paramName);
					percentageOfPHEVs=Integer.parseInt(str);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("config parameter missing in module PSF: " + paramName);
					System.exit(0);
				}
				
				ParametersPSF2.phevAgents = new LinkedList<Id>();

				Controler ctrl = event.getControler();

				Random rand = new Random();

				for (Id personId : ctrl.getPopulation().getPersons().keySet()) {
					if (percentageOfPHEVs / 100 > rand.nextDouble()) {
						ParametersPSF2.phevAgents.add(personId);
					}
				}

			}
		});
	}
*/
}
