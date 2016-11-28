package playground.balac.contribs.carsharing.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoCH1903LV03;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

public class VehiclePath implements LinkEnterEventHandler, PersonLeavesVehicleEventHandler {

	ArrayList<Id<Link>> links = new ArrayList<>(); 
	Map<Integer, ArrayList<Id<Link>>> map = new HashMap<>();
	int key = 0;
	
	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {

		if (event.getVehicleId().toString().equals("FF_992"))
			links.add(event.getLinkId());
	}	
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {

		if (event.getVehicleId().toString().equals("FF_992")) {
			map.put(key, links);
			key++;
			links = new ArrayList<>(); 
		}
	}
	
	public ArrayList<Id<Link>> getLinks() {
		return links;
	}

	public Map<Integer, ArrayList<Id<Link>>> getMap() {
		return map;
	}

	public static void main(String[] args) {

		EventsManager eventsManager = EventsUtils.createEventsManager();
		
		VehiclePath handler = new VehiclePath();
		eventsManager.addHandler(handler);
		EventsReaderXMLv1 eventsReader =  new EventsReaderXMLv1(eventsManager);
		
		eventsReader.readFile(args[0]);
		
		Config config = ConfigUtils.createConfig();
        config.network().setInputFile(args[1]);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Network network = scenario.getNetwork();	
        
        Map<Integer, ArrayList<Id<Link>>> map = handler.getMap();
        int i = 1;
        for (ArrayList<Id<Link>> links : map.values()) {
        
	        CoordinateReferenceSystem crs = MGC.getCRS("EPSG:26914");    // EPSG Code for Swiss CH1903_LV03 coordinate system
	        
	        Collection featuresLink = new ArrayList();
	
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
	
	       // WGS84toCH1903LV03 transformation = new WGS84toCH1903LV03();
	
	        for (Id<Link> id : links) {
	        	
	        	Link link = network.getLinks().get(id);
	        	Coord coordLink = link.getCoord();
	        	//Coord coordLinkT = transformation.transform(coordLink);
		
			    Coordinate fromNodeCoordinate = new Coordinate(link.getFromNode().getCoord().getX(), (link.getFromNode().getCoord()).getY());
	            Coordinate toNodeCoordinate = new Coordinate(link.getToNode().getCoord().getX(), (link.getToNode().getCoord()).getY());
	            Coordinate linkCoordinate = new Coordinate(coordLink.getX(), coordLink.getY());
	            SimpleFeature ft = linkFactory.createPolyline(new Coordinate [] {fromNodeCoordinate, linkCoordinate, toNodeCoordinate},
	                    new Object [] {link.getId().toString(), link.getFromNode().getId().toString(),link.getToNode().getId().toString(), 
	                    		link.getLength(), link.getCapacity(), link.getFreespeed(), link.getAllowedModes().toString()}, null);
	            featuresLink.add(ft);
	        }   
	        
	        ShapeFileWriter.writeGeometries(featuresLink, "C:/Users/balacm/Desktop/path" + i + ".shp");
	        i++;
        }

	}

	
}
