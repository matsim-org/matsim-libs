package playground.balac.utils;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.Facility;


public class AnalyseShoppingBehaviour {

	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath, String  facilitiesfilePath1) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		
		MutableScenario scenario1 = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new FacilitiesReaderMatsimV1(scenario1).readFile(facilitiesfilePath1);
		double centerX = 683217.0; 
	    double centerY = 247300.0;
		Coord coord = new Coord(centerX, centerY);
		int group1 = 0;
		int group2 = 0;
		int group3 = 0;
		int group4 = 0;
		int group5 = 0;
		int numberInside = 0;
		int size = scenario.getPopulation().getPersons().size();	
		
		for (Facility f : scenario1.getActivityFacilities().getFacilities().values()) {
			
			if (CoordUtils.calcEuclideanDistance(f.getCoord(), coord) < 4000)
				numberInside++;
		}
		
		int counter = 0;
		
	    for(Person p:scenario.getPopulation().getPersons().values()) {
	    	boolean groceryInside = false;
	    	boolean homeInside = false;
	    	boolean workInside = false;
	    	boolean educationInside = false;
	    	boolean leisureInside = false;
	    	boolean homeOutside = false;
	    	boolean workOutside = false;
	    	boolean educationOutside = false;
	    	boolean leisureOutside = false;
	    	boolean hasWork = false;
	    	boolean hasEducation = false;
	    	boolean hasLeisure = false;
	    	boolean hasNon = false;
	    	boolean nonInside = false;
	    	boolean insideActivity= false;
	    	boolean outsideActivity = false;
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe: plan.getPlanElements()) {
				if (pe instanceof Activity) {
				 if (((Activity) pe).getType().equals("home")) {
							if ( (CoordUtils.calcEuclideanDistance(((Activity)pe).getCoord(), coord) < 4000.0)) {
								homeInside = true;
							}
						
						
					}
					else if (((Activity) pe).getType().startsWith("work")) {
						hasWork = true;
							if ( (CoordUtils.calcEuclideanDistance(((Activity)pe).getCoord(), coord) < 4000.0)) {
								workInside = true;
						}
					
					
				}
					else if (((Activity) pe).getType().startsWith("education")) {
						hasEducation = true;
						if ( (CoordUtils.calcEuclideanDistance(((Activity)pe).getCoord(), coord) < 4000.0)) {
							educationInside = true;
						}
						else
							educationOutside = true;
				
				
			}
					else if (((Activity) pe).getType().startsWith("leisure")) {
						hasLeisure = true;
						if ( (CoordUtils.calcEuclideanDistance(((Activity)pe).getCoord(), coord) < 4000.0)) {
							leisureInside = true;
					}
				
				
			}
					else if (((Activity) pe).getType().startsWith("nongrocery")) {
						hasNon = true;
						if ( (CoordUtils.calcEuclideanDistance(((Activity)pe).getCoord(), coord) < 4000.0)) {
							nonInside = true;
					}
				
				
			}
					
				}
				
			}
			for (PlanElement pe: plan.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("shopgrocery") && scenario1.getActivityFacilities().getFacilities().containsKey(((Activity) pe).getFacilityId())) {
						
						if ( (CoordUtils.calcEuclideanDistance(((Activity)pe).getCoord(), coord) > 4000)) {
							groceryInside = true;	
							
						}
						else 
							groceryInside = false;
						if (groceryInside)
						{						if ((hasWork && workInside) || (hasEducation && educationInside) || (hasLeisure && leisureInside)
								|| (hasWork && workInside)|| (hasNon && nonInside)) {
							insideActivity = true;
						}
						
						if ((hasWork && !workInside) || (hasEducation && !educationInside) || (hasLeisure && !leisureInside)
								|| (hasWork && !workInside)|| (hasNon && !nonInside)) {
							outsideActivity = true;
						}
						
						
						if (outsideActivity && insideActivity)
							group1++;
						else if (!outsideActivity && insideActivity)
							group2++;
						else if (outsideActivity && !insideActivity)
							group3++;
						else if (!outsideActivity && !insideActivity && homeInside)
							group4++;
						else
							group5++;
						}
						
					}
				
			}
			}
			
		}
	    	System.out.println(numberInside);
			System.out.println(group1);	
			System.out.println(group2);
			System.out.println(group3);
			System.out.println(group4);
	        System.out.println(group5);
	       
	       // ShapeFileWriter.writeGeometries(features, "C:/Users/balacm/Desktop/Retailers_10pc/DemandForFacility.shp");
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		AnalyseShoppingBehaviour cp = new AnalyseShoppingBehaviour();
		
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath, args[3]);
	}

}
