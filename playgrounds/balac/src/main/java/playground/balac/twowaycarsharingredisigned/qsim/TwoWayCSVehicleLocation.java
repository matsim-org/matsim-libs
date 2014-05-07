package playground.balac.twowaycarsharingredisigned.qsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.jfree.util.Log;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.carsharing.preprocess.membership.MyLinkUtils;
import playground.balac.onewaycarsharingredisgned.qsim.OneWayCarsharingRDStation;


public class TwoWayCSVehicleLocation {

	
	private QuadTree<TwoWayCSStation> vehicleLocationQuadTree;	
	
	public TwoWayCSVehicleLocation(String inputFilePath, Controler controler) throws IOException {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

	    for (Link l : controler.getNetwork().getLinks().values()) {
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
	    
	    while(s != null) {
	    	
	    	String[] arr = s.split("\t", -1);
	    
	    	CoordImpl coordStart = new CoordImpl(arr[2], arr[3]);
			Link l = MyLinkUtils.getClosestLink(controler.getNetwork(), coordStart);
	    	
	    	//Link l = controler.getNetwork().getLinks().get(new IdImpl(arr[0]));
	    	
			TwoWayCSStation f = new TwoWayCSStation(l, Integer.parseInt(arr[6]));
	    	
	    	vehicleLocationQuadTree.put(l.getCoord().getX(), l.getCoord().getY(), f);
	    	s = reader.readLine();
	    	
	    }	    
	    
	   
	  }
	public TwoWayCSVehicleLocation(Controler controler, ArrayList<TwoWayCSStation> stations) throws IOException {
	    double minx = (1.0D / 0.0D);
	    double miny = (1.0D / 0.0D);
	    double maxx = (-1.0D / 0.0D);
	    double maxy = (-1.0D / 0.0D);

	    for (Link l : controler.getNetwork().getLinks().values()) {
	      if (l.getCoord().getX() < minx) minx = l.getCoord().getX();
	      if (l.getCoord().getY() < miny) miny = l.getCoord().getY();
	      if (l.getCoord().getX() > maxx) maxx = l.getCoord().getX();
	      if (l.getCoord().getY() <= maxy) continue; maxy = l.getCoord().getY();
	    }
	    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;

	    vehicleLocationQuadTree = new QuadTree<TwoWayCSStation>(minx, miny, maxx, maxy);
	    
	    
	    for(TwoWayCSStation f: stations) {  
	    	
	    	vehicleLocationQuadTree.put(f.getLink().getCoord().getX(), f.getLink().getCoord().getY(), f);
	    }
	   
	  }
	public QuadTree<TwoWayCSStation> getQuadTree() {
		
		return vehicleLocationQuadTree;
	}
	
	public void addVehicle(Link link) {
		
		TwoWayCSStation f = vehicleLocationQuadTree.get(link.getCoord().getX(), link.getCoord().getY());
		
		if (f == null || !f.getLink().getId().toString().equals(link.getId().toString())) {
			
			TwoWayCSStation fNew = new TwoWayCSStation(link,  1);		
			
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
			
		}
		else {
			
			TwoWayCSStation fNew = new TwoWayCSStation(link, f.getNumberOfVehicles() + 1);		
			vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), f);
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
		}
		
		
	}
	
	public void removeVehicle(Link link) {
		
		TwoWayCSStation f = vehicleLocationQuadTree.get(link.getCoord().getX(), link.getCoord().getY());
		
		if ( f.getLink().getId().toString().equals(link.getId().toString())) {
			
			TwoWayCSStation fNew = new TwoWayCSStation(link, f.getNumberOfVehicles() - 1);	
			
						
			vehicleLocationQuadTree.remove(link.getCoord().getX(), link.getCoord().getY(), f);
			vehicleLocationQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), fNew);
			
			
		}
		else {
			
			Log.error("trying to take a car from the station with no cars, this should never happen");
			
		}
		
		
	}
	
	
}
