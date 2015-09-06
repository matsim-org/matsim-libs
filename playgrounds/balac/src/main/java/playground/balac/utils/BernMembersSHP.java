package playground.balac.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;



public class BernMembersSHP {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario);
		networkReader.readFile(args[0]);
	        
	        Network network = scenario.getNetwork();
	        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system
	       
	    	
			final BufferedReader readLink1 = IOUtils.getBufferedReader(args[1]);
			Collection featuresMovedIncrease = new ArrayList();
	        featuresMovedIncrease = new ArrayList<SimpleFeature>();
	        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
	                setCrs(crs).
	                setName("nodes").
	                addAttribute("ID", String.class).
	               //addAttribute("Customers", Integer.class).
	                //addAttribute("Be Af Mo", String.class).
	                
	                create();
	        
	      
		String s = readLink1.readLine();
		s = readLink1.readLine();
		int i = 0;
		while (s != null) {
			
			String[] arr = s.split("\\t");
			s = readLink1.readLine();
			Coord coord = new Coord(Double.parseDouble(arr[5]), Double.parseDouble(arr[4]));
			WGS84toCH1903LV03 b = new WGS84toCH1903LV03();  //transforming coord from WGS84 to CH1903LV03
			coord = b.transform(coord);
			SimpleFeature ft = nodeFactory.createPoint(coord, new Object[] {Integer.toString(i)}, null);
			featuresMovedIncrease.add(ft);		
		
			i++;
			
		}
		
        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/BernMore.shp");

	}

}
