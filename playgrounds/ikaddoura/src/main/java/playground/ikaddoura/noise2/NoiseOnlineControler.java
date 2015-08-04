/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.ikaddoura.noise2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.ikaddoura.noise2.data.GridParameters;
import playground.ikaddoura.noise2.data.NoiseAllocationApproach;
import playground.ikaddoura.noise2.data.NoiseContext;
import playground.ikaddoura.noise2.routing.NoiseTollDisutilityCalculatorFactory;

/**
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseOnlineControler {
	private static final Logger log = Logger.getLogger(NoiseOnlineControler.class);

	private static String configFile;
	private static String setup;
	private static String approach;

	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			
			configFile = args[0];
			log.info("Config file: " + configFile);
			
			setup = args[1];		
			log.info("Setup: " + setup);
			
			approach = args[2];		
			log.info("Approach: " + approach);
			
		} else {
			
			configFile = "/Users/ihab/Desktop/test/config.xml";
			setup = "berlin1";
			approach = "averageCost";
		}
				
		NoiseOnlineControler noiseImmissionControler = new NoiseOnlineControler();
		noiseImmissionControler.run(configFile);
	}

	private void run(String configFile) {
		
		// grid parameters
		
		GridParameters gridParameters = new GridParameters();
		
		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		gridParameters.setConsideredActivitiesForReceiverPointGrid(consideredActivitiesForReceiverPointGrid);
				
		if (setup.equals("berlin1")) {
			
			gridParameters.setReceiverPointGap(100.);
			
			String[] consideredActivitiesForDamages = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
			gridParameters.setConsideredActivitiesForDamages(consideredActivitiesForDamages);
		
		} else if (setup.equals("berlin2")) {
		
			gridParameters.setReceiverPointGap(100.);
	
			String[] consideredActivitiesForDamages = {"home"};
			gridParameters.setConsideredActivitiesForDamages(consideredActivitiesForDamages);
			
		} else {
			throw new RuntimeException("Unknown parameter setup. Aborting...");
		}
		
		// noise parameters
		
		NoiseParameters noiseParameters = new NoiseParameters();
		
		if (approach.equals("averageCost")) {
			noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.AverageCost);
		
		} else if (approach.equals("marginalCost")) {
			noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.MarginalCost);
		
		} else {
			throw new RuntimeException("Unknown noise allocation approach. Aborting...");
		}
		
		noiseParameters.setScaleFactor(10.);
		
		List<Id<Link>> tunnelLinkIDs = new ArrayList<Id<Link>>();
		tunnelLinkIDs.add(Id.create("108041", Link.class));
		tunnelLinkIDs.add(Id.create("108142", Link.class));
		tunnelLinkIDs.add(Id.create("108970", Link.class));
		tunnelLinkIDs.add(Id.create("109085", Link.class));
		tunnelLinkIDs.add(Id.create("109757", Link.class));
		tunnelLinkIDs.add(Id.create("109919", Link.class));
		tunnelLinkIDs.add(Id.create("110060", Link.class));
		tunnelLinkIDs.add(Id.create("110226", Link.class));
		tunnelLinkIDs.add(Id.create("110164", Link.class));
		tunnelLinkIDs.add(Id.create("110399", Link.class));
		tunnelLinkIDs.add(Id.create("96503", Link.class));
		tunnelLinkIDs.add(Id.create("110389", Link.class));
		tunnelLinkIDs.add(Id.create("110116", Link.class));
		tunnelLinkIDs.add(Id.create("110355", Link.class));
		tunnelLinkIDs.add(Id.create("92604", Link.class));
		tunnelLinkIDs.add(Id.create("92603", Link.class));
		tunnelLinkIDs.add(Id.create("25651", Link.class));
		tunnelLinkIDs.add(Id.create("25654", Link.class));
		tunnelLinkIDs.add(Id.create("112540", Link.class));
		tunnelLinkIDs.add(Id.create("112556", Link.class));
		tunnelLinkIDs.add(Id.create("5052", Link.class));
		tunnelLinkIDs.add(Id.create("5053", Link.class));
		tunnelLinkIDs.add(Id.create("5380", Link.class));
		tunnelLinkIDs.add(Id.create("5381", Link.class));
		tunnelLinkIDs.add(Id.create("106309", Link.class));
		tunnelLinkIDs.add(Id.create("106308", Link.class));
		tunnelLinkIDs.add(Id.create("26103", Link.class));
		tunnelLinkIDs.add(Id.create("26102", Link.class));
		tunnelLinkIDs.add(Id.create("4376", Link.class));
		tunnelLinkIDs.add(Id.create("4377", Link.class));
		tunnelLinkIDs.add(Id.create("106353", Link.class));
		tunnelLinkIDs.add(Id.create("106352", Link.class));
		tunnelLinkIDs.add(Id.create("103793", Link.class));
		tunnelLinkIDs.add(Id.create("103792", Link.class));
		tunnelLinkIDs.add(Id.create("26106", Link.class));
		tunnelLinkIDs.add(Id.create("26107", Link.class));
		tunnelLinkIDs.add(Id.create("4580", Link.class));
		tunnelLinkIDs.add(Id.create("4581", Link.class));
		tunnelLinkIDs.add(Id.create("4988", Link.class));
		tunnelLinkIDs.add(Id.create("4989", Link.class));
		tunnelLinkIDs.add(Id.create("73496", Link.class));
		tunnelLinkIDs.add(Id.create("73497", Link.class));
		noiseParameters.setTunnelLinkIDs(tunnelLinkIDs);
				
		// controler
		
		Controler controler = new Controler(configFile);

		NoiseContext noiseContext = new NoiseContext(controler.getScenario(), gridParameters, noiseParameters);
		final NoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new NoiseTollDisutilityCalculatorFactory(noiseContext);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
			}
		});
		controler.addControlerListener(new NoiseCalculationOnline(noiseContext));

		controler.addOverridingModule(new OTFVisModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
	}
	
}
