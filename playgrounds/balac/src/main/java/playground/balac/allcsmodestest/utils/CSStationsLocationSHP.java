package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.StringUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CSStationsLocationSHP {

	public static void main(String[] args) throws NumberFormatException, IOException {
		// TODO Auto-generated method stub

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[0]);
	    String line;
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system

		BufferedReader reader = IOUtils.getBufferedReader(args[1]);

		 Collection<SimpleFeature> featuresMovedIncrease = new ArrayList<SimpleFeature>();
	        featuresMovedIncrease = new ArrayList<SimpleFeature>();
	        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
	                setCrs(crs).
	                setName("nodes").
	                addAttribute("ID", String.class).
	               //addAttribute("Customers", Integer.class).
	                //addAttribute("Be Af Mo", String.class).
	                
	                create();
		
		int i = 0;
		reader.readLine();
		while ((line = reader.readLine()) != null) {
		      String[] parts = StringUtils.explode(line, '\t');
			Coord coord = new Coord(Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
		      
		      SimpleFeature ft = nodeFactory.createPoint(coord, new Object[] {Integer.toString(i)}, null);
  			//if (!scenario1.getActivityFacilities().getFacilities().containsKey(f1.getId()))
  			featuresMovedIncrease.add(ft);
  			i++;
	    	
	    }
		
        ShapeFileWriter.writeGeometries(featuresMovedIncrease, "C:/Users/balacm/Desktop/SHP_files/zurich_zone_10km_area.shp");

		
	}

}
