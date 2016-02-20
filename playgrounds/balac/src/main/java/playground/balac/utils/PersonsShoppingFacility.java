package playground.balac.utils;

import java.util.ArrayList;
import java.util.Collection;

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
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


public class PersonsShoppingFacility {
	
	public void run(String plansFilePath, String networkFilePath, String facilitiesfilePath ) {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReader populationReader = new MatsimPopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(networkFilePath);
		new FacilitiesReaderMatsimV1(scenario).readFile(facilitiesfilePath);
		populationReader.readFile(plansFilePath);
		ArrayList<Coord> a = new ArrayList<Coord>();
		int grocery = 0;
		double centerX = 683217.0; 
	    double centerY = 247300.0;
		Coord coord = new Coord(centerX, centerY);
		int previousIn = 0;
		int previousOut = 0;
	    for(Person p:scenario.getPopulation().getPersons().values()) {
			
			Plan plan = p.getSelectedPlan();
			
			for (PlanElement pe: plan.getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (((Activity) pe).getType().equals("shopgrocery")) {
						
						if ( (CoordUtils.calcEuclideanDistance(((Activity)pe).getCoord(), coord) < 4000)) {
							
							grocery++;
							int index = plan.getPlanElements().indexOf(pe);
							if ((CoordUtils.calcEuclideanDistance(((Activity)plan.getPlanElements().get(index - 2)).getCoord(), coord) < 4000)) {
								previousIn++;
							}
							else
								previousOut++;
							
						}
						
					}
					
				}
				
			}
		}
			System.out.println(grocery);	
			System.out.println(previousIn);
			System.out.println(previousOut);
		 CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system

	        Collection<SimpleFeature> features = new ArrayList();
	       
	     
	        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
	                setCrs(crs).
	                setName("nodes").
	                addAttribute("Id", String.class).
	                //addAttribute("Be Af Mo", String.class).
	                
	                create();
	        

	        for (Coord c:a) {
	            //SimpleFeature ft = nodeFactory.createPoint(f1.getCoord(), new Object[] {f1.getId().toString(), Integer.toString(beforeMove.get(f1.getId())) + "  " + Integer.toString(afterMove.get(f1.getId())) + "   " + Boolean.toString(moved.get(f1.getId()))}, null);
	        	SimpleFeature ft = nodeFactory.createPoint(c, new Object[] {"1"}, null);
	        	features.add(ft);

	        	
	        }
	        
	       
	       // ShapeFileWriter.writeGeometries(features, "C:/Users/balacm/Desktop/Retailers_10pc/DemandForFacility.shp");
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PersonsShoppingFacility cp = new PersonsShoppingFacility();
		
		String plansFilePath = args[0]; 
		String networkFilePath = args[1];
		String facilitiesfilePath = args[2];
		
		cp.run(plansFilePath, networkFilePath,facilitiesfilePath);
	}

}
