package playground.balac.utils;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CreateFacilitiesSimpleSHP {
	

    public static void main(String[] args) throws Exception {
           	
        Config config = ConfigUtils.createConfig();
        config.facilities().setInputFile("C:/Users/balacm/Desktop/EIRASS_Paper/10pc_input_files/facilities_land_price.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
       // Config config1 = ConfigUtils.createConfig();
      //  config1.facilities().setInputFile("C:/Users/balacm/Desktop/facilities_noretailers_extracted_20pc_original.xml");
       // Scenario scenario1 = ScenarioUtils.loadScenario(config1);
        
        Network network = scenario.getNetwork();
        ActivityFacilities f = scenario.getActivityFacilities();     
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system
        int numberOfFirstRetailer = 28;
    	int numberOfSecondRetailer = 18;
    	int numberOfIterations = 1;
    	
		final BufferedReader readLinkRetailers = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Temporary_files/RetailersSummary");

		String s = readLinkRetailers.readLine();
    	HashMap<Id<ActivityFacility>, Integer> afterMove = new HashMap<>();
		
    	for(int j = 1; j <= numberOfIterations; j++) {
			
			for(int i = 0; i < numberOfFirstRetailer; i++) {
			
				s = readLinkRetailers.readLine();
				String[] arr = s.split("\t");
				//if (!scenario1.getActivityFacilities().getFacilities().containsKey(Id.create(arr[1])))
				afterMove.put(Id.create(arr[1], ActivityFacility.class), Integer.parseInt(arr[5]));
				
			}
			for(int i = 0; i < numberOfSecondRetailer; i++) {
			
				s = readLinkRetailers.readLine();
				String[] arr = s.split("\t");
				//if (!scenario1.getActivityFacilities().getFacilities().containsKey(Id.create(arr[1])))
				afterMove.put(Id.create(arr[1], ActivityFacility.class), Integer.parseInt(arr[5]));
				
			}
    	}
        Collection<SimpleFeature> featuresMovedIncrease = new ArrayList<>();
        featuresMovedIncrease = new ArrayList<SimpleFeature>();
        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
                setCrs(crs).
                setName("nodes").
                addAttribute("ID", String.class).
               //addAttribute("Customers", Integer.class).
                //addAttribute("Be Af Mo", String.class).
                
                create();
        
        double centerX = 683217.0; 
    	double centerY = 247300.0;	
        for (ActivityFacility f1:f.getFacilities().values()) {
            		//need to extract the number of customers for each facility
        	//if (Math.sqrt(Math.pow(f1.getCoord().getX() - centerX, 2) + (Math.pow(f1.getCoord().getY() - centerY, 2))) < 5000) {
				
        			SimpleFeature ft = nodeFactory.createPoint(f1.getCoord(), new Object[] {f1.getId().toString()}, null);
        			//if (!scenario1.getActivityFacilities().getFacilities().containsKey(f1.getId()))
        			featuresMovedIncrease.add(ft);
        	//}
        		
        		
        	}
        
        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/facilities_land_price.shp");

        }
        
       
    }
	

