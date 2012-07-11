/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.ikaddoura.parkAndRide.pR;


import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.pt.PtConstants;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.ikaddoura.parkAndRide.pRscoring.BvgScoringFunctionConfigGroupPR;
import playground.ikaddoura.parkAndRide.pRscoring.BvgScoringFunctionFactoryPR;

/**
 * @author Ihab
 *
 */
public class ParkAndRideMain {
	
	static String configFile;
	static String prFacilityFile;
	
	static int prCapacity;
	
	static double addRemoveProb;
	static int addRemoveDisable;
	
	static double changeLocationProb;
	static int changeLocationDisable;
	
	static double timeAllocationProb;
	static int timeAllocationDisable;
	
	public static void main(String[] args) throws IOException {
		
		configFile = "../../shared-svn/studies/ihab/parkAndRide/inputBerlinTest/berlinConfigTEST.xml";
		prFacilityFile = "../../shared-svn/studies/ihab/parkAndRide/inputBerlinTest/PRfacilities_berlin.txt";
		prCapacity = 100;
		
		addRemoveProb = 100;
		addRemoveDisable = 500;
		
		changeLocationProb = 0.1;
		changeLocationDisable = 500;
		
		timeAllocationProb = 0.1;
		timeAllocationDisable = 500;
		
		
//		**************************************************
		
//		configFile = args[0];
//		prFacilityFile = args[1];
//		prCapacity = Integer.parseInt(args[2]);
//		
//		addRemoveProb = Double.parseDouble(args[3]);
//		addRemoveDisable = Integer.parseInt(args[4]);
//		
//		changeLocationProb = Double.parseDouble(args[5]);
//		changeLocationDisable = Integer.parseInt(args[6]);
//		
//		timeAllocationProb = Double.parseDouble(args[7]);
//		timeAllocationDisable = Integer.parseInt(args[8]);
	
		ParkAndRideMain main = new ParkAndRideMain();
		main.run();
	}
	
	private void run() {
		
		Controler controler = new Controler(configFile);
		controler.setOverwriteFiles(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		
		PRFileReader prReader = new PRFileReader(prFacilityFile);
		Map<Id, ParkAndRideFacility> id2prFacility = prReader.getId2prFacility();

		final AdaptiveCapacityControl adaptiveControl = new AdaptiveCapacityControl(id2prFacility, prCapacity);
				
		// add "pt interaction" cause controler.init() is called too late and in a protected way
		ActivityParams transitActivityParams = new ActivityParams(PtConstants.TRANSIT_ACTIVITY_TYPE);
		transitActivityParams.setTypicalDuration(120.0);
		controler.getConfig().planCalcScore().addActivityParams(transitActivityParams);
		
		controler.setScoringFunctionFactory(new BvgScoringFunctionFactoryPR(controler.getConfig().planCalcScore(), new BvgScoringFunctionConfigGroupPR(controler.getConfig()), controler.getNetwork()));

		controler.addControlerListener(new ParkAndRideControlerListener(controler, adaptiveControl, id2prFacility, addRemoveProb, addRemoveDisable, changeLocationProb, changeLocationDisable, timeAllocationProb, timeAllocationDisable));
		
		final MobsimFactory mf = new QSimFactory();
		controler.setMobsimFactory(new MobsimFactory() {
			private QSim mobsim;

			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				mobsim = (QSim) mf.createMobsim(sc, eventsManager);
				mobsim.addMobsimEngine(adaptiveControl);
				return mobsim;
			}
		});
			
		controler.run();
	}
}
	
