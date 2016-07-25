/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.controlerListner.ModalShareControlerListner;
import playground.agarwalamit.analysis.controlerListner.ModalTravelTimeControlerListner;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.counts.MultiModeCountsControlerListener;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.JointCalibrationControler;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.PatnaScoringFunctionFactory;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.router.BikeTimeDistanceTravelDisutilityFactory;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.combined.router.FreeSpeedTravelTimeForTruck;
import playground.agarwalamit.mixedTraffic.patnaIndia.ptFare.PtFareEventHandler;

/**
 * @author amit
 */

public class PatnaPolicyControler {
	
	private static String outputDir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/bau/";
	private static String configFile = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/input/config.xml.gz";
	private static boolean applyTrafficRestrain = false;
	private static boolean addBikeTrack = false;
	
	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		
		if(args.length>0){
			configFile = args[0];
			outputDir = args[1];
		
			applyTrafficRestrain = Boolean.valueOf(args[2]);
			addBikeTrack = Boolean.valueOf(args[3]);
		}
		
		ConfigUtils.loadConfig(config, configFile);
		config.controler().setOutputDirectory(outputDir);
				
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		// policies if any
		if (applyTrafficRestrain ) {
			PatnaTrafficRestrainer.run(scenario.getNetwork());
		} 
		
		if(addBikeTrack) {
			PatnaBikeTrackCreator.run(scenario.getNetwork());
		}
		
		final Controler controler = new Controler(scenario);
		
		
		controler.getConfig().controler().setDumpDataAtEnd(true);

		final BikeTimeDistanceTravelDisutilityFactory builder_bike =  new BikeTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());
		final RandomizingTimeDistanceTravelDisutilityFactory builder_truck =  new RandomizingTimeDistanceTravelDisutilityFactory("truck_ext", config.planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);

				addTravelTimeBinding("bike_ext").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike_ext").toInstance(builder_bike);

				addTravelTimeBinding("truck_ext").to(FreeSpeedTravelTimeForTruck.class);
				addTravelDisutilityFactoryBinding("truck_ext").toInstance(builder_truck);

				for(String mode : Arrays.asList("car_ext","motorbike_ext","motorbike")){
					addTravelTimeBinding(mode).to(networkTravelTime());
					addTravelDisutilityFactoryBinding(mode).to(carTravelDisutilityFactoryKey());					
				}
			}
		});

		controler.addOverridingModule(new AbstractModule() { // ploting modal share over iterations
			@Override
			public void install() {
				this.bind(ModalShareEventHandler.class);
				this.addControlerListenerBinding().to(ModalShareControlerListner.class);

				this.bind(ModalTripTravelTimeHandler.class);
				this.addControlerListenerBinding().to(ModalTravelTimeControlerListner.class);

				this.addControlerListenerBinding().to(MultiModeCountsControlerListener.class);
			}
		});

		// adding pt fare system based on distance 
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().to(PtFareEventHandler.class);
			}
		});
		// for above make sure that util_dist and monetary dist rate for pt are zero.
		ModeParams mp = controler.getConfig().planCalcScore().getModes().get("pt");
		mp.setMarginalUtilityOfDistance(0.0);
		mp.setMonetaryDistanceRate(0.0);


		// add income dependent scoring function factory
		controler.setScoringFunctionFactory(new PatnaScoringFunctionFactory(controler.getScenario())) ;
		
		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = outputDir+"/ITERS/it."+index;
			Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectory(new File(dirToDel),false);
		}

		new File(outputDir+"/analysis/").mkdir();
		String outputEventsFile = outputDir+"/output_events.xml.gz";
		// write some default analysis
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile);
		mtta.run();
		mtta.writeResults(outputDir+"/analysis/modalTravelTime.txt");

		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile);
		msc.run();
		msc.writeResults(outputDir+"/analysis/modalShareFromEvents.txt");

		StatsWriter.run(outputDir);
	}
}
