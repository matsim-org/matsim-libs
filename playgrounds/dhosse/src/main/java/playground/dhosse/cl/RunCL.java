package playground.dhosse.cl;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;

import playground.agarwalamit.munich.inputs.ReadAndAddSubActivities;

public class RunCL {

	static String svnWorkingDir = "C:/Users/Daniel/Documents/work/shared-svn/studies/countries/cl/";
	static String workingDirInputFiles = svnWorkingDir + "Kai_und_Daniel/";
	static String boundariesInputDir = workingDirInputFiles + "exported_boundaries/";
	static String databaseFilesDir = workingDirInputFiles + "exportedFilesFromDatabase/";
	static String visualizationsDir = workingDirInputFiles + "Visualisierungen/";
	static String matsimInputDir = workingDirInputFiles + "inputFiles/";
	
	static String transitFilesDir = svnWorkingDir + "/santiago_pt_demand_matrix/pt_stops_schedule_2013/";
	static String gtfsFilesDir = svnWorkingDir + "/santiago_pt_demand_matrix/gtfs_201306/";
	
	public static void main(String args[]){
		
//		OTFVis.playConfig(matsimInputDir + "config.xml");
//		OTFVis.playMVI(matsimInputDir + "output/ITERS/it.0/0.otfvis.mvi");
		
//		double[] box = NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values());
//		System.out.println("(" + box[0] + "," + box[1] + "),(" + box[2] + "," + box[3] + ")");
//		Controler controler = new Controler(scenario);
//		controler.setOverwriteFiles(true);
//		controler.run();
		
//		OTFVis.playScenario(scenario);
		
//		new NetConverter().plans2Shape(scenario.getPopulation(), visualizationsDir + "activities.shp");
		
//		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//		new NetworkReaderMatsimV1(scenario).parse(matsimInputDir + "secondary.xml.gz");
		
		//coordinate conversion from EPSG:3857 to EPSG:32719
//		new NetConverter().convertCoordinates(scenario.getNetwork(), matsimInputDir + "santiago_tiny_19S.xml");
		
		//for conversion of links into shape file
//		System.out.println(scenario.getNetwork().getLinks().size());
//		System.out.println(config.network().getInputFile());
//		new NetConverter().convertNet2Shape(scenario.getNetwork(), "EPSG:32719", visualizationsDir + "santiago_secondary.shp");
//		
//		System.out.println(Time.writeTime(converter.latestStart) + "\t" + Time.writeTime(converter.latestEnd));
		
//		new NetConverter().convertTransitSchedule(transitFilesDir + "transitSchedule_tertiary.xml.gz");
		
//		String path = "C:/Users/Daniel/Documents/work/shared-svn/studies/countries/cl/santiago_pt_demand_matrix/network_dhosse/";
//		new NetConverter().createNetwork(path + "santiago_tertiary.osm", path + "santiago_tertiary.xml.gz");
		
//		OTFVis.playNetwork(path + "santiago_primary.xml.gz");
		
//		for conversion of raw data into matsim plans
		
//		Config config = ConfigUtils.createConfig();
//		
//		PlanCalcScoreConfigGroup pcs = config.planCalcScore();
//		pcs.addModeParams(pcs.getOrCreateModeParams("feeder bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("main bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("subway"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("collective taxi"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("school bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("taxi"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("motorcycle"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("institutional bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("rural bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("school bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("urban bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("other"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("train"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("ride"));
//		
//		PlansCalcRouteConfigGroup pcr = config.plansCalcRoute();
//		Set<String> networkModes = new HashSet<String>();
//		networkModes.add(TransportMode.car);
//		pcr.setNetworkModes(networkModes);
//		
//		ModeRoutingParams pars = pcr.getModeRoutingParams().get(TransportMode.walk);
//		pars.setTeleportedModeSpeed(5.0/3.6);
//		
//		pars = pcr.getModeRoutingParams().get(TransportMode.bike);
//		pars.setTeleportedModeSpeed(15.0/3.6);
//		
//		pars = new ModeRoutingParams("feeder bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("main bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("subway");
//		pars.setTeleportedModeSpeed(32.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("collective taxi");
//		pars.setTeleportedModeSpeed(30.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("school bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("taxi");
//		pars.setTeleportedModeSpeed(34.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("motorcycle");
//		pars.setTeleportedModeSpeed(34.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("institutional bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("rural bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("urban bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("other");
//		pars.setTeleportedModeSpeed(50.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("train");
//		pars.setTeleportedModeSpeed(50.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("ride");
//		pars.setTeleportedModeSpeed(34.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("walk");
//		pars.setTeleportedModeSpeed(3.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("bike");
//		pars.setTeleportedModeSpeed(15.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		config.network().setInputFile(matsimInputDir + "santiago_secondary.xml.gz");
//		config.counts().set
		
//		new NetConverter().createNetwork(svnWorkingDir + "santiago_pt_demand_matrix/network_dhosse/chile-allWays.osm", matsimInputDir + "santiago_tiny.xml.gz");
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReaderMatsimV5(scenario).parse("C:/Users/Daniel/Documents/work/runs-svn/santiago/input/plans_amit2.xml");
//		new NetworkReaderMatsimV1(scenario).parse(matsimInputDir + "santiago_tiny.xml.gz");
//		new NetConverter().convertCoordinates(scenario.getNetwork(), matsimInputDir + "networkWGS84.xml");
		new NetConverter().plans2Shape(scenario.getPopulation(), visualizationsDir + "persons.shp");
		
//		CSVToPlans converter = new CSVToPlans(matsimInputDir + "plans.xml.gz", 
//											  boundariesInputDir + "Boundaries_20150428_085038.shp");
//		converter.run(databaseFilesDir + "Hogar.csv",
//					  databaseFilesDir + "Persona.csv",
//					  databaseFilesDir + "Export_Viaje.csv",
//					  databaseFilesDir + "Etapa.csv");
//		
//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, matsimInputDir + "config.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		
//		randomizeEndTime(scenario.getPopulation());
//		
//		config.plans().setInputFile(matsimInputDir + "plans_rand.xml");
//		
//		new ConfigWriter(config).write(matsimInputDir + "config_amit.xml");
//		
//		Config config2 = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config2, matsimInputDir + "config_amit.xml");
//		Scenario sc = ScenarioUtils.loadScenario(config2);
//		
//		ReadAndAddSubActivities rsa = new ReadAndAddSubActivities(matsimInputDir + "config_amit.xml", sc);
//		rsa.run(matsimInputDir + "config_amit.xml", config2.plans().getInputFile(), matsimInputDir + "plans_amit2.xml"); 
		
//		new PopulationWriter(scenario.getPopulation()).write(matsimInputDir + "plans_rand.xml");
		
//		int cnt = 0;
//		double latestEndTime = Double.NEGATIVE_INFINITY;
//		double latestStartTime = Double.NEGATIVE_INFINITY;
//		
//		for(Person person : scenario.getPopulation().getPersons().values()){
//			
//			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Activity){
//					Activity act = (Activity)pe;
//					double start = act.getStartTime();
//					double end = act.getEndTime();
//					if(start > latestStartTime) latestStartTime = start;
//					if(end > latestEndTime) latestEndTime = end;
//				}
//			}
//			
//		}
//		System.out.println(Time.writeTime(latestStartTime) + "\t" + Time.writeTime(latestEndTime));
		
//		ReadAndAddSubActivities acts = new ReadAndAddSubActivities(matsimInputDir + "config.xml", scenario);
//		acts.run(config.plans().getInputFile(), matsimInputDir + "config_amit.xml");
		
//		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
//		Controler controler = new Controler(scenario);
//		controler.run();
		
//		new CreateTransitLines(scenario, gtfsFilesDir + "trips.txt", gtfsFilesDir + "stops.txt", gtfsFilesDir + "stop_times.txt", gtfsFilesDir + "frequencies.txt").run("L");
		
//		Set<Id<Person>> personsToRemove = new HashSet<>();
//		
//		for(Person p : scenario.getPopulation().getPersons().values()){
//			
//			Plan plan = p.getSelectedPlan();
//			
//			if(plan.getPlanElements().get(plan.getPlanElements().size() - 1) instanceof Activity){
//				
//				Activity act = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);
//				if(act.getType().equals("pt interaction")){
//					personsToRemove.add(p.getId());
//				}
//				
//			}
//			
//		}
//		
//		for(Id<Person> key: personsToRemove){
//			scenario.getPopulation().getPersons().remove(key);
//		}
//		

//		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
//		lmdd.init(scenario);
//		lmdd.preProcessData();
//		lmdd.postProcessData();
//		lmdd.writeResults(matsimInputDir);
		
//		PlanCalcScoreConfigGroup pcs = config.planCalcScore();
//		pcs.addModeParams(pcs.getOrCreateModeParams("feeder bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("main bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("subway"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("collective taxi"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("school bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("taxi"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("motorcycle"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("institutional bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("rural bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("school bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("urban bus"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("other"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("train"));
//		pcs.addModeParams(pcs.getOrCreateModeParams("ride"));
//		
//		PlansCalcRouteConfigGroup pcr = config.plansCalcRoute();
//		Set<String> networkModes = new HashSet<String>();
//		networkModes.add(TransportMode.car);
//		pcr.setNetworkModes(networkModes);
//		
//		ModeRoutingParams pars = pcr.getModeRoutingParams().get(TransportMode.walk);
//		pars.setTeleportedModeSpeed(5.0/3.6);
//		
//		pars = pcr.getModeRoutingParams().get(TransportMode.bike);
//		pars.setTeleportedModeSpeed(15.0/3.6);
//		
//		pars = new ModeRoutingParams("feeder bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("main bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("subway");
//		pars.setTeleportedModeSpeed(32.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("collective taxi");
//		pars.setTeleportedModeSpeed(30.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("school bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("taxi");
//		pars.setTeleportedModeSpeed(34.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("motorcycle");
//		pars.setTeleportedModeSpeed(34.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("institutional bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("rural bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("urban bus");
//		pars.setTeleportedModeSpeed(25.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("other");
//		pars.setTeleportedModeSpeed(50.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("train");
//		pars.setTeleportedModeSpeed(50.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("ride");
//		pars.setTeleportedModeSpeed(34.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("walk");
//		pars.setTeleportedModeSpeed(3.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		pars = new ModeRoutingParams("bike");
//		pars.setTeleportedModeSpeed(15.0/3.6);
//		pars.setBeelineDistanceFactor(1.3);
//		pcr.addModeRoutingParams(pars);
//		
//		ConfigWriter writer = new ConfigWriter(config);
//		writer.write(matsimInputDir + "config_v2.xml");
		
//		new NetConverter().convertCounts2Shape(inputDirCSV + "puntos.csv", outputDirShp + "puntos.shp");
		
	}
	
	private static void randomizeEndTime(Population population){
		
		Random random = MatsimRandom.getRandom();
		
		for(Person person : population.getPersons().values()){
			
//			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
//				if(pe instanceof Activity){
					
					if(person.getId().toString().equals("10002005")){
						System.out.println("");
					}

					Activity act = (Activity)person.getSelectedPlan().getPlanElements().get(0);
					int index = person.getSelectedPlan().getPlanElements().indexOf(act);
					Activity lastAct = (Activity)person.getSelectedPlan().getPlanElements().get(person.getSelectedPlan().getPlanElements().size()-1);
					
//					if(!act.getType().equals("pt interaction")){
						
						double delta = 0;
						
						while(delta == 0){
							delta = createRandomEndTime(random, act);
							if(act.getEndTime() < 0){
								act.setEndTime(act.getEndTime() - delta);
								delta = 0;
							} else if(lastAct.getEndTime() + delta > 24 * 3600){
								act.setEndTime(act.getEndTime() - delta);
								delta = 0;
							}
						}
						
						for(int i = index + 1; i < person.getSelectedPlan().getPlanElements().size(); i++){
							
							PlanElement pel = person.getSelectedPlan().getPlanElements().get(i);
							
							if(pel instanceof Activity){
								
								Activity activity = (Activity)pel;
								if(activity.getStartTime() != Time.UNDEFINED_TIME){
									activity.setStartTime(activity.getStartTime() + delta);
								} else{
									activity.setMaximumDuration(0.);
								}
								if(activity.getEndTime() != Time.UNDEFINED_TIME){
									activity.setEndTime(activity.getEndTime() + delta);
								}
								
							}
							
						}
							
						
//					}
					
				}
				
//			}
			
//		}
		
		new PopulationWriter(population).write(matsimInputDir + "plans_rand.xml");
		
	}
	
	private static double createRandomEndTime(Random random, Activity act){
		
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = 20*60 * normal + act.getEndTime();
		double delta = endTime - act.getEndTime();
		act.setEndTime(endTime);
		
		return delta;
		
	}

}
