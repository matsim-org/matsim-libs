package playground.balac.utils;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class CreateFacilitiesSHP {

	

    public static void main(String[] args) throws Exception {
   
    	int numberOfFirstRetailer = 29;
    	int numberOfSecondRetailer = 17;
    	int numberOfIterations = 1;
    	
		final BufferedReader readLinkRetailers = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary_ret");

		final BufferedReader readLinkNoRetailers = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/RetailersSummary_noret");

    	HashMap<IdImpl, Integer> beforeMove = new HashMap<IdImpl, Integer>();
    	HashMap<IdImpl, Integer> afterMove = new HashMap<IdImpl, Integer>();
    	HashMap<IdImpl, Boolean> moved = new HashMap<IdImpl, Boolean>();
		
    	for(int j = 1; j <= numberOfIterations; j++) {
			
			for(int i = 0; i < numberOfFirstRetailer; i++) {
			
				String s = readLinkRetailers.readLine();
				String[] arr = s.split("\t");
			
				afterMove.put(new IdImpl(arr[1]), Integer.parseInt(arr[5]));
				CoordImpl coord1 = new CoordImpl(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
				s = readLinkNoRetailers.readLine();
				arr = s.split("\t");
			
				beforeMove.put(new IdImpl(arr[1]), Integer.parseInt(arr[5]));
				CoordImpl coord2 = new CoordImpl(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
				if (coord1.getX() == coord2.getX() && coord1.getY() == coord2.getY()) {
					
					moved.put(new IdImpl(arr[1]), false);
					
				}
				else
					moved.put(new IdImpl(arr[1]), true);
			}
			for(int i = 0; i < numberOfSecondRetailer; i++) {
			
				String s = readLinkRetailers.readLine();
				String[] arr = s.split("\t");
			
				afterMove.put(new IdImpl(arr[1]), Integer.parseInt(arr[5]));
				CoordImpl coord1 = new CoordImpl(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));

				s = readLinkNoRetailers.readLine();
				arr = s.split("\t");
			
				beforeMove.put(new IdImpl(arr[1]), Integer.parseInt(arr[5]));
				CoordImpl coord2 = new CoordImpl(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
				if (coord1.getX() == coord2.getX() && coord1.getY() == coord2.getY()) {
					
					moved.put(new IdImpl(arr[1]), false);
					
				}
				else
					moved.put(new IdImpl(arr[1]), true);
			}
    	}
    	
    	
        Config config = ConfigUtils.createConfig();
        config.facilities().setInputFile("C:/Users/balacm/Desktop/facilities_retailers_more_pop.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
        ActivityFacilities f = scenario.getActivityFacilities();     
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system

        Collection featuresMovedIncrease = new ArrayList();
        Collection featuresMovedDecrease = new ArrayList();
        Collection featuresNotMovedIncrease = new ArrayList();
        Collection featuresNotMovedDecrease = new ArrayList();
        featuresMovedIncrease = new ArrayList();
        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
                setCrs(crs).
                setName("nodes").
                addAttribute("ID", String.class).
                //addAttribute("Be Af Mo", String.class).
                
                create();
        

        for (ActivityFacility f1:f.getFacilities().values()) {
            //SimpleFeature ft = nodeFactory.createPoint(f1.getCoord(), new Object[] {f1.getId().toString(), Integer.toString(beforeMove.get(f1.getId())) + "  " + Integer.toString(afterMove.get(f1.getId())) + "   " + Boolean.toString(moved.get(f1.getId()))}, null);
        	//SimpleFeature ft = nodeFactory.createPoint(f1.getCoord(), new Object[] {f1.getId().toString() }, null);
        	if (moved.get(f1.getId()) == true) {
        		if (afterMove.get(f1.getId()) >= beforeMove.get(f1.getId())) {
        			SimpleFeature ft = nodeFactory.createPoint(f1.getCoord(), new Object[] {Integer.toString(beforeMove.get(f1.getId())) + "  " + Integer.toString(afterMove.get(f1.getId())) }, null);
            	
        			featuresMovedIncrease.add(ft);
        		}
        		else {
        			SimpleFeature ft = nodeFactory.createPoint(f1.getCoord(), new Object[] {Integer.toString(beforeMove.get(f1.getId())) + "  " + Integer.toString(afterMove.get(f1.getId())) }, null);
                	
        			featuresMovedDecrease.add(ft);
        			
        		}
        	}
        	else {
        		
        		if (afterMove.get(f1.getId()) >= beforeMove.get(f1.getId())) {
        			SimpleFeature ft = nodeFactory.createPoint(f1.getCoord(), new Object[] {Integer.toString(beforeMove.get(f1.getId())) + "  " + Integer.toString(afterMove.get(f1.getId())) }, null);
            	
        			featuresNotMovedIncrease.add(ft);
        		}
        		else {
        			SimpleFeature ft = nodeFactory.createPoint(f1.getCoord(), new Object[] {Integer.toString(beforeMove.get(f1.getId())) + "  " + Integer.toString(afterMove.get(f1.getId())) }, null);
                	
        			featuresNotMovedDecrease.add(ft);
        			
        		}
        		
        		
        	}
        }
        
       
        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/Moved_Increase_nosub_more_pop.shp");
        ShapeFileWriter.writeGeometries(featuresMovedDecrease, "C:/Users/balacm/Desktop/SHP_files/Moved_Decrease_nosub_more_pop.shp");
        ShapeFileWriter.writeGeometries(featuresNotMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/NotMoved_Increase_nosub_more_pop.shp");
        ShapeFileWriter.writeGeometries(featuresNotMovedDecrease, "C:/Users/balacm/Desktop/SHP_files/NotMoved_Decrease_nosub_more_pop.shp");
    }
	
}
