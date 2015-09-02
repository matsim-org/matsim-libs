package playground.balac.twowaycarsharingredisigned.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

public class TwoWayCSVehicleLocation {

	
	private QuadTree<TwoWayCSStation> vehicleLocationQuadTree;	
	
	private static final Logger log = Logger.getLogger(TwoWayCSVehicleLocation.class);

	
	public TwoWayCSVehicleLocation(String inputFilePath, Scenario scenario) throws IOException {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);
        LinkUtils linkUtils = new LinkUtils(scenario.getNetwork());
        for (Link l : scenario.getNetwork().getLinks().values()) {
	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    vehicleLocationQuadTree = new QuadTree<TwoWayCSStation>(minx, miny, maxx, maxy);
	    
	    BufferedReader reader = IOUtils.getBufferedReader(inputFilePath);
	    String s = reader.readLine();
	    s = reader.readLine();
	    int i = 1;
	    while(s != null) {
	    	
	    	String[] arr = s.split("\t", -1);
	    
	    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
			Link l = linkUtils.getClosestLink(coordStart);
	    	
	    	//Link l = controler.getNetwork().getLinks().get(new IdImpl(arr[0]));
			ArrayList<String> vehIDs = new ArrayList<String>();
	    	
	    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
	    		vehIDs.add(Integer.toString(i));
	    		i++;
	    	}
			TwoWayCSStation f = new TwoWayCSStation(l, l.getCoord(), Integer.parseInt(arr[6]), vehIDs);
	    	
	    	vehicleLocationQuadTree.put(l.getCoord().getX(), l.getCoord().getY(), f);
	    	s = reader.readLine();
	    	
	    }	    
	    
	   
	  }
	public TwoWayCSVehicleLocation(Scenario scenario, ArrayList<TwoWayCSStation> stations) {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

        for (Link l : scenario.getNetwork().getLinks().values()) {
	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    vehicleLocationQuadTree = new QuadTree<TwoWayCSStation>(minx, miny, maxx, maxy);
	    
	    
	    for(TwoWayCSStation f: stations) {  
	    	if (vehicleLocationQuadTree.get(f.getLink().getCoord().getX(), f.getLink().getCoord().getY()) != null
	    			&& vehicleLocationQuadTree.get(f.getLink().getCoord().getX(), f.getLink().getCoord().getY()).getLink().getId().toString().equals(f.getLink().getId().toString())) {
	    		log.warn("Two carsharing stations were mapped to the same link" + f.getLink().getId().toString() +", consider merging these two stations before the simulation.");
	    		log.warn("These stations are on the coordinates: " + vehicleLocationQuadTree.get(f.getLink().getCoord().getX(), f.getLink().getCoord().getY()).getCoord().toString() + " and " + f.getCoord().toString());
	    	}

	    	vehicleLocationQuadTree.put(f.getLink().getCoord().getX(), f.getLink().getCoord().getY(), f);
	    		
	    		
	    }
	   
	  }
	public QuadTree<TwoWayCSStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle(Link link, String id) {
		
		TwoWayCSStation f = vehicleLocationQuadTree.get(link.getCoord().getX(), link.getCoord().getY());
		
		if (f == null || !f.getLink().getId().toString().equals(link.getId().toString())) {
			
			
			ArrayList<String> vehIDs = new ArrayList<String>();
			
			vehIDs.add(id);
			
			TwoWayCSStation fNew = new TwoWayCSStation(link, link.getCoord(), 1, vehIDs);		
			
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
			
		}
		else {
			ArrayList<String> vehIDs = f.getIDs();
			ArrayList<String> newvehIDs = new ArrayList<String>();
			for (String s : vehIDs) {
				newvehIDs.add(s);
			}
			newvehIDs.add(0, id);
			TwoWayCSStation fNew = new TwoWayCSStation(link, link.getCoord(), f.getNumberOfVehicles() + 1, newvehIDs);		
			vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), f);
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
		}
		
		
	}
	
	public void removeVehicle(TwoWayCSStation station, String id) {
		
		
			ArrayList<String> vehIDs = station.getIDs();
			ArrayList<String> newvehIDs = new ArrayList<String>();
			for (String s : vehIDs) {
				newvehIDs.add(s);
			}
			
			if (!newvehIDs.remove(id))
				throw new NullPointerException("Removing the vehicle did not wok");

			TwoWayCSStation fNew = new TwoWayCSStation(station.getLink(), station.getCoord(), station.getNumberOfVehicles() - 1, newvehIDs);	
			
						
			if (!vehicleLocationQuadTree.remove(station.getLink().getCoord().getX(), station.getLink().getCoord().getY(), station)) 
				throw new NullPointerException("Removing the station did not wok");
			vehicleLocationQuadTree.put(station.getLink().getCoord().getX(), station.getLink().getCoord().getY(), fNew);
			
		
	}
	
	private class LinkUtils {
		
		Network network;
		public LinkUtils(Network network) {
			
			this.network = network;		}
		
		public LinkImpl getClosestLink(Coord coord) {
			
			double distance = (1.0D / 0.0D);
		    Id<Link> closestLinkId = Id.create(0L, Link.class);
		    for (Link link : network.getLinks().values()) {
		      LinkImpl mylink = (LinkImpl)link;
		      Double newDistance = Double.valueOf(mylink.calcDistance(coord));
		      if (newDistance.doubleValue() < distance) {
		        distance = newDistance.doubleValue();
		        closestLinkId = link.getId();
		      }
		    }

		    return (LinkImpl)network.getLinks().get(closestLinkId);
			
		}
	}
	
	
}
