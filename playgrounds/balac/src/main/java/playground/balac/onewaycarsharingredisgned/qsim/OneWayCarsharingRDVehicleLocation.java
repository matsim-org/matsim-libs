package playground.balac.onewaycarsharingredisgned.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;


public class OneWayCarsharingRDVehicleLocation {

	private final static Logger log = Logger.getLogger(OneWayCarsharingRDVehicleLocation.class);

	private QuadTree<OneWayCarsharingRDStation> vehicleLocationQuadTree;	
	
	public OneWayCarsharingRDVehicleLocation(String inputFilePath, Controler controler) throws IOException {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);
        LinkUtils linkUtils = new LinkUtils(controler.getScenario().getNetwork());
        for (Link l : controler.getScenario().getNetwork().getLinks().values()) {
	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    vehicleLocationQuadTree = new QuadTree<OneWayCarsharingRDStation>(minx, miny, maxx, maxy);
	    
	    BufferedReader reader = IOUtils.getBufferedReader(inputFilePath);
	    String s = reader.readLine();
	    s = reader.readLine();
	    int i = 1;
	    while(s != null) {
	    	
	    	String[] arr = s.split("\t", -1);
	    
	    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
			Link l = linkUtils.getClosestLink( coordStart);	    	
			ArrayList<String> vehIDs = new ArrayList<String>();
	    	
	    	for (int k = 0; k < Integer.parseInt(arr[6]); k++) {
	    		vehIDs.add(Integer.toString(i));
	    		i++;
	    	}
	    	OneWayCarsharingRDStation f = new OneWayCarsharingRDStation(l, Integer.parseInt(arr[6]), vehIDs);
	    	
	    	vehicleLocationQuadTree.put(l.getCoord().getX(), l.getCoord().getY(), f);
	    	s = reader.readLine();
	    	
	    }	    
	    
	   
	  }
	public OneWayCarsharingRDVehicleLocation(Controler controler, ArrayList<OneWayCarsharingRDStation> stations) throws IOException {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

        for (Link l : controler.getScenario().getNetwork().getLinks().values()) {
	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    vehicleLocationQuadTree = new QuadTree<OneWayCarsharingRDStation>(minx, miny, maxx, maxy);
	    
	    
	    for(OneWayCarsharingRDStation f: stations) {  
	    	
	    	vehicleLocationQuadTree.put(f.getLink().getCoord().getX(), f.getLink().getCoord().getY(), f);
	    }
	    
	  
	    
	   
	  }
	public QuadTree<OneWayCarsharingRDStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle(Link link, String id) {
		
		OneWayCarsharingRDStation f = vehicleLocationQuadTree.get(link.getCoord().getX(), link.getCoord().getY());
		
		if (f == null || !f.getLink().getId().toString().equals(link.getId().toString())) {
			log.error("Adding a onewaycarsharing vehicle to the link where there is no station! Continuing anyway.");
			
			ArrayList<String> vehIDs = new ArrayList<String>();
			
			vehIDs.add((id));
			
			OneWayCarsharingRDStation fNew = new OneWayCarsharingRDStation(link, 1, vehIDs);		
			
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
			
		}
		else {
			ArrayList<String> vehIDs = f.getIDs();
			ArrayList<String> newvehIDs = new ArrayList<String>();
			for (String s : vehIDs) {
				newvehIDs.add(s);
			}
			newvehIDs.add(id);
			OneWayCarsharingRDStation fNew = new OneWayCarsharingRDStation(link, f.getNumberOfVehicles() + 1, newvehIDs);		
			vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), f);
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
		}
		
		
	}
	
	public void removeVehicle(OneWayCarsharingRDStation station, String id) {
		
		
			ArrayList<String> vehIDs = station.getIDs();
			ArrayList<String> newvehIDs = new ArrayList<String>();
			for (String s : vehIDs) {
				newvehIDs.add(s);
			}
			newvehIDs.remove(id);
			OneWayCarsharingRDStation fNew = new OneWayCarsharingRDStation(station.getLink(), station.getNumberOfVehicles() - 1,newvehIDs);	
			
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
