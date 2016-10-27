package playground.balac.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoCH1903LV03;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class CreateNetworkSHP {
	
	

    public static void main(String[] args) throws Exception {
   
    	double centerX = 683217.0; 
    	double centerY = 247300.0;	    	
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile("file:///C:/Users/balacm/Documents/Networks/zh/mmNetwork.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();
             
        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:21781");    // EPSG Code for Swiss CH1903_LV03 coordinate system

        
        
        
        Collection featuresLink = new ArrayList();
        Collection featuresNode = new ArrayList();

        PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder().
                setCrs(crs).
                setName("link").
                addAttribute("ID", String.class).
                addAttribute("fromID", String.class).
                addAttribute("toID", String.class).
                addAttribute("length", Double.class).
                addAttribute("capacity", Double.class).
                addAttribute("freespeed", Double.class).
                addAttribute("modes", String.class).

                create();
        
        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
                setCrs(crs).
                setName("node").
                addAttribute("ID", String.class).
               // addAttribute("type", String.class).
              //  addAttribute("capacity", Double.class).
              //  addAttribute("freespeed", Double.class).
                create();
        CH1903LV03PlustoCH1903LV03 transformation = new CH1903LV03PlustoCH1903LV03();

        for (Link link : network.getLinks().values()) {
        	//if (link.getAllowedModes().contains("car")){
        		
        		Coord coordLink = link.getCoord();
        		Coord coordLinkT = transformation.transform(coordLink);
			//	if(Math.sqrt(Math.pow(coordLinkT.getX() - centerX, 2) +(Math.pow(coordLinkT.getY() - centerY, 2))) < 31000) {
	
		            Coordinate fromNodeCoordinate = new Coordinate(transformation.transform(link.getFromNode().getCoord()).getX(), transformation.transform(link.getFromNode().getCoord()).getY());
		            Coordinate toNodeCoordinate = new Coordinate(transformation.transform(link.getToNode().getCoord()).getX(), transformation.transform(link.getToNode().getCoord()).getY());
		            Coordinate linkCoordinate = new Coordinate(coordLinkT.getX(), coordLinkT.getY());
		            SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
		                    new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), 
		                    		link.getLength(), link.getCapacity(), link.getFreespeed(), link.getAllowedModes().toString()}, null);
		            featuresLink.add(ft);
	            
				//}
        	//}
        }   
        
        for (Node node : network.getNodes().values()) {
        	
        	Coord coordNode = node.getCoord();
    		Coord coordLinkT = transformation.transform(coordNode);

        	SimpleFeature ft = nodeFactory.createPoint(new Coordinate(coordLinkT.getX(), coordLinkT.getY()), new Object [] {node.getId().toString()},null);
        	
            featuresNode.add(ft);
        }
        ShapeFileWriter.writeGeometries(featuresLink, "C:/Users/balacm/Documents/Networks/zh/links.shp");
        ShapeFileWriter.writeGeometries(featuresNode, "C:/Users/balacm/Documents/Networks/zh/nodes.shp");

      /*  features = new ArrayList();
        PointFeatureFactory nodeFactory = new PointFeatureFactory.Builder().
                setCrs(crs).
                setName("nodes").
                addAttribute("ID", String.class).
                create();

        for (Node node : network.getNodes().values()) {
            SimpleFeature ft = nodeFactory.createPoint(node.getCoord(), new Object[] {node.getId().toString()}, null);
            features.add(ft);
        }
        ShapeFileWriter.writeGeometries(features, "C:/Users/balacm/Desktop/Emissions/network_nodes.shp");*/
    }
}