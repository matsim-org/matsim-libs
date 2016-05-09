package playground.singapore.springcalibration.preprocess;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;


public class PlanCorrector {
	
	private final static Logger log = Logger.getLogger(PlanCorrector.class);

	public static void main(String[] args) {
		PlanCorrector corrector = new PlanCorrector();
		corrector.run(args[0], args[1], args[2]);
	}
	
	public void run(String plansFile, String facilitiesFile, String plansOutFile) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario).readFile(plansFile);
		new MatsimFacilitiesReader(scenario).readFile(facilitiesFile);
		
		this.addfacilities2Freight(scenario);
		
		this.writePlans(scenario.getPopulation(), scenario.getNetwork(), plansOutFile);
		log.info("finished ###################################################");
		
	}
	
	private void addfacilities2Freight(MutableScenario scenario) {
		
		Population population = scenario.getPopulation();
		ActivityFacilities facilities = scenario.getActivityFacilities();
		QuadTree<Id<ActivityFacility>> facilitiesQuadTree = this.buildQuadTree(scenario, "freight");
		
		int freightCnt = 0;
		for (Person p : population.getPersons().values()) {
			Plan plan = p.getSelectedPlan();
			
			for (PlanElement pe : plan.getPlanElements()){	
				if(pe instanceof Activity){
					Activity act = ((Activity) pe);
					String type = act.getType();
					
					if (type.equals("freight")) {
						Id<ActivityFacility> facilityId = facilitiesQuadTree.getClosest(act.getCoord().getX(), act.getCoord().getY());
						act.setFacilityId(facilityId);
						freightCnt++;
						double distance = CoordUtils.calcEuclideanDistance(act.getCoord(), facilities.getFacilities().get(facilityId).getCoord());
						
						if (distance > 50.0) {
							log.info("Long difference for agent " + p.getId().toString() + " and act ending at " + act.getEndTime() + ": " + distance);
						}
						
					}
 				}
			}	
		}
		log.info("Converted " + freightCnt + " freight agents.");
	}

	
	private void writePlans(Population population, Network network, String outFile) {
		PopulationWriter writer = new PopulationWriter(population);
		writer.write(outFile);
	}
	
private QuadTree<Id<ActivityFacility>> buildQuadTree(Scenario scenario, String type) {
		
		Map<Id<ActivityFacility>, ActivityFacility> facilities = scenario.getActivityFacilities().getFacilitiesForActivityType(type);
		
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		
		for (ActivityFacility facility : facilities.values()) {
			Coord coord = facility.getCoord();
			double x = coord.getX();
			double y = coord.getY();
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;
			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
		}
		QuadTree<Id<ActivityFacility>> quadTree = new QuadTree<Id<ActivityFacility>>(minX, minY, maxX, maxY);
		for (ActivityFacility facility : facilities.values()) {
			Coord coord = facility.getCoord();
			double x = coord.getX();
			double y = coord.getY();
			quadTree.put(x, y, facility.getId());
		}		
		return quadTree;
	}

}
