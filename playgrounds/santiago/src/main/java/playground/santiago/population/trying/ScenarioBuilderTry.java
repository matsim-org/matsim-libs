/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.santiago.population.trying;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.ControlerConfigGroup.MobsimType;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.CountsConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.LinkStatsConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.PlansConfigGroup.NetworkRouteType;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehicleBehavior;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.PtConstants;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;

import playground.santiago.SantiagoScenarioConstants;
import playground.santiago.SantiagoScenarioConstants.SubpopulationName;
import playground.santiago.population.CSVToPlans;

/**
 * Creates an initial population and config for the Greater Santiago Area, executing the following steps:
 * <ol>
 * <li>Create population from survey data</li>
 * <li>Filter population such that we only have plans where all activities end before midnight</li>
 * <li>Randomize activity end times</li>
 * <li>Adding activity types according to their reported typical duration to the config</li>
 * </ol>
 * 
 * @author dhosse, kturner, benjamin
 * 
 */

public class ScenarioBuilderTry {
	private static final Logger log = Logger.getLogger(ScenarioBuilderTry.class);
	
//	final String pathForMatsim = "../../../runs-svn/santiago/run20/";
//	final boolean prepareForModeChoice = false;
	final String pathForMatsim = "../../../runs-svn/santiago/run32/";
	final boolean prepareForModeChoice = true;
	
	final int writeStuffInterval = 50;
	final int nrOfThreads = 6;

	final String svnWorkingDir = "../../../shared-svn/studies/countries/cl/";
	final String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/inputFromElsewhere/";
	final String boundariesInputDir = workingDirInputFiles + "exported_boundaries/";
	final String databaseFilesDir = workingDirInputFiles + "exportedFilesFromDatabase/";
	final String outputDir = svnWorkingDir + "Kai_und_Daniel/inputForMATSim/";
	
	final String popA0eAX = "A0equalAX";		//Population with first Activity = last Activity
	final String popA0neAX = "A0NoNequalAX";	//Population with first Activity != last Activity
	
	public static void main(String args[]){
		ScenarioBuilderTry scb = new ScenarioBuilderTry();
		scb.build();
	}
	
	private void build() {
		File output = new File(outputDir);
		if(!output.exists()) createDir(new File(outputDir));
		
		Config config = ConfigUtils.createConfig();
		setConfigParameters(config);
		
		CSVToPlans converter = new CSVToPlans(config,
											  outputDir + "plans/",
											  boundariesInputDir + "Boundaries_20150428_085038.shp");
		converter.run(databaseFilesDir + "Hogar.csv",
					  databaseFilesDir + "Persona.csv",
					  databaseFilesDir + "Export_Viaje.csv",
					  databaseFilesDir + "Etapa.csv",
					  databaseFilesDir + "comunas.csv");
		
		Scenario scenarioFromEOD = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioFromEOD).readFile(outputDir + "plans/plans_eod.xml.gz");
		Population popFromEOD = scenarioFromEOD.getPopulation();
		
		//removing all persons with less than 3 plan elements (e.g. "latent demand")
		removePersonsWithLessThanThreePlanElements(popFromEOD);
		
		//removing all persons with implausible activity times
		removePersonsWithImplausibleActivityTimes(popFromEOD);
		
		//removing all persons that are not in bounding box
		removePersonsWithInvalidCoords(popFromEOD);
		
		//classify population according to their first and last activity types
		Map<String, Population> populationMap = classifyPopulationA0eAXandA0neAX(popFromEOD);
		new PopulationWriter(populationMap.get(popA0eAX)).write(outputDir + "plans/plans_A0eAx_coords.xml.gz");
		new PopulationWriter(populationMap.get(popA0neAX)).write(outputDir + "plans/plans_A0neAx_coords_beforeMidnight.xml.gz");
		log.info("persons with A0 equal to AX: " + populationMap.get(popA0eAX).getPersons().size());
		log.info("persons with A0 non equal to AX (and AX ending before midnight): " + populationMap.get(popA0neAX).getPersons().size());
		
		Scenario scenarioTmp = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioTmp).readFile(outputDir + "plans/plans_A0eAx_coords.xml.gz");
		new PopulationReader(scenarioTmp).readFile(outputDir + "plans/plans_A0neAx_coords_beforeMidnight.xml.gz");
		
		//define meaningful population for scoring and meaningful (= stated) typical durations
		ActivityClassifierTry aap = new ActivityClassifierTry(scenarioTmp);
		aap.run();
		Population outPop = aap.getOutPop();
		//calculating sample size here before adding freight
		final double sampleSize = outPop.getPersons().size();
		log.info("population has " + sampleSize + " persons.");
		
		//randomize end times
		randomizeEndTimes(outPop);

//		//add freight here so the end times will not be randomized
//		addFreightPop(aap.getOutPop());
//		new PopulationWriter(outPop).write(outputDir + "plans/plans_final.xml.gz");
			
		//finish config
		setActivityParams(aap.getActivityType2TypicalDuration(), config);
		final double scaleFactor = SantiagoScenarioConstants.N / sampleSize;
		log.info("setting scale factor to " + SantiagoScenarioConstants.N + " / " + sampleSize + " = " + scaleFactor);
		setCountsParameters(config.counts(), scaleFactor);
		setQSimParameters(config.qsim(), scaleFactor);
		new ConfigWriter(config).write(outputDir + "config_final.xml");
	}

	private void removePersonsWithImplausibleActivityTimes(Population population) {
		Set<Id<Person>> persons2Remove = new HashSet<Id<Person>>();
		int smallerZeroTimeCounter = 0;
		int startAfterEndTimeCounter = 0;
		for(Person person : population.getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			double lastStartTime = Double.NEGATIVE_INFINITY;
			double lastEndTime = Double.NEGATIVE_INFINITY;
			for(int ii = 0; ii < selectedPlan.getPlanElements().size(); ii++) {
				PlanElement pe = selectedPlan.getPlanElements().get(ii);
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					if(ii == 0){ //first activity
						
					} else if (ii  == selectedPlan.getPlanElements().size() - 1){ //last activity

					} else { // all intermediate activities
						if(act.getStartTime() < 0. || act.getEndTime() < 0.){
							persons2Remove.add(person.getId());
							smallerZeroTimeCounter++;
							break;
						}
						if(act.getStartTime() > act.getEndTime()){
							log.info("Start time " + act.getStartTime() + "; End time " + act.getEndTime());
							persons2Remove.add(person.getId());
							startAfterEndTimeCounter++;
							break;
						}
					}
//					if(act.getStartTime() > lastStartTime) lastStartTime = act.getStartTime();
//					if(act.getEndTime() > lastEndTime) lastEndTime = act.getEndTime();
				}
			}
		}
		for(Id<Person> personId : persons2Remove){
			population.getPersons().remove(personId);
		}
		log.info("Removed " + persons2Remove.size() + " persons because they had implausible activity times.");
		log.info("Details: "
//				+ undefinedTimeCounter + " undefinded time problems, " 
				+ smallerZeroTimeCounter + " smaller zero time problems, "
				+ startAfterEndTimeCounter + " start time after end time problems.");
	}

	private Map<String, Population> classifyPopulationA0eAXandA0neAX(Population population) {
		Map<String, Population> populationMap = new HashMap<String, Population>();
		//for persons with a0 = aX
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop1 = sc.getPopulation();
		//for persons with a0 != aX
		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop2 = sc2.getPopulation();

		int counter = 0;
		for(Person person : population.getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			Activity firstAct = (Activity) selectedPlan.getPlanElements().get(0);
			Activity lastAct = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().size()-1);
			
			if(firstAct.getType().equals(lastAct.getType())){
				pop1.addPerson(person);
			} else{
				if(lastAct.getStartTime() > Time.MIDNIGHT || lastAct.getEndTime() > Time.MIDNIGHT){
					counter ++;
				} else {
					pop2.addPerson(person);
				}
			}
		}
		log.info("Removed " + counter + " persons because they the last activity was started or ended after midnight and its type was different from the first activity.");
		populationMap.put(popA0eAX, pop1);
		populationMap.put(popA0neAX, pop2);
		return populationMap;
	}

	private void removePersonsWithLessThanThreePlanElements(Population population) {
		Set<Id<Person>> persons2Remove = new HashSet<Id<Person>>();
		for(Person person : population.getPersons().values()){
			if(person.getSelectedPlan().getPlanElements().size() < 3){
				persons2Remove.add(person.getId());
			}
		}
		for(Id<Person> personId : persons2Remove){
			population.getPersons().remove(personId);
		}
		log.info("Removed " + persons2Remove.size() + " persons because they had less than 3 plan elements.");
	}

	private void removePersonsWithInvalidCoords(Population population) {
		// TODO: move this to SantiagoScenarioConstants?
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", SantiagoScenarioConstants.toCRS);
		final double xMin = -71.3607;
		final double yMin = -33.8875;
		final double xMax = -70.4169;
		final double yMax = -33.0144;
		Coord leftBottom = ct.transform(new Coord(xMin, yMin));
		Coord rightTop = ct.transform(new Coord(xMax, yMax));
		double minX = leftBottom.getX();
		double maxX = rightTop.getX();
		double minY = leftBottom.getY();
		double maxY = rightTop.getY();
		
		Set<Id<Person>> persons2Remove = new HashSet<Id<Person>>();
		for(Person person : population.getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			for(PlanElement pe : selectedPlan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					if(act.getCoord() == null){
						persons2Remove.add(person.getId());
						break;
					}
					if(act.getCoord().getX() == 0 || act.getCoord().getY() == 0){
						persons2Remove.add(person.getId());
						break;
					}
					if(act.getCoord().getX() < minX || act.getCoord().getX() > maxX ||
							act.getCoord().getY() < minY || act.getCoord().getY() > maxY){
						persons2Remove.add(person.getId());
						break;
					}
				}
			}
		}
		for(Id<Person> personId : persons2Remove){
			population.getPersons().remove(personId);
		}
		log.info("Removed " + persons2Remove.size() + " persons because they had at least one activity with invalid coordinates.");
	}

	private void setActivityParams(SortedMap<String, Double> acts, Config config) {
		for(String act :acts.keySet()){
			if(act.equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
				//should not happen, but in case do nothing
			} else {
				ActivityParams params = new ActivityParams();
				params.setActivityType(act);
				params.setTypicalDuration(acts.get(act));
				// Minimum duration is now specified by typical duration.
//				params.setMinimalDuration(acts.get(act).getSecond());
				params.setClosingTime(Time.UNDEFINED_TIME);
				params.setEarliestEndTime(Time.UNDEFINED_TIME);
				params.setLatestStartTime(Time.UNDEFINED_TIME);
				params.setOpeningTime(Time.UNDEFINED_TIME);
				params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
				config.planCalcScore().addActivityParams(params);
			}
		}
	}

	private void randomizeEndTimes(Population population){
		log.info("Randomizing activity end times...");
		Random random = MatsimRandom.getRandom();
		for(Person person : population.getPersons().values()){
			double timeShift = 0.;
			List<PlanElement> pes= person.getSelectedPlan().getPlanElements();
			for(PlanElement pe : pes){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					// TODO: this should not happen any more after using ActivityClassifier?
					if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
						// TODO: should also not happen any more.
						if(act.getEndTime() - act.getStartTime() == 0){
							timeShift += 1800.;
						}
					}
				}
			}
			
			Activity firstAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Activity lastAct = (Activity) person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1);
			String firstActType = firstAct.getType();
			String lastActType = lastAct.getType();
			
			double delta = 0.;
			while(delta == 0.){
				delta = createRandomEndTime(random);
				if(firstAct.getEndTime() + delta + timeShift < 0.){ //avoid shifting A0 end time before midnight
					delta = 0.;
				}
				if(!firstActType.equals(lastActType)){ 							    //if A0neAX, ...
					if(lastAct.getStartTime() + delta + timeShift > Time.MIDNIGHT){ //...avoid shifting start time of AX after midnight
						delta = 0.;
					}
				} else { //if A0eAX, ...
						 //...allow to shift it after midnight 
				}
				// if an activity end time for last activity exists, it should be 24:00:00
				// in order to avoid zero activity durations, this check is done
				// TODO: I dont understand this...
				if(lastAct.getEndTime() != Time.UNDEFINED_TIME){
					if(lastAct.getStartTime() + delta + timeShift >= lastAct.getEndTime()){
						delta = 0.;
					}
				}
			}
			
			for(int i = 0; i < pes.size(); i++){
				PlanElement pe = pes.get(i);
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					if(!act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
						if(pes.indexOf(act) > 0){ //set start times for all but the first activity 
							act.setStartTime(act.getStartTime() + delta);
						}
						if(pes.indexOf(act) < pes.size()-1){ //set end times for all but the last activity
							act.setEndTime(act.getEndTime() + delta);
						}
					} else {
						// do nothing
//						throw new RuntimeException("Activity from type " + PtConstants.TRANSIT_ACTIVITY_TYPE + " found in initial plans. Aborting...");
					}
				}
			}
		}
		log.info("...Done.");
	}
	
	private double createRandomEndTime(Random random){
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = 20 * 60 * normal;
		
		return endTime;
	}
	
	private void setConfigParameters(Config config){
		setControlerParameters(config.controler());
//		setCountsParameters(config.counts());
		setGlobalParameters(config.global());
		setLinkStatsParameters(config.linkStats());
		setNetworkParameters(config.network());
		setParallelEventHandlingParameters(config.parallelEventHandling());
		setPlanCalcScoreParameters(config.planCalcScore());
		setPlanParameters(config.plans());
		setPlansCalcRouteParameters(config.plansCalcRoute());
//		setQSimParameters(config.qsim());
		setStrategyParameters(config.strategy());
		if(prepareForModeChoice){
			setTransitParameters(config.transit(), config.transitRouter());
		}
		
		//creation of more than one subpopulation in config not possible yet ->removed Module, creation in SantiagoScenarioRunner
//		setSubtourModeChoiceParameters(config.subtourModeChoice());
		config.removeModule("subtourModeChoice");
		
//		setTimeAllocationMutatorParameters(config.timeAllocationMutator());
		setTravelTimeCalculatorParameters(config.travelTimeCalculator());
		setVspExperimentalParameters(config.vspExperimental());
	}
	
	private void setTransitParameters(TransitConfigGroup transit, TransitRouterConfigGroup transitRouter) {
		Set<String> transitModes = new HashSet<String>();
		transitModes.add(TransportMode.pt);
		transitModes.add(SantiagoScenarioConstants.Modes.bus.toString());
		transitModes.add(SantiagoScenarioConstants.Modes.metro.toString());
//		transitModes.add(SantiagoScenarioConstants.Modes.train.toString());
		transit.setTransitModes(transitModes);
//		transit.setTransitScheduleFile(pathForMatsim + "input/transitschedule.xml");
		transit.setTransitScheduleFile(pathForMatsim + "input/transitschedule_simplified.xml");
		transit.setVehiclesFile(pathForMatsim + "input/transitvehicles.xml");
		transit.setUseTransit(true);
//		transitRouter.setMaxBeelineWalkConnectionDistance(500.);
//		transitRouter.setSearchRadius(200.);
		transitRouter.setExtensionRadius(500.);
	}

	private void setControlerParameters(ControlerConfigGroup cc){
		cc.setLinkToLinkRoutingEnabled(false);
		HashSet<EventsFileFormat> eventsFileFormats = new HashSet<EventsFileFormat>();
		eventsFileFormats.add(EventsFileFormat.xml);
		cc.setEventsFileFormats(eventsFileFormats);
		cc.setFirstIteration(0);
		cc.setLastIteration(100);
		cc.setMobsim(MobsimType.qsim.name());
		cc.setOutputDirectory(pathForMatsim + "output/");
		cc.setRoutingAlgorithmType(RoutingAlgorithmType.Dijkstra);
		cc.setRunId(null);	//should not be "", because then all file names start with a dot. --> null or any number. (KT, 2015-08-17) 
		cc.setWriteEventsInterval(writeStuffInterval);
		cc.setWritePlansInterval(writeStuffInterval);
		cc.setSnapshotFormat(CollectionUtils.stringToSet("otfvis"));
		cc.setWriteSnapshotsInterval(0);
	}
	
	private void setCountsParameters(CountsConfigGroup counts, double scaleFactor){
		// TODO: check what adding taxi, colectivo, and freight changes
		counts.setAnalyzedModes(TransportMode.car);
		counts.setAverageCountsOverIterations(5);
		counts.setCountsScaleFactor(scaleFactor);
		counts.setDistanceFilter(null);
		counts.setDistanceFilterCenterNode(null);
		counts.setFilterModes(false);
		counts.setInputFile(pathForMatsim + "input/counts_merged_VEH_C01.xml");
		counts.setOutputFormat("all");
		counts.setWriteCountsInterval(writeStuffInterval);
	}
	
	private void setGlobalParameters(GlobalConfigGroup global){
		global.setCoordinateSystem(SantiagoScenarioConstants.toCRS);
		global.setNumberOfThreads(nrOfThreads);
		global.setRandomSeed(4711);
	}
	
	private void setLinkStatsParameters(LinkStatsConfigGroup ls){
		ls.setAverageLinkStatsOverIterations(5);
		ls.setWriteLinkStatsInterval(writeStuffInterval);
	}
	
	private void setNetworkParameters(NetworkConfigGroup net){
		net.setChangeEventsInputFile(null);
		net.setInputFile(pathForMatsim + "input/network_merged_cl.xml.gz");
		net.setLaneDefinitionsFile(null);
		net.setTimeVariantNetwork(false);
	}
	
	private void setParallelEventHandlingParameters(ParallelEventHandlingConfigGroup peh){
		peh.setEstimatedNumberOfEvents(null);
		peh.setNumberOfThreads(nrOfThreads);
	}
	
	private void setPlanCalcScoreParameters(PlanCalcScoreConfigGroup pcs){
		pcs.setBrainExpBeta(1.0);
//		pcs.setPathSizeLogitBeta(0.0);
//		pcs.setEarlyDeparture_utils_hr(-0.0);
		pcs.setFractionOfIterationsToStartScoreMSA(0.8);
//		pcs.setLateArrival_utils_hr(-18.0);
		pcs.setLearningRate(1.0);
		pcs.setMarginalUtilityOfMoney(0.0023);
		pcs.setPerforming_utils_hr(4.014);
		pcs.setUsingOldScoringBelowZeroUtilityDuration(false);
//		pcs.setUtilityOfLineSwitch(-1.0);
//		pcs.setMarginalUtlOfWaiting_utils_hr(-0.0);
		pcs.setWriteExperiencedPlans(false);
		
		ModeParams carParams = new ModeParams(TransportMode.car);
		carParams.setConstant(0.0);
//		carParams.setMarginalUtilityOfDistance(0.0);
		carParams.setMarginalUtilityOfTraveling(-1.056);
		carParams.setMonetaryDistanceRate(-0.248);
		pcs.addModeParams(carParams);
		
		ModeParams rideParams = new ModeParams(TransportMode.ride);
		rideParams.setConstant(0.0);
//		rideParams.setMarginalUtilityOfDistance(0.0);
		rideParams.setMarginalUtilityOfTraveling(-1.056);
		rideParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(rideParams);
		
		ModeParams taxiParams = new ModeParams(SantiagoScenarioConstants.Modes.taxi.toString());
		taxiParams.setConstant(0.0);
//		taxiParams.setMarginalUtilityOfDistance(0.0);
		taxiParams.setMarginalUtilityOfTraveling(-1.056);
		taxiParams.setMonetaryDistanceRate(-1.0); //see http://www.numbeo.com/taxi-fare/city_result.jsp?country=Chile&city=Santiago
		pcs.addModeParams(taxiParams);
		
		ModeParams colectivoParams = new ModeParams(SantiagoScenarioConstants.Modes.colectivo.toString());
		colectivoParams.setConstant(0.0);
//		colectivoParams.setMarginalUtilityOfDistance(0.0);
		colectivoParams.setMarginalUtilityOfTraveling(-1.056);
		colectivoParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(colectivoParams);
		
		ModeParams trainParams = new ModeParams(SantiagoScenarioConstants.Modes.train.toString());
		trainParams.setConstant(0.0);
//		trainParams.setMarginalUtilityOfDistance(0.0);
		trainParams.setMarginalUtilityOfTraveling(-1.056);
		trainParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(trainParams);
		
		/*
		 * begin pt parameter settings
		 * */
		if(!prepareForModeChoice){
			ModeParams busParams = new ModeParams(SantiagoScenarioConstants.Modes.bus.toString());
			busParams.setConstant(0.0);
//			busParams.setMarginalUtilityOfDistance(0.0);
			busParams.setMarginalUtilityOfTraveling(-1.056);
			busParams.setMonetaryDistanceRate(-0.0);
			pcs.addModeParams(busParams);
			
			ModeParams metroParams = new ModeParams(SantiagoScenarioConstants.Modes.metro.toString());
			metroParams.setConstant(0.0);
//			metroParams.setMarginalUtilityOfDistance(0.0);
			metroParams.setMarginalUtilityOfTraveling(-1.056);
			metroParams.setMonetaryDistanceRate(-0.0);
			pcs.addModeParams(metroParams);
			
		} else {
			ModeParams ptParams = new ModeParams(TransportMode.pt);
			ptParams.setConstant(-1.0);
//			ptParams.setMarginalUtilityOfDistance(0.0);
			ptParams.setMarginalUtilityOfTraveling(-1.056);
			ptParams.setMonetaryDistanceRate(-0.0);
			pcs.addModeParams(ptParams);
		}
		/*
		 * end pt parameter settings
		 * */
		
		ModeParams walkParams = new ModeParams(TransportMode.walk);
		walkParams.setConstant(0.0);
//		walkParams.setMarginalUtilityOfDistance(0.0);
		walkParams.setMarginalUtilityOfTraveling(-1.056);
		walkParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(walkParams);
		
		ModeParams bikeParams = new ModeParams(TransportMode.bike);
		bikeParams.setConstant(0.0);
//		bikeParams.setMarginalUtilityOfDistance(0.0);
		bikeParams.setMarginalUtilityOfTraveling(-1.056);
		bikeParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(bikeParams);
		
		ModeParams otherModeParams = new ModeParams(TransportMode.other);
		otherModeParams.setConstant(0.0);
//		otherModeParams.setMarginalUtilityOfDistance(0.0);
		otherModeParams.setMarginalUtilityOfTraveling(-1.056);
		otherModeParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(otherModeParams);
	}
	
	private void setPlanParameters(PlansConfigGroup plans){
		plans.setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration);
		if(prepareForModeChoice) plans.setInputPersonAttributeFile(pathForMatsim + "input/" +"agentAttributes.xml");
		plans.setInputFile(pathForMatsim + "input/" + "plans_final" + ".xml.gz");
		plans.setNetworkRouteType(NetworkRouteType.LinkNetworkRoute);
		plans.setSubpopulationAttributeName(SubpopulationName.carUsers); 
		plans.setRemovingUnneccessaryPlanAttributes(true);
	}
	
	private void setPlansCalcRouteParameters(PlansCalcRouteConfigGroup pcr){
		Set<String> networkModes = new HashSet<String>();
		networkModes.add(TransportMode.car);
		networkModes.add(TransportMode.ride);
		networkModes.add(SantiagoScenarioConstants.Modes.taxi.toString());
		networkModes.add(SantiagoScenarioConstants.Modes.colectivo.toString());
		networkModes.add(SantiagoScenarioConstants.Modes.other.toString());
//		if(prepareForModeChoice) networkModes.add(TransportMode.pt);
		pcr.setNetworkModes(networkModes);
		
	/*
	 * begin pt parameter settings
	 * */
		if(!prepareForModeChoice){
			ModeRoutingParams busParams = new ModeRoutingParams(SantiagoScenarioConstants.Modes.bus.toString());
			busParams.setBeelineDistanceFactor(1.3);
			busParams.setTeleportedModeSpeed(25 / 3.6);
			pcr.addModeRoutingParams(busParams);

			ModeRoutingParams metroParams = new ModeRoutingParams(SantiagoScenarioConstants.Modes.metro.toString());
			metroParams.setBeelineDistanceFactor(1.3);
			metroParams.setTeleportedModeSpeed(32 / 3.6);
			pcr.addModeRoutingParams(metroParams);
		} else {
			//TODO: This is by default set to walk parameters; changing this might require defining my own TripRouter...
//			ModeRoutingParams transitWalkParams = new ModeRoutingParams(TransportMode.transit_walk);
//			transitWalkParams.setBeelineDistanceFactor(1.3);
//			transitWalkParams.setTeleportedModeSpeed(3 / 3.6);
//			pcr.addModeRoutingParams(transitWalkParams);
		}
	/*
	 * end pt parameter settings
	 * */
		
		ModeRoutingParams trainParams = new ModeRoutingParams(SantiagoScenarioConstants.Modes.train.toString());
		trainParams.setBeelineDistanceFactor(1.3);
		trainParams.setTeleportedModeSpeed(50 / 3.6);
		pcr.addModeRoutingParams(trainParams);
			
		ModeRoutingParams walkParams = new ModeRoutingParams(TransportMode.walk);
		walkParams.setBeelineDistanceFactor(1.3);
		walkParams.setTeleportedModeSpeed(4 / 3.6);
		pcr.addModeRoutingParams(walkParams);
		
		ModeRoutingParams bikeParams = new ModeRoutingParams(TransportMode.bike);
		bikeParams.setBeelineDistanceFactor(1.3);
		bikeParams.setTeleportedModeSpeed(10 / 3.6);
		pcr.addModeRoutingParams(bikeParams);
	}
	
	private void setQSimParameters(QSimConfigGroup qsim, double scaleFactor){
		qsim.setStartTime(0 * 3600);
		qsim.setEndTime(30 * 3600);
		double flowCapFactor = (1 / scaleFactor);
		qsim.setFlowCapFactor(flowCapFactor);
//		qsim.setStorageCapFactor(0.015);
		qsim.setStorageCapFactor(flowCapFactor * 3.);
		qsim.setInsertingWaitingVehiclesBeforeDrivingVehicles(false);
		qsim.setLinkDynamics(LinkDynamics.FIFO);
//		qsim.setLinkWidth(30);
		Set<String> mainModes = new HashSet<String>();
		mainModes.add(TransportMode.car);
		// not necessary since pt is not a congested mode
//		if(prepareForModeChoice) mainModes.add(TransportMode.pt);
		qsim.setMainModes(mainModes);
		qsim.setNodeOffset(0.0);
		qsim.setNumberOfThreads(nrOfThreads);
		qsim.setSimStarttimeInterpretation(StarttimeInterpretation.maxOfStarttimeAndEarliestActivityEnd);
		qsim.setSnapshotStyle(SnapshotStyle.equiDist);
		qsim.setSnapshotPeriod(0.0);
//		qsim.setStuckTime(10.0);
//		qsim.setTimeStepSize(1.0);
		qsim.setTrafficDynamics(TrafficDynamics.queue);
		qsim.setUsePersonIdForMissingVehicleId(true);
		qsim.setUsingFastCapacityUpdate(false);
		qsim.setUsingThreadpool(false);
		qsim.setVehicleBehavior(VehicleBehavior.teleport);
	}
	
	private void setStrategyParameters(StrategyConfigGroup strategy){
		strategy.setFractionOfIterationsToDisableInnovation(0.8);
		strategy.setMaxAgentPlanMemorySize(5);
		strategy.setPlanSelectorForRemoval("WorstPlanSelector");
	}
	
	private void setTravelTimeCalculatorParameters(TravelTimeCalculatorConfigGroup ttc){
		ttc.setAnalyzedModes(TransportMode.car);
		ttc.setCalculateLinkToLinkTravelTimes(false);
		ttc.setCalculateLinkTravelTimes(true);
		ttc.setFilterModes(false);
		ttc.setTravelTimeAggregatorType("optimistic");
		ttc.setTraveltimeBinSize(900);
		ttc.setTravelTimeCalculatorType(TravelTimeCalculatorType.TravelTimeCalculatorArray.name());
		ttc.setTravelTimeGetterType("average");
	}
	
	private void setVspExperimentalParameters(VspExperimentalConfigGroup vsp){
		vsp.setLogitScaleParamForPlansRemoval(1.0);
		vsp.setVspDefaultsCheckingLevel(VspDefaultsCheckingLevel.warn);
		vsp.setWritingOutputEvents(true);
	}
	
	private void createDir(File file) {
		log.info("Directory " + file + " created: "+ file.mkdirs());	
	}

	//	/**
	//	 * 
	//	 * Separates the initial population in two separate populations:
	//	 * <ul>
	//	 * <li>first act = last act</li>
	//	 * <li>first act != last act</li>
	//	 * </ul>
	//	 * 
	//	 * All plans of the returned populations have
	//	 * <ol>
	//	 * <li>at least three plan elements</li>
	//	 * <li>no coords equal to (0,0) or null</li>
	//	 * <li>activity start and end times only BEFORE midnight</li>
	//	 * </ol>
	//	 * 
	//	 * @param population
	//	 * @return
	//	 */
	//	private Map<String, Population> getPlansBeforeMidnight(Population population){
	//		//for persons with a0 = aX
	//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	//		Population pop1 = sc.getPopulation();
	//		//for persons with a0 != aX
	//		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	//		Population pop2 = sc2.getPopulation();
	//		
	//		for(Person person : population.getPersons().values()){
	//			boolean addPerson = true;
	//			double lastStartTime = Double.NEGATIVE_INFINITY;
	//			double lastEndTime = Double.NEGATIVE_INFINITY;
	//			
	//			Plan selectedPlan = person.getSelectedPlan();
	//			
	//			for(PlanElement pe : selectedPlan.getPlanElements()){
	//				if(pe instanceof Activity){
	//					Activity act = (Activity)pe;
	//					if(act.getCoord() == null){
	//						addPerson = false;
	//						break;
	//					}
	//					if(act.getCoord().getX() == 0 || act.getCoord().getY() == 0){
	//						addPerson = false;
	//						break;
	//					}
	//					
	//					if(act.getStartTime() != Time.UNDEFINED_TIME){
	//						if(act.getStartTime() > lastStartTime && act.getStartTime() <= Time.MIDNIGHT){
	//							lastStartTime = act.getStartTime();
	//						} else{
	//							addPerson = false;
	//							break;
	//						}
	//					}
	//					if(act.getEndTime() != Time.UNDEFINED_TIME){
	//						if(act.getEndTime() > lastEndTime && act.getEndTime() <= 24*3600){
	//							lastEndTime = act.getEndTime();
	//						} else{
	//							addPerson = false;
	//							break;
	//						}
	//					}
	//					if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
	//						if(act.getStartTime() > act.getEndTime()){
	//							addPerson = false;
	//							break;
	//						}
	//					}
	//				}
	//			}
	//			
	//			if(!addPerson) continue;
	//			
	//			Activity firstAct = (Activity) selectedPlan.getPlanElements().get(0);
	//			Activity lastAct = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().size()-1);
	//			
	//			if(firstAct.getType().equals(lastAct.getType())){
	//				pop1.addPerson(person);
	//			} else{
	//				pop2.addPerson(person);
	//			}
	//		}
	//		
	//		Map<String, Population> populationMap = new HashMap<String, Population>();
	//		populationMap.put(popA0eAX, pop1);
	//		populationMap.put(popA0neAX, pop2);
	//		
	//		return populationMap;
	//	}
		
		private static Population getRest(Population allPersons, Population population1, Population population2){
			CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", "EPSG:32719");
			final double x1 = -71.3607;
			final double y1 = -33.8875;
			Coord leftBottom = ct.transform(new Coord(x1, y1));
			final double x = -70.4169;
			final double y = -33.0144;
			Coord rightTop = ct.transform(new Coord(x, y));
			double minX = leftBottom.getX();
			double maxX = rightTop.getX();
			double minY = leftBottom.getY();
			double maxY = rightTop.getY();
			
			Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
			
			for(Person person : allPersons.getPersons().values()){
				if(!population1.getPersons().values().contains(person) && !population2.getPersons().values().contains(person)){
					boolean add = true;
					
					if(person.getSelectedPlan().getPlanElements().size() < 3) continue;
					for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
						if(pe instanceof Activity){
							Activity act = (Activity) pe;
							if(act.getCoord() == null){
								add = false;
								break;
							}
							if(act.getCoord().getX() == 0 || act.getCoord().getY() == 0){
								add = false;
								break;
							}
							if(act.getCoord().getX() < minX || act.getCoord().getX() > maxX ||
									act.getCoord().getY() < minY || act.getCoord().getY() > maxY){
								add = false;
								break;
							}
						}
					}
					if(add) population.addPerson(person);
				}
			}
			return population;
		}

		//	/**
		//	 * 
		//	 * Separates the initial population in two separate populations:
		//	 * <ul>
		//	 * <li>first act = last act</li>
		//	 * <li>first act != last act</li>
		//	 * </ul>
		//	 * 
		//	 * All plans of the returned populations have
		//	 * <ol>
		//	 * <li>at least three plan elements</li>
		//	 * <li>no coords equal to (0,0) or null</li>
		//	 * <li>activity start and end times only BEFORE midnight</li>
		//	 * </ol>
		//	 * 
		//	 * @param population
		//	 * @return
		//	 */
		//	private Map<String, Population> getPlansBeforeMidnight(Population population){
		//		//for persons with a0 = aX
		//		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//		Population pop1 = sc.getPopulation();
		//		//for persons with a0 != aX
		//		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		//		Population pop2 = sc2.getPopulation();
		//		
		//		for(Person person : population.getPersons().values()){
		//			boolean addPerson = true;
		//			double lastStartTime = Double.NEGATIVE_INFINITY;
		//			double lastEndTime = Double.NEGATIVE_INFINITY;
		//			
		//			Plan selectedPlan = person.getSelectedPlan();
		//			
		//			for(PlanElement pe : selectedPlan.getPlanElements()){
		//				if(pe instanceof Activity){
		//					Activity act = (Activity)pe;
		//					if(act.getCoord() == null){
		//						addPerson = false;
		//						break;
		//					}
		//					if(act.getCoord().getX() == 0 || act.getCoord().getY() == 0){
		//						addPerson = false;
		//						break;
		//					}
		//					
		//					if(act.getStartTime() != Time.UNDEFINED_TIME){
		//						if(act.getStartTime() > lastStartTime && act.getStartTime() <= Time.MIDNIGHT){
		//							lastStartTime = act.getStartTime();
		//						} else{
		//							addPerson = false;
		//							break;
		//						}
		//					}
		//					if(act.getEndTime() != Time.UNDEFINED_TIME){
		//						if(act.getEndTime() > lastEndTime && act.getEndTime() <= 24*3600){
		//							lastEndTime = act.getEndTime();
		//						} else{
		//							addPerson = false;
		//							break;
		//						}
		//					}
		//					if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
		//						if(act.getStartTime() > act.getEndTime()){
		//							addPerson = false;
		//							break;
		//						}
		//					}
		//				}
		//			}
		//			
		//			if(!addPerson) continue;
		//			
		//			Activity firstAct = (Activity) selectedPlan.getPlanElements().get(0);
		//			Activity lastAct = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().size()-1);
		//			
		//			if(firstAct.getType().equals(lastAct.getType())){
		//				pop1.addPerson(person);
		//			} else{
		//				pop2.addPerson(person);
		//			}
		//		}
		//		
		//		Map<String, Population> populationMap = new HashMap<String, Population>();
		//		populationMap.put(popA0eAX, pop1);
		//		populationMap.put(popA0neAX, pop2);
		//		
		//		return populationMap;
		//	}
			
			private Population cutPlansTo24H(Population population){
				Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				Population populationOut = scenario.getPopulation();
				PopulationFactory popFactory = (PopulationFactory)populationOut.getFactory();
				
				for(Person person : population.getPersons().values()){
					
					int validUntilIndex = 0;
					
					List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
					
					double lastStartTime = Double.NEGATIVE_INFINITY;
					double lastEndTime = Double.NEGATIVE_INFINITY;
					
					for(PlanElement pe : planElements){
						if(pe instanceof Activity){
							Activity act = (Activity)pe;
							if(act.getStartTime() != Time.UNDEFINED_TIME){
								if(act.getStartTime() >= lastStartTime && act.getStartTime() <= 24*3600){
									lastStartTime = act.getStartTime();
								} else {
									break;
								}
							}
							if(act.getEndTime() != Time.UNDEFINED_TIME){
								if(act.getEndTime() >= lastEndTime && act.getEndTime() <= 24*3600){
									lastEndTime = act.getEndTime();
								} else{
									break;
								}
							}
							
							if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
								if(act.getStartTime() > act.getEndTime()){
									break;
								}
							}
						}
						validUntilIndex++;
					}
					
					Person personOut = popFactory.createPerson(person.getId());
					Plan planOut = popFactory.createPlan();
					
					for(int i = 0; i < validUntilIndex; i++){
						
						PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
						if(pe instanceof Activity){
							planOut.addActivity((Activity)pe);
						} else{
							planOut.addLeg((Leg)pe);
						}
					}
					
					personOut.addPlan(planOut);
					personOut.setSelectedPlan(planOut);
					
					boolean valid = false;
					
					while(!valid && planOut.getPlanElements().size() > 0){
						
						PlanElement pe = planOut.getPlanElements().get(planOut.getPlanElements().size()-1);
						if(pe instanceof Leg){
							planOut.getPlanElements().remove(planOut.getPlanElements().size()-1);
						} else if(pe instanceof Activity){
							Activity act = (Activity)pe;
							if(!act.getType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)){
								valid = true;
							} else{
								planOut.getPlanElements().remove(planOut.getPlanElements().size()-1);
							}
						}
					}
					
					if(planOut.getPlanElements().size() > 2){
						
						Activity firstAct = (Activity)personOut.getSelectedPlan().getPlanElements().get(0);
						Activity lastAct = (Activity)personOut.getSelectedPlan().getPlanElements().get(personOut.getSelectedPlan().getPlanElements().size()-1);
						
						if(!firstAct.getType().equals(lastAct.getType())){
							firstAct.setStartTime(0.);
							lastAct.setEndTime(24 * 3600);
						}
						populationOut.addPerson(personOut);
					}
				}
				return populationOut;
			}

			/**
			 * Freight traffic will only be added to population if file exists. Otherwise do nothing.
			 * 
			 * @param populationOut
			 */
			private void addFreightPop(Population populationOut) {
				File freightPlansFile = new File(outputDir + "plans/freight_plans.xml.gz");
				if (!freightPlansFile.exists()){
					log.warn("Freight population file not found under " + freightPlansFile + "; no freight population added.");
				} else {
					log.info("Adding freight population to O-D based population");
					Scenario scenarioFreight = ScenarioUtils.createScenario(ConfigUtils.createConfig());	
					new PopulationReader(scenarioFreight).readFile(freightPlansFile.toString());
					for (Person person : scenarioFreight.getPopulation().getPersons().values()){
						populationOut.addPerson(person);
					}
				}
			}

			private void removePersons(Population population) {
				population.getPersons().remove(Id.createPersonId("23187103"));
				population.getPersons().remove(Id.createPersonId("13768103"));
				population.getPersons().remove(Id.createPersonId("11865102"));
				population.getPersons().remove(Id.createPersonId("11955002"));
				population.getPersons().remove(Id.createPersonId("21043102"));
				population.getPersons().remove(Id.createPersonId("27752101"));
				population.getPersons().remove(Id.createPersonId("21043101"));
				population.getPersons().remove(Id.createPersonId("14380104"));
				population.getPersons().remove(Id.createPersonId("25611002"));
				population.getPersons().remove(Id.createPersonId("13491103"));
				population.getPersons().remove(Id.createPersonId("22343101"));	
			}
			
//			/**
//			 * 
//			 * Separates the initial population in two separate populations:
//			 * <ul>
//			 * <li>first act = last act</li>
//			 * <li>first act != last act</li>
//			 * </ul>
//			 * 
//			 * All plans of the returned populations have
//			 * <ol>
//			 * <li>at least three plan elements</li>
//			 * <li>no coords equal to (0,0) or null</li>
//			 * <li>activity start and end times only BEFORE midnight</li>
//			 * </ol>
//			 * 
//			 * @param population
//			 * @return
//			 */
//			private Map<String, Population> getPlansBeforeMidnight(Population population){
//				//for persons with a0 = aX
//				Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//				Population pop1 = sc.getPopulation();
//				//for persons with a0 != aX
//				Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//				Population pop2 = sc2.getPopulation();
//				
//				for(Person person : population.getPersons().values()){
//					boolean addPerson = true;
//					double lastStartTime = Double.NEGATIVE_INFINITY;
//					double lastEndTime = Double.NEGATIVE_INFINITY;
//					
//					Plan selectedPlan = person.getSelectedPlan();
//					
//					for(PlanElement pe : selectedPlan.getPlanElements()){
//						if(pe instanceof Activity){
//							Activity act = (Activity)pe;
//							if(act.getCoord() == null){
//								addPerson = false;
//								break;
//							}
//							if(act.getCoord().getX() == 0 || act.getCoord().getY() == 0){
//								addPerson = false;
//								break;
//							}
//							
//							if(act.getStartTime() != Time.UNDEFINED_TIME){
//								if(act.getStartTime() > lastStartTime && act.getStartTime() <= Time.MIDNIGHT){
//									lastStartTime = act.getStartTime();
//								} else{
//									addPerson = false;
//									break;
//								}
//							}
//							if(act.getEndTime() != Time.UNDEFINED_TIME){
//								if(act.getEndTime() > lastEndTime && act.getEndTime() <= 24*3600){
//									lastEndTime = act.getEndTime();
//								} else{
//									addPerson = false;
//									break;
//								}
//							}
//							if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
//								if(act.getStartTime() > act.getEndTime()){
//									addPerson = false;
//									break;
//								}
//							}
//						}
//					}
//					
//					if(!addPerson) continue;
//					
//					Activity firstAct = (Activity) selectedPlan.getPlanElements().get(0);
//					Activity lastAct = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().size()-1);
//					
//					if(firstAct.getType().equals(lastAct.getType())){
//						pop1.addPerson(person);
//					} else{
//						pop2.addPerson(person);
//					}
//				}
//				
//				Map<String, Population> populationMap = new HashMap<String, Population>();
//				populationMap.put(popA0eAX, pop1);
//				populationMap.put(popA0neAX, pop2);
//				
//				return populationMap;
//			}
}