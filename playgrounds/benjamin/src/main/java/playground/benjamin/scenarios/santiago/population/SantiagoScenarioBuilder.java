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

package playground.benjamin.scenarios.santiago.population;

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
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup.TravelTimeCalculatorType;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.config.TransitRouterConfigGroup;

import playground.benjamin.scenarios.santiago.SantiagoScenarioConstants;
import playground.benjamin.scenarios.santiago.SantiagoScenarioConstants.SubpopulationName;

public class SantiagoScenarioBuilder {
	private static final Logger log = Logger.getLogger(SantiagoScenarioBuilder.class);

	final String svnWorkingDir = "../../../shared-svn/studies/countries/cl/";
	final String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/inputFromElsewhere/";
	final String boundariesInputDir = workingDirInputFiles + "exported_boundaries/";
	final String databaseFilesDir = workingDirInputFiles + "exportedFilesFromDatabase/";
	final String visualizationsDir = workingDirInputFiles + "Visualisierungen/";
	final String outputDir = svnWorkingDir + "Kai_und_Daniel/inputForMATSim/";
	
	final String transitFilesDir = svnWorkingDir + "santiago_pt_demand_matrix/pt_stops_schedule_2013/";
	final String gtfsFilesDir = svnWorkingDir + "santiago_pt_demand_matrix/gtfs_201306/";
	
	final String popA0eAX = "A0equalAX";		//Population with first Activity = last Activity
	final String popA0neAX = "A0NoNequalAX";	//Population with first Activity != last Activity
	
//	final String pathForMatsim = "../../../runs-svn/santiago/run20/";
//	final boolean prepareForModeChoice = false;
	final String pathForMatsim = "../../../runs-svn/santiago/run30/";
	final boolean prepareForModeChoice = true;
	
	/**
	 * Creates an initial population for the santiago scenario, executing the following steps:
	 * <ol>
	 * <li>Create population from survey data</li>
	 * <li>Filter population such that we only have plans in which all activities end before midnight</li>
	 * <li>Randomize activity end times</li>
	 * <li>Classify activities according to their typical and minimum duration</li>
	 * </ol>
	 * 
	 * Along with the generation of plans, an input config file is created and written.
	 * Todo-marks show positions for essential config settings, e.g. paths and names of in-/output.  
	 * 
	 * @param args
	 */
	public static void main(String args[]){
		SantiagoScenarioBuilder scb = new SantiagoScenarioBuilder();
		scb.build();
	}
	
	private void build() {
		File output = new File(outputDir);
		if(!output.exists()) createDir(new File(outputDir));
		
		Config config = ConfigUtils.createConfig();
		setUpConfigParameters(config);
		
		CSVToPlans converter = new CSVToPlans(config,
											  outputDir + "plans/",
											  boundariesInputDir + "Boundaries_20150428_085038.shp");
		converter.run(databaseFilesDir + "Hogar.csv",
					  databaseFilesDir + "Persona.csv",
					  databaseFilesDir + "Export_Viaje.csv",
					  databaseFilesDir + "Etapa.csv",
					  databaseFilesDir + "comunas.csv");
		
		Scenario scenarioFromEOD = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenarioFromEOD).readFile(outputDir + "plans/plans_eod.xml.gz");
		
		//TODO: check if really needed with "relative score computation" and cutting only A0neAX to midnight (see below)
		removePersons(scenarioFromEOD.getPopulation());
		
		//TODO: rather first sort A0eAX and A0neAX, and then cut to midnight only for the latter...use Amits code below instead?
		Map<String, Population> populationMap = getPlansBeforeMidnight(scenarioFromEOD.getPopulation());
		new PopulationWriter(populationMap.get(popA0eAX)).write(outputDir + "plans/plans_cropped_A0eAx_coords_beforeMidnight.xml.gz");
		log.info("persons with a0 equal to aX: " + populationMap.get(popA0eAX).getPersons().size());
		new PopulationWriter(populationMap.get(popA0neAX)).write(outputDir + "plans/plans_cropped_A0neAx_coords_beforeMidnight.xml.gz");
		log.info("persons with a0 non equal to aX: " + populationMap.get(popA0neAX).getPersons().size());
		double sampleSizeEOD = populationMap.get(popA0eAX).getPersons().size() + populationMap.get(popA0neAX).getPersons().size(); 
		
		Scenario scenarioTmp = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population populationTmp = scenarioTmp.getPopulation();
		new MatsimPopulationReader(scenarioTmp).readFile(outputDir + "plans/plans_cropped_A0eAx_coords_beforeMidnight.xml.gz");
		new MatsimPopulationReader(scenarioTmp).readFile(outputDir + "plans/plans_cropped_A0neAx_coords_beforeMidnight.xml.gz");
		
		randomizeEndTimes(populationTmp);
		
		//finish population
		ActivityClassifier aap = new ActivityClassifier(scenarioTmp);
		aap.run();

		//add here so the end times will not be randomized
		addFreightPop(aap.getOutPop());
		
		new PopulationWriter(aap.getOutPop()).write(outputDir + "plans/plans_final.xml.gz");
				
		//finish config
		SortedMap<String, Double> acts = aap.getActivityType2TypicalDuration();
		setActivityParams(acts, config);

		setCountsParameters(config.counts(), sampleSizeEOD);
		setQSimParameters(config.qsim(), sampleSizeEOD);
		new ConfigWriter(config).write(outputDir + "config_final.xml");
	}

	private void setActivityParams(SortedMap<String, Double> acts, Config config) {
		for(String act :acts.keySet()){
			ActivityParams params = new ActivityParams();
			params.setActivityType(act);
			params.setTypicalDuration(acts.get(act));
			// Minimum duration is now specified by typical duration.
//			params.setMinimalDuration(acts.get(act).getSecond());
			params.setClosingTime(Time.UNDEFINED_TIME);
			params.setEarliestEndTime(Time.UNDEFINED_TIME);
			params.setLatestStartTime(Time.UNDEFINED_TIME);
			params.setOpeningTime(Time.UNDEFINED_TIME);
			params.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
			// TODO: overwriting this is not recommended but enforced for closing time
			if (act.equals("pt interaction")){
				params.setScoringThisActivityAtAll(false);
				params.setClosingTime(0);
			}
			config.planCalcScore().addActivityParams(params);
		}
	}

	/**
	 * Freight traffic will only be added to population if file exists. Otherwise do nothing.
	 * @param populationOut
	 */
	private void addFreightPop(Population populationOut) {
		File freightPlansFile = new File(outputDir + "plans/freight_plans.xml.gz");
		if (!freightPlansFile.exists()){
			log.warn("Freight population file not found under " + freightPlansFile + "; no freight population added.");
		} else {
			log.info("Adding freight population to O-D based population");
			Scenario scenarioFreight = ScenarioUtils.createScenario(ConfigUtils.createConfig());	
			new MatsimPopulationReader(scenarioFreight).readFile(freightPlansFile.toString());
			for (Person person : scenarioFreight.getPopulation().getPersons().values()){
				populationOut.addPerson(person);
			}
		}
	}

	/**
	 * remove persons from the population that get extremely negative score
	 * @param population
	 */
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

	/**
	 * 
	 * Separates the initial population in two separate populations:
	 * <ul>
	 * <li>first act = last act</li>
	 * <li>first act != last act</li>
	 * </ul>
	 * 
	 * All plans of the returned populations have
	 * <ol>
	 * <li>at least three plan elements</li>
	 * <li>no coords equal to (0,0) or null</li>
	 * <li>activity start and end times only BEFORE midnight</li>
	 * </ol>
	 * 
	 * @param population
	 * @return
	 */
	private Map<String, Population> getPlansBeforeMidnight(Population population){
//		top=-33.0144 left=-71.3607 bottom=-33.8875 right=-70.4169
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("EPSG:4326", SantiagoScenarioConstants.toCRS);
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
		
		//for persons with a0 = aX
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop = sc.getPopulation();
		//for persons with a0 != aX
		Scenario sc2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop2 = sc2.getPopulation();
		
		for(Person person : population.getPersons().values()){
			if(person.getSelectedPlan().getPlanElements().size() < 3){
				continue;
			}
			boolean addPerson = true;
			double lastStartTime = Double.NEGATIVE_INFINITY;
			double lastEndTime = Double.NEGATIVE_INFINITY;
			
			Plan selectedPlan = person.getSelectedPlan();
			
			for(PlanElement pe : selectedPlan.getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					if(act.getCoord() == null){
						addPerson = false;
						break;
					}
					if(act.getCoord().getX() == 0 || act.getCoord().getY() == 0){
						addPerson = false;
						break;
					}
					if(act.getCoord().getX() < minX || act.getCoord().getX() > maxX ||
							act.getCoord().getY() < minY || act.getCoord().getY() > maxY){
						addPerson = false;
						break;
					}
					if(act.getStartTime() != Time.UNDEFINED_TIME){
						if(act.getStartTime() > lastStartTime && act.getStartTime() <= 24*3600){
							lastStartTime = act.getStartTime();
						} else{
							addPerson = false;
							break;
						}
					}
					if(act.getEndTime() != Time.UNDEFINED_TIME){
						if(act.getEndTime() > lastEndTime && act.getEndTime() <= 24*3600){
							lastEndTime = act.getEndTime();
						} else{
							addPerson = false;
							break;
						}
					}
					if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
						if(act.getStartTime() > act.getEndTime()){
							addPerson = false;
							break;
						}
					}
				}
			}
			
			if(!addPerson) continue;
			
			Activity firstAct = (Activity) selectedPlan.getPlanElements().get(0);
			Activity lastAct = (Activity) selectedPlan.getPlanElements().get(selectedPlan.getPlanElements().size()-1);
			
			if(firstAct.getType().equals(lastAct.getType())){
				pop.addPerson(person);
			} else{
				pop2.addPerson(person);
			}
		}
		
		Map<String, Population> populationMap = new HashMap<String, Population>();
		populationMap.put(popA0eAX, pop);
		populationMap.put(popA0neAX, pop2);
		
		return populationMap;
	}
	
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
	
	private Population cutPlansTo24H(Population population){
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population populationOut = scenario.getPopulation();
		PopulationFactoryImpl popFactory = (PopulationFactoryImpl)populationOut.getFactory();
		
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
					if(!act.getType().equals("pt interaction")){
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
	
	private void randomizeEndTimes(Population population){
		log.info("Randomizing activity end times...");
		Random random = MatsimRandom.getRandom();
		for(Person person : population.getPersons().values()){
			double timeShift = 0.;
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				if(pe instanceof Activity){
					Activity act = (Activity) pe;
					if(act.getStartTime() != Time.UNDEFINED_TIME && act.getEndTime() != Time.UNDEFINED_TIME){
						if(act.getEndTime() - act.getStartTime() == 0){
							timeShift += 1800.;
						}
					}
				}
			}
			
			Activity firstAct = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			Activity lastAct = (Activity) person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1);
			
			double delta = 0;
			while(delta == 0){
				delta = createRandomEndTime(random);
				if(firstAct.getEndTime() + delta < 0){
					delta = 0;
				}
				if(lastAct.getStartTime() + delta + timeShift > 24 * 3600){
					delta = 0;
				}
				if(lastAct.getEndTime() != Time.UNDEFINED_TIME){
					// if an activity end time for last activity exists, it should be 24:00:00
					// in order to avoid zero activity durations, this check is done
					if(lastAct.getStartTime() + delta + timeShift >= lastAct.getEndTime()){
						delta = 0;
					}
				}
			}
			
			for(int i = 0; i < person.getSelectedPlan().getPlanElements().size(); i++){
				PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
				if(pe instanceof Activity){
					Activity act = (Activity)pe;
					if(!act.getType().equals("pt interaction")){
						if(person.getSelectedPlan().getPlanElements().indexOf(act) > 0){
							act.setStartTime(act.getStartTime() + delta);
						}
						if(person.getSelectedPlan().getPlanElements().indexOf(act) < person.getSelectedPlan().getPlanElements().size()-1){
							act.setEndTime(act.getEndTime() + delta);
						}
					}
//					else {
//						log.warn("This should not happen! ");
//					}
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
		double endTime = 20*60 * normal;
		
		return endTime;
	}
	
	/**
	 * 
	 * Convenience method for creating a config file along with a new population.
	 * Any changes in config parameters can be done in the sub-methods.
	 * The flow capacity and the counts scale factor are set automatically using the given population size of the Santiago Metropolitan area
	 * (see {@linkplain SantiagoScenarioConstants}) and the size of the MATSim population.
	 * 
	 * @param config
	 */
	private void setUpConfigParameters(Config config){
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
		
		//creation of more than one subpopulation in config not possible yet ->removed Module, creation in SantiagoScenarioRunner (edited by BK) ,KT 2015-09-15.
//		setSubtourModeChoiceParameters(config.subtourModeChoice());	//creation for more than one subpopulation not possibple in config, creation in SantiagoScenarioRunner (edited by BK) ,KT 2015-09-15.
		config.removeModule("subtourModeChoice");
		
//		setTimeAllocationMutatorParameters(config.timeAllocationMutator());
		setTravelTimeCalculatorParameters(config.travelTimeCalculator());
		setVspExperimentalParameters(config.vspExperimental());
	}
	
	private void setTransitParameters(TransitConfigGroup transit, TransitRouterConfigGroup transitRouter) {
		Set<String> transitModes = new HashSet<String>();
		transitModes.add(SantiagoScenarioConstants.Modes.bus.toString());
		transitModes.add(SantiagoScenarioConstants.Modes.metro.toString());
		transitModes.add(SantiagoScenarioConstants.Modes.train.toString());
		transit.setTransitModes(transitModes);
//		transit.setTransitScheduleFile(pathForMatsim + "input/transitschedule.xml");
		transit.setTransitScheduleFile(pathForMatsim + "input/transitschedule_simplified.xml");
		transit.setVehiclesFile(pathForMatsim + "input/transitvehicles.xml");
		transit.setUseTransit(true);
		transitRouter.setExtensionRadius(500);
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
		Set<String> snapshotFormat = new HashSet<String>();
		snapshotFormat.add("otfvis");
		cc.setSnapshotFormat(snapshotFormat);
		cc.setWriteEventsInterval(10);
		cc.setWritePlansInterval(10);
		cc.setWriteSnapshotsInterval(10);
	}
	
	private void setCountsParameters(CountsConfigGroup counts, double sampleSizeEOD){
		// TODO: check what adding taxi, colectiovo, motorcycle and freight changes
		counts.setAnalyzedModes(TransportMode.car);
		counts.setAverageCountsOverIterations(5);
		counts.setCountsScaleFactor(SantiagoScenarioConstants.N / sampleSizeEOD);
		counts.setDistanceFilter(null);
		counts.setDistanceFilterCenterNode(null);
		counts.setFilterModes(false);
		counts.setCountsFileName(pathForMatsim + "input/counts_merged_VEH_C01.xml");
		counts.setOutputFormat("all");
		counts.setWriteCountsInterval(10);
	}
	
	private void setGlobalParameters(GlobalConfigGroup global){
		global.setCoordinateSystem(SantiagoScenarioConstants.toCRS);
		global.setNumberOfThreads(2);
		global.setRandomSeed(4711);
	}
	
	private void setLinkStatsParameters(LinkStatsConfigGroup ls){
		ls.setAverageLinkStatsOverIterations(5);
		ls.setWriteLinkStatsInterval(10);
	}
	
	private void setNetworkParameters(NetworkConfigGroup net){
		net.setChangeEventInputFile(null);
		net.setInputFile(pathForMatsim + "input/network_merged_cl.xml.gz");
		net.setLaneDefinitionsFile(null);
		net.setTimeVariantNetwork(false);
	}
	
	private void setParallelEventHandlingParameters(ParallelEventHandlingConfigGroup peh){
		peh.setEstimatedNumberOfEvents(null);
		peh.setNumberOfThreads(2);
	}
	
	private void setPlanCalcScoreParameters(PlanCalcScoreConfigGroup pcs){
		pcs.setBrainExpBeta(1);
		pcs.setPathSizeLogitBeta(0.0);
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
		carParams.setMarginalUtilityOfDistance(0.0);
		carParams.setMarginalUtilityOfTraveling(-1.056);
		carParams.setMonetaryDistanceRate(-0.248);
		pcs.addModeParams(carParams);
		
		ModeParams rideParams = new ModeParams(TransportMode.ride);
		rideParams.setConstant(0.0);
		rideParams.setMarginalUtilityOfDistance(0.0);
		rideParams.setMarginalUtilityOfTraveling(-1.056);
		rideParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(rideParams);
		
		ModeParams taxiParams = new ModeParams(SantiagoScenarioConstants.Modes.taxi.toString());
		taxiParams.setConstant(0.0);
		taxiParams.setMarginalUtilityOfDistance(0.0);
		taxiParams.setMarginalUtilityOfTraveling(-1.056);
		taxiParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(taxiParams);
		
		ModeParams colectivoParams = new ModeParams(SantiagoScenarioConstants.Modes.colectivo.toString());
		colectivoParams.setConstant(0.0);
		colectivoParams.setMarginalUtilityOfDistance(0.0);
		colectivoParams.setMarginalUtilityOfTraveling(-1.056);
		colectivoParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(colectivoParams);
		
		ModeParams motorcycleParams = new ModeParams(SantiagoScenarioConstants.Modes.motorcycle.toString());
		motorcycleParams.setConstant(0.0);
		motorcycleParams.setMarginalUtilityOfDistance(0.0);
		motorcycleParams.setMarginalUtilityOfTraveling(-1.056);
		motorcycleParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(motorcycleParams);

		ModeParams schoolBusParams = new ModeParams(SantiagoScenarioConstants.Modes.school_bus.toString());
		schoolBusParams.setConstant(0.0);
		schoolBusParams.setMarginalUtilityOfDistance(0.0);
		schoolBusParams.setMarginalUtilityOfTraveling(-1.056);
		schoolBusParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(schoolBusParams);
		
		ModeParams busParams = new ModeParams(SantiagoScenarioConstants.Modes.bus.toString());
		busParams.setConstant(0.0);
		busParams.setMarginalUtilityOfDistance(0.0);
		busParams.setMarginalUtilityOfTraveling(-1.056);
		busParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(busParams);
		
		ModeParams metroParams = new ModeParams(SantiagoScenarioConstants.Modes.metro.toString());
		metroParams.setConstant(0.0);
		metroParams.setMarginalUtilityOfDistance(0.0);
		metroParams.setMarginalUtilityOfTraveling(-1.056);
		metroParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(metroParams);
		
		ModeParams trainParams = new ModeParams(SantiagoScenarioConstants.Modes.train.toString());
		trainParams.setConstant(0.0);
		trainParams.setMarginalUtilityOfDistance(0.0);
		trainParams.setMarginalUtilityOfTraveling(-1.056);
		trainParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(trainParams);
		
		ModeParams walkParams = new ModeParams(TransportMode.walk);
		walkParams.setConstant(0.0);
		walkParams.setMarginalUtilityOfDistance(0.0);
		walkParams.setMarginalUtilityOfTraveling(-1.056);
		walkParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(walkParams);
		
		ModeParams bikeParams = new ModeParams(TransportMode.bike);
		bikeParams.setConstant(0.0);
		bikeParams.setMarginalUtilityOfDistance(0.0);
		bikeParams.setMarginalUtilityOfTraveling(-1.056);
		bikeParams.setMonetaryDistanceRate(-0.0);
		pcs.addModeParams(bikeParams);
		
		ModeParams otherModeParams = new ModeParams(TransportMode.other);
		otherModeParams.setConstant(0.0);
		otherModeParams.setMarginalUtilityOfDistance(0.0);
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
		networkModes.add(SantiagoScenarioConstants.Modes.motorcycle.toString());
		networkModes.add(SantiagoScenarioConstants.Modes.school_bus.toString());
		pcr.setNetworkModes(networkModes);
		
	/*
	 * using the following modes as a network modes using time dependent car travel times
	 * */
//		ModeRoutingParams rideParams = new ModeRoutingParams(TransportMode.ride);
//		rideParams.setBeelineDistanceFactor(1.3);
//		rideParams.setTeleportedModeSpeed(34 / 3.6);
//		pcr.addModeRoutingParams(rideParams);
//		
//		ModeRoutingParams taxiParams = new ModeRoutingParams(Constants.Modes.taxi.toString());
//		taxiParams.setBeelineDistanceFactor(1.3);
//		taxiParams.setTeleportedModeSpeed(34 / 3.6);
//		pcr.addModeRoutingParams(taxiParams);
//		
//		ModeRoutingParams colectivoParams = new ModeRoutingParams(Constants.Modes.colectivo.toString());
//		colectivoParams.setBeelineDistanceFactor(1.3);
//		colectivoParams.setTeleportedModeSpeed(30 / 3.6);
//		pcr.addModeRoutingParams(colectivoParams);
//		
//		ModeRoutingParams motorcycleParams = new ModeRoutingParams(Constants.Modes.motorcycle.toString());
//		motorcycleParams.setBeelineDistanceFactor(1.3);
//		motorcycleParams.setTeleportedModeSpeed(34 / 3.6);
//		pcr.addModeRoutingParams(motorcycleParams);
//
//		ModeRoutingParams schoolBusParams = new ModeRoutingParams(Constants.Modes.school_bus.toString());
//		schoolBusParams.setBeelineDistanceFactor(1.3);
//		schoolBusParams.setTeleportedModeSpeed(25 / 3.6);
//		pcr.addModeRoutingParams(schoolBusParams);
		
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

			ModeRoutingParams trainParams = new ModeRoutingParams(SantiagoScenarioConstants.Modes.train.toString());
			trainParams.setBeelineDistanceFactor(1.3);
			trainParams.setTeleportedModeSpeed(50 / 3.6);
			pcr.addModeRoutingParams(trainParams);
		}
	/*
	 * end pt parameter settings
	 * */
		
		ModeRoutingParams walkParams = new ModeRoutingParams(TransportMode.walk);
		walkParams.setBeelineDistanceFactor(1.3);
		walkParams.setTeleportedModeSpeed(5 / 3.6);
		pcr.addModeRoutingParams(walkParams);
		
		ModeRoutingParams bikeParams = new ModeRoutingParams(TransportMode.bike);
		bikeParams.setBeelineDistanceFactor(1.3);
		bikeParams.setTeleportedModeSpeed(15 / 3.6);
		pcr.addModeRoutingParams(bikeParams);
		
		ModeRoutingParams otherModeParams = new ModeRoutingParams(TransportMode.other);
		otherModeParams.setBeelineDistanceFactor(1.3);
		otherModeParams.setTeleportedModeSpeed(25 / 3.6);
		pcr.addModeRoutingParams(otherModeParams);
	}
	
	private void setQSimParameters(QSimConfigGroup qsim, double sampleSizeEOD){
		qsim.setEndTime(Time.UNDEFINED_TIME);
		double flowCapFactor = (sampleSizeEOD / SantiagoScenarioConstants.N);
		qsim.setFlowCapFactor(flowCapFactor);
//		qsim.setStorageCapFactor(0.015);
		qsim.setStorageCapFactor(flowCapFactor * 3.);
		qsim.setInsertingWaitingVehiclesBeforeDrivingVehicles(false);
		qsim.setLinkDynamics(LinkDynamics.FIFO.name());
//		qsim.setLinkWidth(30);
		Set<String> mainModes = new HashSet<String>();
		mainModes.add(TransportMode.car);
		qsim.setMainModes(mainModes);
		qsim.setNodeOffset(0.0);
		qsim.setNumberOfThreads(2);
		qsim.setSimStarttimeInterpretation(StarttimeInterpretation.maxOfStarttimeAndEarliestActivityEnd);
		qsim.setSnapshotStyle(SnapshotStyle.equiDist);
		qsim.setSnapshotPeriod(0.0);
		qsim.setStartTime(Time.UNDEFINED_TIME);
		qsim.setStuckTime(10.0);
		qsim.setTimeStepSize(1.0);
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
		
	//creation for more than one subpopulation moved to SantiagoScenarioRunner for consistency reasons
//		StrategySettings changeExpBeta = new StrategySettings();
//		changeExpBeta.setStrategyName("ChangeExpBeta");
//		changeExpBeta.setWeight(0.7);
//		strategy.addStrategySettings(changeExpBeta);
//		
//		StrategySettings reRoute = new StrategySettings();
//		reRoute.setStrategyName("ReRoute");
//		reRoute.setWeight(0.15);
//		strategy.addStrategySettings(reRoute);
		
	//creation for more than one subpopulation not possibple in config, creation in SantiagoScenarioRunner (edited by BK) ,KT 2015-09-15.
//		StrategySettings subTourModeChoice = new StrategySettings();
//		subTourModeChoice.setStrategyName("SubtourModeChoice");
//		subTourModeChoice.setSubpopulation(SubpopulationValues.carAvail);
//		subTourModeChoice.setWeight(0.15);
//		strategy.addStrategySettings(subTourModeChoice);
	}
	
	//creation for more than one subpopulation not possibple in config, creation in SantiagoScenarioRunner (edited by BK) ,KT 2015-09-15.
//	private void setSubtourModeChoiceParameters(SubtourModeChoiceConfigGroup smc){
//		smc.setChainBasedModes(new String[]{TransportMode.car, TransportMode.bike});
//		smc.setConsiderCarAvailability(true); // true or false ? (KT 2015-09-18); is not considered anyways (BK 2015-10-19)
//		smc.setModes(new String[]{TransportMode.car, Constants.Modes.bus.toString(), Constants.Modes.metro.toString(), TransportMode.walk, TransportMode.bike});
//	}
	
//	private void setTimeAllocationMutatorParameters(TimeAllocationMutatorConfigGroup tam){
//		tam.setAffectingDuration(false);
//		tam.setMutationRange(7600.);
//	}
	
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
}