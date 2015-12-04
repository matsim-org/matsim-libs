package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RentalLocationsSHSP {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
	        
	        
	        Network network = scenario.getNetwork();
	        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system
	       
	    	
			final BufferedReader readLink = IOUtils.getBufferedReader(args[1]);
			
			
			 Collection<SimpleFeature> featuresMovedIncrease = new ArrayList<>();
		        featuresMovedIncrease = new ArrayList<SimpleFeature>();
		        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
		                setCrs(crs).
		                setName("nodes").
		                addAttribute("ID", String.class).
		               //addAttribute("Customers", Integer.class).
		                //addAttribute("Be Af Mo", String.class).
		                
		                create();
		        
		        Collection<SimpleFeature> featuresMovedIncrease2 = new ArrayList<>();
		        featuresMovedIncrease2 = new ArrayList<SimpleFeature>();
		        PointFeatureFactory nodeFactory2 = new PointFeatureFactory.Builder().
		                setCrs(crs).
		                setName("nodes").
		                addAttribute("ID", String.class).
		               //addAttribute("Customers", Integer.class).
		                //addAttribute("Be Af Mo", String.class).
		                
		                create();
			String s = readLink.readLine();
			s = readLink.readLine();
			
			int i = 0;
			while (s != null) {
				i++;
				String[] arr = s.split("\t");
				
				if (arr[1].contains("c")) {
				
				SimpleFeature ft = nodeFactory.createPoint(network.getLinks().get(Id.create(arr[4], Link.class)).getCoord(), new Object[] {Integer.toString(i)}, null);
    			featuresMovedIncrease.add(ft);
				}
				else {
					SimpleFeature ft = nodeFactory2.createPoint(network.getLinks().get(Id.create(arr[4], Link.class)).getCoord(), new Object[] {Integer.toString(i)}, null);
	    			featuresMovedIncrease2.add(ft);
				}
    			s = readLink.readLine();
			}
	        
	        
	        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/rentals_location_ff_normal_price_rb.shp");
	        ShapeFileWriter.writeGeometries(featuresMovedIncrease2, "C:/Users/balacm/Desktop/SHP_files/rentals_location_ff_normal_price_ff.shp");

	}

}
