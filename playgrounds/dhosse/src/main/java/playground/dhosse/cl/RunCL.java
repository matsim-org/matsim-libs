package playground.dhosse.cl;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

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
		
//		for conversion of raw data into matsim plans
//		CSVToPlans converter = new CSVToPlans(matsimInputDir + "plans.xml.gz", 
//											  boundariesInputDir + "Boundaries_20150428_085038.shp");
//		converter.run(databaseFilesDir + "Persona.csv",
//					  databaseFilesDir + "Export_Viaje.csv",
//					  databaseFilesDir + "Etapa.csv");
//		
//		System.out.println(Time.writeTime(converter.latestStart) + "\t" + Time.writeTime(converter.latestEnd));
		
//		new NetConverter().convertTransitSchedule(transitFilesDir + "transitSchedule_tertiary.xml.gz");
		
//		String path = "C:/Users/Daniel/Documents/work/shared-svn/studies/countries/cl/santiago_pt_demand_matrix/network_dhosse/";
//		new NetConverter().createNetwork(path + "santiago_tertiary.osm", path + "santiago_tertiary.xml.gz");
		
//		OTFVis.playNetwork(path + "santiago_primary.xml.gz");
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, matsimInputDir + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
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
//		randomizeEndTime(scenario.getPopulation());
		
//		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
//		Controler controler = new Controler(scenario);
//		controler.run();
		
//		LegModeDistanceDistribution lmdd = new LegModeDistanceDistribution();
//		lmdd.init(scenario);
//		lmdd.preProcessData();
//		lmdd.postProcessData();
//		lmdd.writeResults(matsimInputDir);
		
//		for(Person person : scenario.getPopulation().getPersons().values()){
//			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Activity){
//					randomizeEndTime((Activity)pe);
//				}
//			}
//		}
//		
//		ReadAndAddSubActivities acts = new ReadAndAddSubActivities(matsimInputDir + "config.xml", scenario);
//		acts.run(config.plans().getInputFile(), matsimInputDir + "config_amit.xml");
		
//		double maxStartTime = Double.NEGATIVE_INFINITY; 
//		double maxEndTime = Double.NEGATIVE_INFINITY;
//		for(Person person : scenario.getPopulation().getPersons().values()){
//			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
//				if(pe instanceof Activity){
//					Activity act = (Activity)pe;
//					if(act.getEndTime() != Time.UNDEFINED_TIME){
//						if(act.getEndTime() > maxEndTime)
//							maxEndTime = act.getEndTime();
//					}
//					if(act.getStartTime() != Time.UNDEFINED_TIME){
//						if(act.getStartTime() > maxStartTime)
//							maxStartTime = act.getStartTime();
//					}
//				}
//			}
//		}
//		
//		System.out.println("max start time: " + Time.writeTime(maxStartTime) + "; max end time: " + Time.writeTime(maxEndTime));
		
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
			
			for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
				
				if(pe instanceof Activity){

					Activity act = (Activity)pe;
					int index = person.getSelectedPlan().getPlanElements().indexOf(act);
					
					if(!act.getType().equals("pt interaction")){
						
						if(index < person.getSelectedPlan().getPlanElements().size() - 2){
							
							Activity act2 = (Activity) person.getSelectedPlan().getPlanElements().get(index + 2);
							
							if(act2.getType().equals("pt interaction")){
								System.out.println(person.getId().toString());
								act2 = (Activity) person.getSelectedPlan().getPlanElements().get(index + 4);
								
							}
							
							double ttime = act2.getStartTime() - act.getEndTime();
							
							createRandomEndTime(random, act);
							
							act2.setStartTime(act.getEndTime() + ttime);
							
						}
						
					}
					
				}
				
			}
			
		}
		
		new PopulationWriter(population).write(matsimInputDir + "plans_rand.xml");
		
	}
	
	private static void createRandomEndTime(Random random, Activity act){
		
		//draw two random numbers [0;1] from uniform distribution
		double r1 = random.nextDouble();
		double r2 = random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = 10*60 * normal + act.getEndTime();
		act.setEndTime(endTime);
		
	}

}
