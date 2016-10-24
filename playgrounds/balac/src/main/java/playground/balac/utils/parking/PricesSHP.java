package playground.balac.utils.parking;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class PricesSHP {

	public static void main(String[] args) throws IOException {
		final BufferedReader readLinkRetailers = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/prices1hNew.txt");

      
             
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system

        
        Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
                setCrs(crs).
                setName("node").
                addAttribute("ID", String.class).
                addAttribute("type", Double.class).
                addAttribute("occupancy", Double.class).
                create();
        int i = 0;
        String s = readLinkRetailers.readLine();
        	while(s != null) {
        		
        		String[] arr = s.split(";");
        		if (arr[0].startsWith("gp")) {
	        		Coord coord = CoordUtils.createCoord(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
	    			SimpleFeature ft;
	
	        		
	        			ft = nodeFactory.createPoint(coord, new Object[] {Integer.toString(i),  "garage", arr[40]}, null);
	        		
	            	
	        		features.add(ft);
	        		i++;
        		}
        		s = readLinkRetailers.readLine();
        	}
         
        ShapeFileWriter.writeGeometries(features, "C:/Users/balacm/Documents/doc/presentations/ParkingSeminar/balacm/QGIS/SHP/pricesGarageHourly11h.shp");

	}

}
