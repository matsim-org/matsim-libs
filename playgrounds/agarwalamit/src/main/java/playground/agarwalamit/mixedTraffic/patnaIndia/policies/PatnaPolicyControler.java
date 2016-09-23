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
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ScoringParameterSet;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelMoneyScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.core.scoring.functions.CharyparNagelScoringParametersForPerson;
import org.matsim.core.scoring.functions.SubpopulationCharyparNagelScoringParameters;
import org.matsim.core.utils.io.IOUtils;

import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.controlerListner.ModalShareControlerListner;
import playground.agarwalamit.analysis.controlerListner.ModalTravelTimeControlerListner;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.counts.MultiModeCountsControlerListener;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.BikeTimeDistanceTravelDisutilityFactory;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForTruck;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter.PatnaUserGroup;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * @author amit
 */

public class PatnaPolicyControler {

	private static String dir = "../../../../repos/runs-svn/patnaIndia/run108/jointDemand/policies/0.15pcu/";
	private static boolean applyTrafficRestrain = false;
	private static boolean addBikeTrack = false;
	private static boolean isAllwoingMotorbikeOnBikeTrack = true;

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();

		String outputDir ;

		if(args.length>0){
			dir = args[0];
			applyTrafficRestrain = Boolean.valueOf(args[1]);
			addBikeTrack = Boolean.valueOf(args[2]);
			isAllwoingMotorbikeOnBikeTrack = Boolean.valueOf(args[3]);
			outputDir = dir+args[4];
		}  else {
			if(applyTrafficRestrain ) {
				if (isAllwoingMotorbikeOnBikeTrack) throw new RuntimeException("Two situations -- traffic restrain and motorbike on bike track -- are not considered.");
				if (addBikeTrack) outputDir = dir+"/both/";
				else outputDir = dir+"/trafficRestrain/";
			} else if(addBikeTrack && !isAllwoingMotorbikeOnBikeTrack) outputDir = dir+"/bikeTrack/";
			else if(isAllwoingMotorbikeOnBikeTrack) outputDir = dir+"/BT-mb/";
			else outputDir = dir+"/baseCaseCtd/";			
		}

		String inputDir = dir+"/input/";
		String configFile = inputDir + "configBaseCaseCtd.xml";

		ConfigUtils.loadConfig(config, configFile);

		config.controler().setOutputDirectory(outputDir);

		//==
		// after calibration;  departure time is fixed for urban; check if time choice is not present
		Collection<StrategySettings> strategySettings = config.strategy().getStrategySettings();
		for(StrategySettings ss : strategySettings){ // departure time is fixed now.
			if ( ss.getStrategyName().equals(DefaultStrategy.TimeAllocationMutator.toString()) ) {
				throw new RuntimeException("Time mutation should not be used; fixed departure time must be used after cadyts calibration.");
			}
		}
		//==

		//==
		// take only selected plans so that time for urban and location for external traffic is fixed.
		// not anymore, there is now second calibration after cadyts and before baseCaseCtd; thus all plans in the choice set.
		String inPlans = "baseCaseOutput_plans.xml.gz";
		config.plans().setInputFile(inputDir + inPlans);
		//==

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWriteEventsInterval(1);

		// policies if any
		if (applyTrafficRestrain && addBikeTrack ) {
			config.network().setInputFile(inputDir + "/networkWithTrafficRestricationAndBikeTrack.xml.gz");
		} else if (applyTrafficRestrain ) {
			config.network().setInputFile(inputDir + "/networkWithTrafficRestrication.xml.gz");
		} else if(addBikeTrack && !isAllwoingMotorbikeOnBikeTrack) {
			config.network().setInputFile(inputDir + "/networkWithBikeTrack.xml.gz");
		} else if (isAllwoingMotorbikeOnBikeTrack) {
			config.network().setInputFile(inputDir + "/networkWithBikeMotorbikeTrack.xml.gz");
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);

		// removal of some links may lead to exception if routes are not removed from leg.
		// do this before setting a new network so that cord from removed links can be extracted.
		if(applyTrafficRestrain ) removeRoutes(scenario, inputDir+"/network.xml.gz"); 

		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.getConfig().strategy().setMaxAgentPlanMemorySize(10);

		final BikeTimeDistanceTravelDisutilityFactory builder_bike =  new BikeTimeDistanceTravelDisutilityFactory("bike", config.planCalcScore());
		final RandomizingTimeDistanceTravelDisutilityFactory builder_truck =  new RandomizingTimeDistanceTravelDisutilityFactory("truck", config.planCalcScore());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				addTravelTimeBinding("bike").to(FreeSpeedTravelTimeForBike.class);
				addTravelDisutilityFactoryBinding("bike").toInstance(builder_bike);

				addTravelTimeBinding("truck").to(FreeSpeedTravelTimeForTruck.class);
				addTravelDisutilityFactoryBinding("truck").toInstance(builder_truck);

				addTravelTimeBinding("motorbike").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("motorbike").to(carTravelDisutilityFactoryKey());					
			}
		});

		controler.addOverridingModule(new AbstractModule() { // plotting modal share over iterations
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
		addScoringFunction(controler);

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
		String userGroup = PatnaUserGroup.urban.toString();
		ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile, userGroup, new PatnaPersonFilter());
		mtta.run();
		mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

		ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new PatnaPersonFilter());
		msc.run();
		msc.writeResults(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt");

		StatsWriter.run(outputDir);
	}

	private static void addScoringFunction(final Controler controler){
		// scoring function
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			final CharyparNagelScoringParametersForPerson parameters = new SubpopulationCharyparNagelScoringParameters( controler.getScenario() );
			@Inject Network network;
			@Inject Population population;
			@Inject PlanCalcScoreConfigGroup planCalcScoreConfigGroup; // to modify the util parameters
			@Inject ScenarioConfigGroup scenarioConfig;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final CharyparNagelScoringParameters params = parameters.getScoringParameters( person );

				SumScoringFunction sumScoringFunction = new SumScoringFunction();
				sumScoringFunction.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				sumScoringFunction.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				Double ratioOfInc = 1.0;

				if ( PatnaPersonFilter.isPersonBelongsToUrban(person.getId())) { // inc is not available for commuters and through traffic
					Double monthlyInc = (Double) population.getPersonAttributes().getAttribute(person.getId().toString(), PatnaUtils.INCOME_ATTRIBUTE);
					Double avgInc = PatnaUtils.MEADIAM_INCOME;
					ratioOfInc = avgInc/monthlyInc;
				}

				planCalcScoreConfigGroup.setMarginalUtilityOfMoney(ratioOfInc );				

				ScoringParameterSet scoringParameterSet = planCalcScoreConfigGroup.getScoringParameters( null ); // parameters set is same for all subPopulations 

				CharyparNagelScoringParameters.Builder builder = new CharyparNagelScoringParameters.Builder(
						planCalcScoreConfigGroup, scoringParameterSet, scenarioConfig);
				final CharyparNagelScoringParameters modifiedParams = builder.build();

				sumScoringFunction.addScoringFunction(new CharyparNagelLegScoring(modifiedParams, network));
				sumScoringFunction.addScoringFunction(new CharyparNagelMoneyScoring(modifiedParams));
				return sumScoringFunction;
			}
		});
	}

	private static void removeRoutes(final Scenario scenario, final String baseCaseNetwork){
		// this is required because routes are generated from initial base case network; and new network does not have certain links.
		Scenario scNetwork = LoadMyScenarios.loadScenarioFromNetwork(baseCaseNetwork); 

		//since some links are now removed, route in the plans will throw exception, remove them.
		for (Person p : scenario.getPopulation().getPersons().values()){
			for(Plan plan : p.getPlans()){
				List<PlanElement> pes = plan.getPlanElements();
				for (PlanElement pe :pes ){
					if (pe instanceof Activity) { 
						Activity act = ((Activity)pe);
						Id<Link> linkId = act.getLinkId();
						Coord cord = act.getCoord();

						if (cord == null && linkId == null) throw new RuntimeException("Activity "+act.toString()+" do not have either of link id or coord. Aborting...");
						else if (linkId == null ) { /*nothing to do; cord is assigned*/ }
						else if (cord==null && ! scNetwork.getNetwork().getLinks().containsKey(linkId)) throw new RuntimeException("Activity "+act.toString()+" do not have cord and link id is not present in network. Aborting...");
						else {
							cord = scNetwork.getNetwork().getLinks().get(linkId).getCoord();
							act.setLinkId(null);
							act.setCoord(cord);
						}
					} else if ( pe instanceof Leg){
						Leg leg = (Leg) pe;
						leg.setRoute(null);
					}
				}
			}
		}
	}
}
