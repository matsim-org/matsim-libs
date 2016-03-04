package playground.balac.twowaycarsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

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

public class CarsUsedLocationsffSHP {

	public static void main(String[] args) throws IOException {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
	        
		Set<String> a1 = new TreeSet<String>();
		Set<String> a2 = new TreeSet<String>();
	        Network network = scenario.getNetwork();
	        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system
	       
	    	
			final BufferedReader readLink2 = IOUtils.getBufferedReader(args[1]);

			

			HashMap<String, String> mapa = new HashMap<String, String>();

			
			 Collection<SimpleFeature> featuresMovedIncrease = new ArrayList<>();
		        featuresMovedIncrease = new ArrayList<SimpleFeature>();
		        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
		                setCrs(crs).
		                setName("nodes").
		                addAttribute("ID", String.class).
		               //addAttribute("Customers", Integer.class).
		                //addAttribute("Be Af Mo", String.class).
		                
		                create();
		        
		      
		String s;
			s = readLink2.readLine();
			s = readLink2.readLine();
			int i = 0;
			while( s != null) {
				String[] arr = s.split("\\s");
			
					
					SimpleFeature ft = nodeFactory.createPoint(network.getLinks().get(Id.create(arr[4], Link.class)).getCoord(), new Object[] {Integer.toString(i)}, null);
					featuresMovedIncrease.add(ft);
					
				
					i++;
					
				
				s = readLink2.readLine();
			}
	        
	        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/used_ff_low_1x.shp");
	     

	}

}
