/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kairuns.run;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile.OutputFormat;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
 * @author nagel
 *
 */
final class BerlinUtils {
//	static class PtTravelTime implements TravelTime {
//		@Override public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
//			return 1.8 * link.getLength() / link.getFreespeed(time) ; // pt as 2 x car free speed.  Reducing this because it comes out too high.
//		}
//	}

	static class BikeTravelTime implements TravelTime {
		@Override public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return link.getLength() / Math.min( link.getFreespeed(time) , 15./3.6 ) ; // google maps assumes an average speed of 15km/h but I think that's unrealistic
			// 12.3 see http://www.urbanist-magazin.de/2015/06/das-konzept-der-effektiven-geschwindigkeit/
			// which points to http://www.stadtentwicklung.berlin.de/verkehr/politik_planung/zahlen_fakten/download/Mobilitaet_dt_komplett.pdf
			// Setting this to 15 after all since we are stuck in car congestion.
		}
	}

	static class WalkTravelTime implements TravelTime {
		@Override public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return link.getLength() / Math.min( link.getFreespeed(time) , 4.3/3.6 ) ;
		}
	}

	private BerlinUtils(){} // do not instantiate

	static void unterLindenQuiet(Network network) {
		List<String> strs = new ArrayList<>() ;
		// northern side (going from west to east):
		strs.add("24913304_25662562_25664713-24913304_25664713_271373068-24913304_271373068_21487180") ;
		strs.add("4611699_21487242_272408975-4611699_272408975_25662562") ;
		strs.add("4611699_27005148_25664841-4611699_25664841_21487242") ;
		strs.add("4611699_27005148_25664841-4611699_25664841_21487242") ;
		strs.add("4067932_29207434_29194639") ;
		strs.add("15972319_524739197_29207434") ;
		strs.add("24913519_29221504_768082167-24913519_768082167_669878077-24913519_669878077_25665809-24913519_25665809_31160079-24913519_31160079_611330949-24913519_611330949_160503256-15972319_160503256_524739197") ;
	
		// initially forgotten:
		strs.add("61584187_29194639_29194675-61584187_29194675_258127852-61584187_258127852_29194673-61584187_29194673_29194671-61584187_29194671_29194669-61584187_29194669_21487183-54048863_21487183_160493853-15971197_160493853_160493846-4611699_160493846_29193497-4611699_29193497_25665646-4611699_25665646_25665636-4611699_25665636_271296293-4611699_271296293_262455378-4611699_262455378_262455379-4611699_262455379_27005148") ;
	
		// southern side (going from west to east):
		strs.add("24240246_25662552_271388393-24240246_271388393_25664742-24240246_25664742_25661397") ;
		strs.add("24913297_25661397_25662689") ;
		strs.add("24913297_25662689_272409455-24913297_272409455_25664779-24913297_25664779_538796570-24913297_538796570_450169274-24913297_450169274_262455363-24913297_262455363_29207420-24913297_29207420_538795322-24913297_538795322_687892314-24913297_687892314_538795142-24913297_538795142_25664959-24913297_25664959_272411798-24913297_272411798_542370615-24913297_542370615_25665029-66514480_25665029_29207429-66514480_29207429_29207422-56927201_29207422_160493838-15971193_160493838_160493831-15971196_160493831_25665007-54048862_25665007_25665987-54048862_25665987_441784215-54048862_441784215_258127856-54048862_258127856_441784220-54048862_441784220_258127854-54048862_258127854_697323163-54048862_697323163_697323164") ;
		strs.add("54048862_697323164_441784236-54048862_441784236_29207821") ;
		strs.add("23013165_29207821_160503259") ;
		strs.add("4611698_160503259_341340983-4611698_341340983_171440823-4611698_171440823_25665988-4611698_25665988_341340986") ;
		strs.add("4611698_341340986_29221506") ;
	
		for ( String str : strs ) { 
			Link link = network.getLinks().get( Id.createLinkId(str) ) ;
			Gbl.assertNotNull(link);
			link.setFreespeed( 2/3.6); // 4km/h = pedestrian speed ... allow local cars to access/egress
		}
	}

	static void setTunnelLinkIds(NoiseConfigGroup noiseParameters) {
		// yyyyyy Same link ids?  Otherwise ask student
		Set<Id<Link>> tunnelLinkIDs = new HashSet<>();
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
		noiseParameters.setTunnelLinkIDsSet(tunnelLinkIDs);
	}

	static void createActivityParameters(Config config) {
		{
			ActivityParams params = new ActivityParams("home") ;
			params.setTypicalDuration(12*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("not specified") ;
			params.setTypicalDuration(0.5*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(22*3600);
			config.planCalcScore().addActivityParams(params);
			// yyyy should probably be same as "home" in many situations: first activity of day
		}
		{
			ActivityParams params = new ActivityParams("leisure") ;
			params.setTypicalDuration(2*3600);
			params.setOpeningTime(10*3600);
			params.setClosingTime(22*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("shopping") ;
			params.setTypicalDuration(1*3600);
			params.setOpeningTime(8*3600);
			params.setClosingTime(20*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("work") ;
			params.setTypicalDuration(9*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(19*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("education") ;
			params.setTypicalDuration(8*3600);
			params.setOpeningTime(8*3600);
			params.setClosingTime(18*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("business") ;
			params.setTypicalDuration(1*3600);
			params.setOpeningTime(9*3600);
			params.setClosingTime(20*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("multiple") ;
			params.setTypicalDuration(0.5*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(22*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("other") ;
			params.setTypicalDuration(0.5*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(22*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("see a doctor") ;
			params.setTypicalDuration(1*3600);
			params.setOpeningTime(6*3600);
			params.setClosingTime(20*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("holiday / journey") ;
			params.setTypicalDuration(20*3600);
			config.planCalcScore().addActivityParams(params);
		}
		{
			ActivityParams params = new ActivityParams("dummy") ; // Personenwirtschaftsverkehr von Sebastian Schneider
			params.setTypicalDuration(1*3600);
			config.planCalcScore().addActivityParams(params);
		}
	}

	static void mergeNoiseFiles(String outputFilePath) {
		final String receiverPointsFile = outputFilePath + "/receiverPoints/receiverPoints.csv" ;
	
		final String[] labels = { "immission", "consideredAgentUnits", "damages_receiverPoint" };
		final String[] workingDirectories = { outputFilePath + "/immissions/" , outputFilePath + "/consideredAgentUnits/", outputFilePath + "/damages_receiverPoint/" };
	
	
		MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
		merger.setWorkingDirectory(workingDirectories);
		merger.setReceiverPointsFile(receiverPointsFile);
		merger.setLabel(labels);
		merger.setOutputFormat(OutputFormat.xyt);
		merger.setThreshold(1.);
		merger.setOutputDirectory(outputFilePath);
		merger.run();
	
	}

	static void createAndAddModeVehicleTypes(Vehicles vehicles) {
		{
			String mode = TransportMode.car ;
			VehicleType type = VehicleUtils.getFactory().createVehicleType(Id.create(mode, VehicleType.class));
			type.setMaximumVelocity(200./3.6);
			type.setPcuEquivalents(1.);
			vehicles.addVehicleType(type);
		}
		{
			String mode = TransportMode.bike ;
			VehicleType type = VehicleUtils.getFactory().createVehicleType(Id.create(mode, VehicleType.class));
			type.setMaximumVelocity(10.0/3.6);
			type.setPcuEquivalents(0.05);
			vehicles.addVehicleType(type);
		}
	}

	static void setStrategies(Config config, boolean equil, boolean modeChoice) {
		{
			StrategySettings stratSets = new StrategySettings( ) ;
			stratSets.setStrategyName( DefaultSelector.ChangeExpBeta.name() ) ;
			stratSets.setWeight(0.9);
			config.strategy().addStrategySettings(stratSets);
		}
		{
			StrategySettings stratSets = new StrategySettings( ) ;
			stratSets.setStrategyName( DefaultStrategy.ReRoute.name() ) ;
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
			if ( !equil ) {
				config.plansCalcRoute().setInsertingAccessEgressWalk(true);
			}
		}
		if ( modeChoice ) {
			StrategySettings stratSets = new StrategySettings( ) ;
			stratSets.setStrategyName( DefaultStrategy.ChangeSingleTripMode.name() );
			stratSets.setWeight(0.1);
			config.strategy().addStrategySettings(stratSets);
			if ( equil ) {
				config.changeMode().setModes(new String[] {"car","pt"});
			} else {
				config.changeMode().setModes(new String[] {"walk","bike","car","pt"});
			}
		}
		{
//			if ( timeChoice ) {
//				StrategySettings stratSets = new StrategySettings( ) ;
//				stratSets.setStrategyName( DefaultStrategy.TimeAllocationMutator.name() );
//				stratSets.setWeight(0.1);
//				config.strategy().addStrategySettings(stratSets);
//			}
			config.timeAllocationMutator().setMutationRange(7200.);    // pacify consistency checker
			config.timeAllocationMutator().setAffectingDuration(false); // dto.
		}
	}

	@SuppressWarnings("unused")
	private static void computeNoise(final double sampleFactor, Config config, Scenario scenario) {
		// noise parameters
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) scenario.getConfig().getModule("noise");
	
		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivitiesForReceiverPointGrid);
	
		noiseParameters.setReceiverPointGap(2000.); // 200 is possible but overkill as long as this is not debugged.
	
		String[] consideredActivitiesForDamages = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);
	
		noiseParameters.setScaleFactor(1./sampleFactor); // yyyyyy sample size!!!!
	
		setTunnelLinkIds(noiseParameters);
	
		// ---
	
		String outputDirectory = config.controler().getOutputDirectory()+"/noise/" ;
	
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		noiseCalculation.run();		
	
		// ---
	
		String outputFilePath = outputDirectory + "analysis_it." + config.controler().getLastIteration() + "/";
		mergeNoiseFiles(outputFilePath);
	}

	static void createMultimodalParameters(Config config) {
		config.qsim().setVehiclesSource(VehiclesSource.modeVehicleTypesFromVehiclesData);
		config.qsim().setLinkDynamics(LinkDynamics.PassingQ.name()); // I think this is ok even when not needed.

		// network modes for router:
		Collection<String> networkModes = new ArrayList<>() ;
		config.plansCalcRoute().setNetworkModes(networkModes);
		
		// network modes for qsim:
		Collection<String> mainModes = Arrays.asList( new String[] { TransportMode.car } ) ;
		// (not including walk here since otherwise walk would be stuck between congested cars)
		config.qsim().setMainModes(mainModes);
	
		{
			String mode = TransportMode.walk ;

			ModeRoutingParams pars = new ModeRoutingParams(mode) ;
			pars.setTeleportedModeFreespeedFactor(1.); 
			pars.setTeleportedModeFreespeedLimit(5./3.6);
			config.plansCalcRoute().addModeRoutingParams(pars);

			// scoring:
			ModeParams params = new ModeParams( mode ) ;
			params.setConstant(0);
			params.setMarginalUtilityOfTraveling(-1.); // this should be by distance, but with const spd does not matter
			config.planCalcScore().addModeParams(params);
		}
		{
			String mode = TransportMode.bike ;

			ModeRoutingParams pars = new ModeRoutingParams(mode) ;
			pars.setTeleportedModeFreespeedFactor(1.); 
			pars.setTeleportedModeFreespeedLimit(14.5/3.6);
			config.plansCalcRoute().addModeRoutingParams(pars);

			// scoring:
			ModeParams params = new ModeParams( mode ) ;
			params.setConstant(-1.); // maybe some initiation costs compared to walk
			params.setMarginalUtilityOfTraveling(-4.);
			config.planCalcScore().addModeParams(params);
		}
		{
			String mode = TransportMode.pt ;
			
			ModeRoutingParams pars = new ModeRoutingParams(mode) ;
			pars.setTeleportedModeFreespeedFactor(2.1); 
			config.plansCalcRoute().addModeRoutingParams(pars);

//			networkModes.add(mode) ;

			// scoring:
			ModeParams params = new ModeParams(mode) ;
			params.setConstant(-3.); // (parameterizes access/egress walk)
			params.setMarginalUtilityOfTraveling(0.);
			config.planCalcScore().addModeParams(params);
		}
		{
			String mode = TransportMode.car ;

			networkModes.add(mode) ;
			
			// scoring:
			ModeParams params = new ModeParams(mode) ;
			params.setConstant(-0.); // worse than bike but better than pt.  But may include some fixed cost ...
			params.setMarginalUtilityOfTraveling(0.);
			params.setMonetaryDistanceRate(-0.10/1000.); // recall that this is Eu/m (!)
			config.planCalcScore().addModeParams(params);
		}
		{
			String mode = "undefined" ;
			
			ModeRoutingParams pars = new ModeRoutingParams(mode) ;
			pars.setBeelineDistanceFactor(1.3);
			pars.setTeleportedModeSpeed( 50./3.6 );
			config.plansCalcRoute().addModeRoutingParams(pars);
	
			ModeParams params = new ModeParams(mode) ;
			params.setConstant(-6.);
			params.setMarginalUtilityOfTraveling(0.); // yyyy should make this very unattractive so it dies out
			config.planCalcScore().addModeParams(params);
		}
		
		// yyyy missing is "in car as passenger"
	}

}
