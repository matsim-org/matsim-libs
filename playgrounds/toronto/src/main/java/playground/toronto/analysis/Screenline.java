package playground.toronto.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

/**
 * 
 * @author pkucirek
 *
 */
public class Screenline implements LinkEnterEventHandler, LinkLeaveEventHandler{

	//STATIC LOADERS
	
	/**
	 * 
	 * @param shapefile
	 * @return
	 */
	public static Map<Id, Screenline> openShapefile(String shapefile){
		HashMap<Id, Screenline> result = new HashMap<Id, Screenline>();
		
		return result;
	}
	
	/**
	 * 
	 * @param shapefile
	 * @param network
	 * @return
	 */
	public static Map<Id, Screenline> openShapefile(String shapefile, Network network){
		HashMap<Id, Screenline> result = new HashMap<Id, Screenline>();
		
		return result;
	}
	
	//Attributes
	private Id id;
	public String description;
	private String plusName;
	private String minusName;
	
	private List<Id> plusLinks;
	private List<Id> minusLinks;
	private LineString geom;
	private GeometryFactory factory;
	
	public Screenline(Id id, String plusName, String minusName){
		this.id = id;
		this.plusName = plusName;
		this.minusName = minusName;
		
	}

	public void loadLinksFromNetwork(Network network, boolean initializeLinks){
		
	}
	
	private void checkAddLink(Link link){
		
		Coordinate coord1 = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Coordinate coord2 = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		LineString gLink = factory.createLineString(new Coordinate[]{coord1, coord2});
		
		if (gLink.crosses(geom)){
			
		}
		
	}
	
	public List<Id> getPlusLinks(){
		return this.plusLinks;
	}
	
	public List<Id> getMinusLinks(){
		return this.minusLinks;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO Auto-generated method stub
		
	}
	
}
