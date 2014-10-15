package playground.balac.utils;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class FreeLinksSHP {

    public static void main(String[] args) throws Exception {
   
		final BufferedReader readLinkRetailers = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/test.txt");

        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("C:/Users/balacm/Desktop/Retailers_10pc/network.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
             
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system

        
        ArrayList<Id<Link>> a = new ArrayList<>();
        
        
        	
        	String s = readLinkRetailers.readLine();
			String[] arr = s.split("\\s");
			for(int i = 0; i < arr.length; i++) {
				a.add(Id.create(arr[i], Link.class));
        }
        
        Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
        PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
                setCrs(crs).
                setName("link").
                addAttribute("ID", String.class).
                addAttribute("fromID", String.class).
                addAttribute("toID", String.class).
                addAttribute("length", Double.class).
                addAttribute("type", String.class).
                addAttribute("capacity", Double.class).
                addAttribute("freespeed", Double.class).
                create();

        for (Link link : network.getLinks().values()) {
        	if (a.contains(link.getId())) {
        		Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
        		Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
        		Coordinate linkCoordinate = new Coordinate(link.getCoord().getX(), link.getCoord().getY());
        		SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
                    new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), link.getLength(), ((LinkImpl)link).getType(), link.getCapacity(), link.getFreespeed()}, null);
        		features.add(ft);
        	}
        }   
        ShapeFileWriter.writeGeometries(features, "C:/Users/balacm/Desktop/SHP_files/test_carsharing_4.shp");
        
      
    }
}