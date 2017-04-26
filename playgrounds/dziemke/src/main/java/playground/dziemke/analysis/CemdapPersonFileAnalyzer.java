package playground.dziemke.analysis;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.dziemke.utils.ShapeReader;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dziemke
 * 
 */
public class CemdapPersonFileAnalyzer {
	public static void main(String[] args) {
	    // Parameters
		boolean onlyInterior = false; // int
		boolean onlyBerlinBased = true; // ber	usually varied for analysis
		boolean distanceFilter = true; // dist	usually varied for analysis
//		double minDistance = 0;
		double maxDistance = 100;
		Integer planningAreaId = 11000000;
		
//		Integer minAge = 66;
		Integer maxAge = 65;	
		
//		String runId = "run_145";
//		String usedIteration = "150"; // most frequently used value: 150 
		String runId = "run_184";
		String usedIteration = "300"; // most frequently used value: 150 

	    
	    // Input files
	    String networkFile = "D:/Workspace/shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
//	    String eventsFile = "D:/Workspace/data/cemdapMatsimCadyts/output/" + runId + "/ITERS/it." + usedIteration + "/" 
	    String eventsFile = "D:/Workspace/runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + "/" 
				+ runId + "." + usedIteration + ".events.xml.gz";
//	    String cemdapPersonFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap_berlin/19/persons1.dat";
	    String cemdapPersonFile = "D:/Workspace/data/cemdapMatsimCadyts/input/cemdap_berlin/18/persons1.dat";
	    
	    String shapeFileBerlin = "D:/Workspace/data/cemdapMatsimCadyts/input/shapefiles/Berlin_DHDN_GK4.shp";
	    Map<Integer, Geometry> zoneGeometries = ShapeReader.read(shapeFileBerlin, "NR");
	    Geometry berlinGeometry = zoneGeometries.get(planningAreaId);

		
		// Create an EventsManager instance (MATSim infrastructure)
	    EventsManager eventsManager = EventsUtils.createEventsManager();
	    TripHandler handler = new TripHandler();
	    eventsManager.addHandler(handler);
	 
	    // Connect a file reader to the EventsManager and read in the event file
	    MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
	    reader.readFile(eventsFile);
	    System.out.println("Events file read!");
	    
	    // check if all trips have been completed; if so, result will be zero
	    int numberOfIncompleteTrips = 0;
	    for (Trip trip : handler.getTrips().values()) {
	    	if(!trip.getTripComplete()) { numberOfIncompleteTrips++; }
	    }
	    System.out.println(numberOfIncompleteTrips + " trips are incomplete.");
	    
	    
	    // parse person file
	 	CemdapPersonInputFileReader cemdapPersonFileReader = new CemdapPersonInputFileReader();
	 	cemdapPersonFileReader.parse(cemdapPersonFile);
	 	
	    	    	    
	    // get network, which is needed to calculate distances
	    Config config = ConfigUtils.createConfig();
	    Scenario scenario = ScenarioUtils.createScenario(config);
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
	    networkReader.readFile(networkFile);
	    Network network = scenario.getNetwork();
	    
	    int counter = 0;
	 
	    
	    // do calculations
	    for (Trip trip : handler.getTrips().values()) {
	    	if(trip.getTripComplete()) {
	    		boolean considerTrip = false;
	    		
	    		
	    		// get coordinates of links
	    		Id<Link> departureLinkId = trip.getDepartureLinkId();
	    		Id<Link> arrivalLinkId = trip.getArrivalLinkId();
	    		
	    		Link departureLink = network.getLinks().get(departureLinkId);
	    		Link arrivalLink = network.getLinks().get(arrivalLinkId);
	    		
	    		double arrivalCoordX = arrivalLink.getCoord().getX();
	    		double arrivalCoordY = arrivalLink.getCoord().getY();
	    		double departureCoordX = departureLink.getCoord().getX();
	    		double departureCoordY = departureLink.getCoord().getY();
	    		
	    		
	    		// calculate (beeline) distance
	    		double horizontalDistanceInMeter = (Math.abs(departureCoordX - arrivalCoordX)) / 1000;
	    		double verticalDistanceInMeter = (Math.abs(departureCoordY - arrivalCoordY)) / 1000;
	    		
	    		double tripDistanceBeeline = Math.sqrt(horizontalDistanceInMeter * horizontalDistanceInMeter 
	    				+ verticalDistanceInMeter * verticalDistanceInMeter);
	    		
	    		
	    		// create points
	    		Point arrivalLocation = MGC.xy2Point(arrivalCoordX, arrivalCoordY);
    			Point departureLocation = MGC.xy2Point(departureCoordX, departureCoordY);
	    		
    			
	    		// choose if trip will be considered
	    		if (onlyInterior == true) {
	    			if (berlinGeometry.contains(arrivalLocation) && berlinGeometry.contains(departureLocation)) {
	    				considerTrip = true;
	    			}
	    		} else if (onlyBerlinBased == true) {
	    			if (berlinGeometry.contains(arrivalLocation) || berlinGeometry.contains(departureLocation)) {
	    				considerTrip = true;
	    			}
	    		} else {
	    			considerTrip = true;
	    		}
	    		
	    		if (distanceFilter == true && tripDistanceBeeline >= maxDistance) {
	    			considerTrip = false;
	    		}
	    		
//	    		if (distanceFilter == true && tripDistanceBeeline <= minDistance) {
//	    			considerTrip = false;
//	    		}
	    		
	    		
	    		//--------------------------------------------------------------------------------------------------------------------
	    		
	    		// person-specific attributes
				String personId = trip.getPersonId().toString();
				int age = (int) cemdapPersonFileReader.getPersonAttributes().getAttribute(personId, "age");
				    
//				if (age < minAge) {
//					considerTrip = false;
//				}
				
//				if (age > maxAge) {
//					considerTrip = false;
//				}
				
				int employed = (int) cemdapPersonFileReader.getPersonAttributes().getAttribute(personId, "employed");
				if (employed == 1) { // can be varied
					considerTrip = false;
				}
				
				
				// (further) trip-specific attributes
				boolean doesWorkTrip = false;
	    		if (trip.getActivityTypeBeforeTrip().equals("work")) {
	    			doesWorkTrip = true;	    			
	    		}
	    		
				if (doesWorkTrip == true) { // can be varied
	    			considerTrip = false;
	    		}
				
				//--------------------------------------------------------------------------------------------------------------------
	    		
				
				// counter
	    		if (considerTrip == true) {
	    			counter++;
	    		}
	    	}
	    }
	    
	    System.out.println("Counter = " + counter);
	}	
}