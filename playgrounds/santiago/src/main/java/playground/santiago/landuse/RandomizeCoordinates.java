package playground.santiago.landuse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.opengis.feature.simple.SimpleFeature;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class RandomizeCoordinates {
	

	final String svnWorkingDir = "../../../shared-svn/projects/santiago/scenario/";
	final String runsWorkingDir = "../../../runs-svn/santiago/TMP/input/";

	final ShapeFileReader reader = new ShapeFileReader();
	Collection<SimpleFeature> features = reader.readFileAndInitialize("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/3_ShapeZonasEOD/zonificacion_eod2012.shp");

	final String plansFolder = svnWorkingDir + "inputForMATSim/plans/2_10pct/";
	final String plansFile = plansFolder + "expanded_plans_1.xml.gz";
	
	final String configFolder = svnWorkingDir + "inputForMATSim/";
	final String configFile = configFolder + "expanded_config_1.xml";
	
	
	

	

	private void Run(){
		
		

		Config config = ConfigUtils.loadConfig(configFile);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());		
		PopulationReader pr = new PopulationReader(scenario);		
		pr.readFile(plansFile);		
		Population expandedPlans = scenario.getPopulation();		
		Map <Id,Integer> AgentCondition = getAgentCondition(expandedPlans);
		Population newPlans = createNewPlans(AgentCondition, expandedPlans, features);
		writeNewPopulation (newPlans);
		writeNewConfig (config);

		
	}
	
	public static void main(String[] args) {
		
		RandomizeCoordinates rcwfi = new RandomizeCoordinates();
		rcwfi.Run();
		
	}
	
	private long getEODZone (Coord coord , Collection<SimpleFeature> features){
		
		Point point = MGC.xy2Point(coord.getX(), coord.getY());		
		Map<Long,Geometry>geometriesById = new HashMap<>();
		
		for (SimpleFeature feature : features) {
			
			geometriesById.put((Long) feature.getAttribute("ID"),(Geometry) feature.getDefaultGeometry());
			
		}
		
		long zone=1;
		for (long id : geometriesById.keySet()){
			if(geometriesById.get(id).contains(point)){
				
				zone = id ;
				break;

			}
		}
		
		return zone;
		
	}
	
	private Multimap <Long,ActivityFacility> buildTheMap (String activityType, Collection<SimpleFeature> features){

		/**************/
		String IDAct = activityType.substring(0,2);
		Config config = ConfigUtils.createConfig();
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		/*Only necessary when the activity type has multiple activity options*/
		Scenario scenarioAux1 = ScenarioUtils.createScenario(config);
		Scenario scenarioAux2 = ScenarioUtils.createScenario(config);
		Scenario scenarioAux3 = ScenarioUtils.createScenario(config);
		/*************************************************************/
		
		MatsimFacilitiesReader fr = new MatsimFacilitiesReader(scenario);
		
		/*Only necessary when the activity type has multiple activity options*/
		MatsimFacilitiesReader frAux1 = new MatsimFacilitiesReader(scenarioAux1);
		MatsimFacilitiesReader frAux2 = new MatsimFacilitiesReader(scenarioAux2);
		MatsimFacilitiesReader frAux3 = new MatsimFacilitiesReader(scenarioAux3);
		/************************************************************/
		
		switch (IDAct){
		
		case ("ho"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/hogByArea.xml");
			break;
			
		case ("wo"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/admByArea.xml");
			frAux1.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/indByArea.xml");
			frAux2.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/minByArea.xml");
			frAux3.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/ofByArea.xml");
			break;
			
		case ("bu"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/admByArea.xml");
			frAux1.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/indByArea.xml");
			frAux2.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/minByArea.xml");
			frAux3.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/ofByArea.xml");
			break;
			
		case("ed"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/edByArea.xml");
			break;
			
		case("he"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/salByArea.xml");
			break;
			
		case("vi"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/hogByArea.xml");
			break;
			
		case("sh"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/comByArea.xml");
			break;
			
		case("le"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/cultByArea.xml");
			frAux1.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/depByArea.xml");
			frAux2.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/hotByArea.xml");
			break;
			
		case("ot"):
			fr.readFile("../../../shared-svn/projects/santiago/scenario/inputFromElsewhere/landUse/SII/FacilitiesFile/otrByArea.xml");
			break;
		}
		/**************/
		
		
		Multimap<Long, ActivityFacility > actFacilitiesByTAZ = HashMultimap.create();
		
		

		/*Simple cases*/
		if (IDAct.equals("ho")||IDAct.equals("ed")||IDAct.equals("he")||IDAct.equals("sh")||IDAct.equals("vi")||IDAct.equals("ot")){
		
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			
				Coord coord = facility.getCoord();
				long TAZId = getEODZone (coord, features);				
				actFacilitiesByTAZ.put(TAZId,facility);

				
			}
		
		/*work and busy*/
		} else if (IDAct.equals("wo")||IDAct.equals("bu")){
			
				for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
				
					Coord coord = facility.getCoord();
					long TAZId = getEODZone (coord, features);				
					actFacilitiesByTAZ.put(TAZId,facility);
				}
			
				for (ActivityFacility facility : scenarioAux1.getActivityFacilities().getFacilities().values()) {
					
					Coord coord = facility.getCoord();
					long TAZId = getEODZone (coord, features);				
					actFacilitiesByTAZ.put(TAZId,facility);
				}
				
				for (ActivityFacility facility : scenarioAux2.getActivityFacilities().getFacilities().values()) {
					
					Coord coord = facility.getCoord();
					long TAZId = getEODZone (coord, features);				
					actFacilitiesByTAZ.put(TAZId,facility);
				}
				
				for (ActivityFacility facility : scenarioAux3.getActivityFacilities().getFacilities().values()) {
					
					Coord coord = facility.getCoord();
					long TAZId = getEODZone (coord, features);				
					actFacilitiesByTAZ.put(TAZId,facility);
				}
				
		/*leisure*/
		} else {
						
			for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
			
				Coord coord = facility.getCoord();
				long TAZId = getEODZone (coord, features);				
				actFacilitiesByTAZ.put(TAZId,facility);
			}
		
			for (ActivityFacility facility : scenarioAux1.getActivityFacilities().getFacilities().values()) {
				
				Coord coord = facility.getCoord();
				long TAZId = getEODZone (coord, features);				
				actFacilitiesByTAZ.put(TAZId,facility);
			}
			
			for (ActivityFacility facility : scenarioAux2.getActivityFacilities().getFacilities().values()) {
				
				Coord coord = facility.getCoord();
				long TAZId = getEODZone (coord, features);				
				actFacilitiesByTAZ.put(TAZId,facility);
			}
			
		}
		
	
		return actFacilitiesByTAZ;
		
		
	}
	
	private Coord selectRandomFacility (Multimap <Long,ActivityFacility> activityByTAZ, Long TAZId){
 
	Collection<ActivityFacility> setToSearch = activityByTAZ.get(TAZId);

	int size = setToSearch.size();
	
	Coord coord=new Coord(0,0);

	if (size!=0){
		
		int item = new Random().nextInt(size);	
		int i = 0;

		for(ActivityFacility facility : setToSearch){	

			if (i == item){			
//				coord.setXY(facility.getCoord().getX(), facility.getCoord().getY());
				coord = facility.getCoord();


			}
			i = i + 1;
		}

	}	
	return coord;

 }

	private Map <Id,Integer> getAgentCondition(Population originalPlans){
		
		ArrayList<String> repeatedPopulationIds=new ArrayList<>();
		ArrayList<Id> populationIds = new ArrayList<Id>();
		Map <Id,Integer> agentCondition = new HashMap<Id,Integer>();
		
		for (Person p : originalPlans.getPersons().values()) {
			
			String tempIds = p.getId().toString();
			String [] partIds = tempIds.split("_");
			repeatedPopulationIds.add(partIds[0]);
			populationIds.add(p.getId());
			
		}
		int i=0;
		int j=1;
		
		//The first agent is an original agent.
		agentCondition.put(populationIds.get(i), 0);
		
		while(j<populationIds.size()){
			if (repeatedPopulationIds.get(i).equals(repeatedPopulationIds.get(j))){
				agentCondition.put(populationIds.get(j), 1);

				j=j+1;
			}else{
					agentCondition.put(populationIds.get(j),0);
					i=j;
					j=j+1;
				
			}
		}
		
		return agentCondition;

		}
		
	private Population createNewPlans(Map<Id, Integer> agentCondition, Population originalPlans, Collection<SimpleFeature> features){
		
		Multimap <Long,ActivityFacility> hogByTAZ = buildTheMap("home", features);
		Multimap <Long,ActivityFacility> workByTAZ = buildTheMap("work", features);
		Multimap <Long,ActivityFacility> busByTAZ = buildTheMap("busiest", features);
		Multimap <Long,ActivityFacility> edByTAZ = buildTheMap("education", features);
		Multimap <Long,ActivityFacility> healthByTAZ = buildTheMap("health", features);
		Multimap <Long,ActivityFacility> visitByTAZ = buildTheMap("visit", features);
		Multimap <Long,ActivityFacility> shopByTAZ = buildTheMap("shopping", features);
		Multimap <Long,ActivityFacility> leisByTAZ = buildTheMap("leisure", features);
		Multimap <Long,ActivityFacility> otherByTAZ = buildTheMap("other", features);
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		Population newPlans = scenario.getPopulation();
		Coord zeroCoord = new Coord (0,0);
		 
		for (Person p : originalPlans.getPersons().values()){

			Id<Person> pInId = Id.createPersonId( p.getId() );
			Person pIn = newPlans.getFactory().createPerson( pInId  );
			newPlans.addPerson( pIn );
			
			if (agentCondition.get(p.getId()).equals(1)){
				
				for (Plan plan : p.getPlans()){
				
					Plan planIn = newPlans.getFactory().createPlan();
					List<PlanElement> pes = plan.getPlanElements();
					
					for ( PlanElement pe : pes){
					
					if(pe instanceof Leg) {
					
						Leg leg = (Leg) pe;
						Leg legIn = newPlans.getFactory().createLeg(leg.getMode());
						planIn.addLeg(legIn);
						
					} else {
											
						Activity actTemp = (Activity) pe; 
						String tempType = actTemp.getType();						
						String IDAct = tempType.substring(0,2);
						Coord tempCoord = actTemp.getCoord();			
						long TAZId = getEODZone(tempCoord ,  features);
						Coord newCoord = new Coord (0,0);

						switch (IDAct){
						
						case ("ho"):
							newCoord = selectRandomFacility(hogByTAZ, TAZId);						
							break;
							
						case ("wo"):
							newCoord = selectRandomFacility(workByTAZ, TAZId);						
							break;
							
						case ("bu"):
							newCoord = selectRandomFacility(busByTAZ, TAZId);						
							break;
							
						case("ed"):
							newCoord = selectRandomFacility(edByTAZ, TAZId);						
							break;
							
						case("he"):
							newCoord = selectRandomFacility(healthByTAZ, TAZId);						
							break;
							
						case("vi"):
							newCoord = selectRandomFacility(visitByTAZ, TAZId);						
							break;
							
						case("sh"):
							newCoord = selectRandomFacility(shopByTAZ, TAZId);						
							break;
							
						case("le"):							
							newCoord = selectRandomFacility(leisByTAZ, TAZId);						
							break;
							
						case("ot"):
							newCoord = selectRandomFacility(otherByTAZ, TAZId);						
							break;
						}
						
						if (!newCoord.equals(zeroCoord)){
						
							Activity actIn = newPlans.getFactory().createActivityFromCoord(tempType, newCoord);
							planIn.addActivity(actIn);
							actIn.setEndTime(actTemp.getEndTime());
							actIn.setStartTime(actTemp.getStartTime());
							
						} else {
							
							Activity actIn = newPlans.getFactory().createActivityFromCoord(tempType, tempCoord);
							planIn.addActivity(actIn);
							actIn.setEndTime(actTemp.getEndTime());
							actIn.setStartTime(actTemp.getStartTime());
							
						}
						
	
					}
					
					
					}
					pIn.addPlan(planIn);
					
					
				}

				
				
			} else if (agentCondition.get(p.getId()).equals(0)) {
				

				for (Plan plan : p.getPlans()){
					
					Plan planIn = newPlans.getFactory().createPlan();
					List<PlanElement> pes = plan.getPlanElements();
					
					for ( PlanElement pe : pes){
					
					if(pe instanceof Leg) {
					
						Leg leg = (Leg) pe;
						Leg legIn = newPlans.getFactory().createLeg(leg.getMode());
						planIn.addLeg(legIn);
						
					} else {
						
						Activity actIn = (Activity)pe;
						Activity actOut = newPlans.getFactory().createActivityFromCoord(actIn.getType(), actIn.getCoord());
						planIn.addActivity(actOut);				
						actOut.setEndTime(actIn.getEndTime());
						actOut.setStartTime(actIn.getStartTime());

					}



					}
					
					pIn.addPlan(planIn);
				}
			}
		}

	
		return newPlans;
		
	}

	private void writeNewPopulation (Population population){
		
		PopulationWriter pw = new PopulationWriter(population);
		pw.write(plansFolder + "randomized_expanded_plans.xml.gz");
		
	}
	
	private void writeNewConfig (Config config){
		
		PlansConfigGroup plans = config.plans();
		plans.setInputFile(runsWorkingDir + "randomized_expanded_plans.xml.gz");
		new ConfigWriter(config).write(configFolder + "randomized_expanded_config.xml");
		
	}
	
}
