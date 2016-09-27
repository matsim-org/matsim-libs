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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.bikeTrack;

import java.io.File;
import java.util.*;
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule.DefaultStrategy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.*;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.StatsWriter;
import playground.agarwalamit.analysis.controlerListner.ModalShareControlerListner;
import playground.agarwalamit.analysis.controlerListner.ModalTravelTimeControlerListner;
import playground.agarwalamit.analysis.linkVolume.FilteredLinkVolumeHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareEventHandler;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.analysis.travelTime.ModalTravelTimeAnalyzer;
import playground.agarwalamit.analysis.travelTime.ModalTripTravelTimeHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.BikeTimeDistanceTravelDisutilityFactory;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForBike;
import playground.agarwalamit.mixedTraffic.patnaIndia.router.FreeSpeedTravelTimeForTruck;
import playground.agarwalamit.mixedTraffic.patnaIndia.scoring.PtFareEventHandler;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaPersonFilter;
import playground.agarwalamit.mixedTraffic.patnaIndia.utils.PatnaUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;
import playground.agarwalamit.utils.MapUtils;

/**
 * @author amit
 */

public class PatnaBikeTrackConnectionControler2 {

	private static String dir = FileUtils.RUNS_SVN+"/patnaIndia/run108/jointDemand/policies/0.15pcu/";

	private static String initialNetwork = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/network.xml.gz";
	private static String bikeTrack = PatnaUtils.INPUT_FILES_DIR + "/simulationInputs/network/shpNetwork/bikeTrack.xml.gz";

	private static final Logger LOG = Logger.getLogger(BikeConnectorLinkControlerListner.class);

	private static final List<String> modes = Arrays.asList("bike");
	private static final Set<String> allowedModes = new HashSet<>(modes);
	private static final double blendFactor = 0.95;
	private static int numberOfConnectors = 15;
	private static int updateConnectorsAfterIteration = 1;
	private static int maxItration = 100;

	private static boolean isAllwoingMotorbikeOnBikeTrack = false;

	public static void main(String[] args) {


		if(args.length>0){
			dir= args[0];
			initialNetwork = args[1];
			bikeTrack = args[2];

			numberOfConnectors = Integer.valueOf(args[3]);
			updateConnectorsAfterIteration = Integer.valueOf(args[4]);
			maxItration = Integer.valueOf(args[5]);
		}

		Map<Id<Link>, Link> linkIds = new HashMap<>(); // just to keep information about links
		SortedMap<Id<Link>,Double> linkId2Count = new TreeMap<>(); // need to update the counts after every run.

		// initialize
		LOG.info("========================== Initializing scenario ... ");
		Scenario scenario = getScenario();

		// add all possible connectors to it
		LOG.info("========================== Adding all possible connectors to bike track...");

		BikeTrackConnectionIdentifier connectionIdentifier = new BikeTrackConnectionIdentifier(initialNetwork,bikeTrack);
		connectionIdentifier.run();

		List<Node> bikeTrackNodes = connectionIdentifier.getBikeTrackNodes();
		linkIds = connectionIdentifier.getConnectedLinks();

		for(Node n : bikeTrackNodes) {
			if(scenario.getNetwork().getNodes().containsKey(n.getId())) continue;
			NetworkUtils.createAndAddNode(scenario.getNetwork(),n.getId(),n.getCoord());
		}

		for (Id<Link> lId : linkIds.keySet()) {
			Link l = linkIds.get(lId);
			l.setAllowedModes(allowedModes);
			scenario.getNetwork().addLink(l);
		}

		// start trials
		for(int index = 1; index < maxItration/updateConnectorsAfterIteration; index++) {
			removeRoutes(scenario);

			LOG.info("========================== Running trial "+index);
			FilteredLinkVolumeHandler volHandler = new FilteredLinkVolumeHandler(modes);
			volHandler.reset(0);

			String outputDir = scenario.getConfig().controler().getOutputDirectory();
			outputDir = outputDir+"_"+index;
			int firstIt = scenario.getConfig().controler().getFirstIteration();
			int lastIt = firstIt + index * updateConnectorsAfterIteration;

			scenario.getConfig().controler().setLastIteration( lastIt );
			scenario.getConfig().controler().setOutputDirectory(outputDir);

 			final Controler controler = new Controler(scenario);
			addOverrides(controler);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(volHandler);
				}
			});
			controler.run();

			LOG.info("========================== Finished trial "+index);

			LOG.info("========================== Removing the connector links if exists...");
			for  (Id<Link> lId : linkIds.keySet()) {
				scenario.getNetwork().removeLink(lId);
			}

			LOG.info("========================== Updating the bike counts on the connectors link...");

			// update the link counts
			if(index==1) { // nothing to update; just store info
				Map<Id<Link>, Map<Integer, Double>> link2time2vol = volHandler.getLinkId2TimeSlot2LinkCount();
				for(Id<Link> linkId : linkIds.keySet()) {
					double count;
					if ( link2time2vol.containsKey(linkId) ) count = MapUtils.doubleValueSum( link2time2vol.get(linkId) );
					else count = 0.0;
					linkId2Count.put(linkId, count);
				}
			} else {
				Map<Id<Link>, Map<Integer, Double>> link2time2vol = volHandler.getLinkId2TimeSlot2LinkCount();
				for(Id<Link> linkId : linkId2Count.keySet()) {
					double oldCount = linkId2Count.get(linkId);
					double count = link2time2vol.containsKey(linkId) ? MapUtils.doubleValueSum( link2time2vol.get(linkId) ) : 0.0;
					linkId2Count.put(linkId,  count * (1-blendFactor) +  blendFactor * oldCount);
				}
			}

			// sort based on the values (i.e. link volume)
			Comparator<Map.Entry<Id<Link>, Double>> byValue = (entry1, entry2) -> entry1.getValue().compareTo(
					entry2.getValue());

			LOG.info("========================== Adding new connectors links based on the count...");
			// take only pre-decided number of links.
			Iterator<Map.Entry<Id<Link>, Double>> iterator = linkId2Count.entrySet().stream().sorted(byValue.reversed()).limit(numberOfConnectors).iterator();
			while (iterator.hasNext()) {
				Map.Entry<Id<Link>, Double> next = iterator.next();
				Link l = linkIds.get(next.getKey());
				l.setAllowedModes(allowedModes);
				scenario.getNetwork().addLink(l);
				LOG.info("========================== Connector "+ l.getId()+" is added to the network, volume on this link is "+ next.getValue());
			}

			// delete unnecessary iterations folder here.
			for (int inx = firstIt+1; inx <lastIt; inx ++){
				String dirToDel = outputDir+"/ITERS/it."+inx;
				Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
				IOUtils.deleteDirectory(new File(dirToDel),false);
			}

			new File(outputDir+"/analysis/").mkdir();
			String outputEventsFile = outputDir+"/output_events.xml.gz";
			// write some default analysis
			String userGroup = PatnaPersonFilter.PatnaUserGroup.urban.toString();
			ModalTravelTimeAnalyzer mtta = new ModalTravelTimeAnalyzer(outputEventsFile, userGroup, new PatnaPersonFilter());
			mtta.run();
			mtta.writeResults(outputDir+"/analysis/modalTravelTime_"+userGroup+".txt");

			ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new PatnaPersonFilter());
			msc.run();
			msc.writeResults(outputDir+"/analysis/modalShareFromEvents_"+userGroup+".txt");

			StatsWriter.run(outputDir);
		}
	}

	public static Scenario getScenario() {
		Config config = ConfigUtils.createConfig();
		String outputDir ;

		if(isAllwoingMotorbikeOnBikeTrack) outputDir = dir+"/bikeTrackConnectors-mb/";
		else outputDir = dir+"/bikeTrackConnectors/";

		String inputDir = dir+"/input/";
		String configFile = inputDir + "configBaseCaseCtd.xml";

		ConfigUtils.loadConfig(config, configFile);
		config.controler().setOutputDirectory(outputDir);

		config.network().setInputFile(inputDir+"network.xml.gz");
		String inPlans = inputDir+"baseCaseOutput_plans.xml.gz";
		config.plans().setInputFile( inPlans);
		config.plans().setInputPersonAttributeFile(inputDir+"output_personAttributes.xml.gz");
		config.vehicles().setVehiclesFile(inputDir+"output_vehicles.xml.gz");

		//==
		// after calibration;  departure time is fixed for urban; check if time choice is not present
		Collection<StrategySettings> strategySettings = config.strategy().getStrategySettings();
		for(StrategySettings ss : strategySettings){ // departure time is fixed now.
			if ( ss.getStrategyName().equals(DefaultStrategy.TimeAllocationMutator.toString()) ) {
				throw new RuntimeException("Time mutation should not be used; fixed departure time must be used after cadyts calibration.");
			}
		}
		//==
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setWriteEventsInterval(10);
		config.controler().setDumpDataAtEnd(true);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private static void addOverrides(Controler controler) {
		final BikeTimeDistanceTravelDisutilityFactory builder_bike =  new BikeTimeDistanceTravelDisutilityFactory("bike", controler.getScenario().getConfig().planCalcScore());
		final RandomizingTimeDistanceTravelDisutilityFactory builder_truck =  new RandomizingTimeDistanceTravelDisutilityFactory("truck", controler.getScenario().getConfig().planCalcScore());

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

	private static void removeRoutes(Scenario scenario) {
		LOG.info("Routes of all bike plans will be re-moved.");

		// following is required to get the routes (or links) from base network and not from the running scenario network.
		Scenario scNetwork = LoadMyScenarios.loadScenarioFromNetwork(initialNetwork);

		// remove routes from legs of bike mode
		for (Person p : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : p.getPlans()) {
				List<PlanElement> pes = plan.getPlanElements();
				for (PlanElement pe : pes) {
					if(pe instanceof Activity) {
						Activity act = ((Activity)pe);
						Id<Link> linkId = act.getLinkId();
						Coord cord = act.getCoord();

						if(linkId==null) { // activity should have at least one of link id or coord
							if(cord==null) throw new RuntimeException("Activity "+act.toString()+" do not have either of link id or coord. Aborting...");
							else {/*nothing to do; cord is assigned*/ }
						} else if (cord==null) { // if cord is null, get it from
							if(scNetwork.getNetwork().getLinks().containsKey(linkId)) {
								cord = scNetwork.getNetwork().getLinks().get(linkId).getCoord();
								act.setLinkId(null);
								act.setCoord(cord);
							} else throw new RuntimeException("Activity "+act.toString()+" do not have cord and link id is not present in network. Aborting...");
						}
					}
					else if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						leg.setRoute(null);
					}
				}
			}
		}
	}
}