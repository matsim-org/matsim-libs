package playground.toronto.analysis;

import java.util.ArrayList;
import java.util.Collection;
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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

/**
 * A screenline is a traffic count tool where several count stations along a pre-determined
 * boundary are aggregated into one - the border of a country, for example. This class
 * facilitates screenline analysis by loading in a screenline geometry and figuring out
 * which network links cross it (and in which direction).
 * 
 * <br><br>The screenline geometry is directional, in the order of its vertices. Therefore,
 * links crossing the screnline right-to-left are considered the <em>positive direction</em>,
 * while links crossing left-to-right are considered the <em>negative direction</em>. 
 * It is up to the screenline coder to assign useful names to each direction.
 * 
 * <br><br>[Insert information about handler behaviour]
 * 
 * @author pkucirek
 * @author aweiss
 */
public class Screenline{

	/////////////////////////////////////////////////////////////////////////////////////
	
	// Identifiers for shapefile attributes.
	public final static String ID = "Id";
	public final static String POSDIRNAME = "PosDirName";
	public final static String NEGDIRNAME = "NegDirName";
	public final static String DESCRNAME = "Descr";
	
	
	/**
	 * Loads a Map of {@link Screenline} (Id : Screenline) from a shapefile.
	 * @param shapefile - The address of a properly-formatted shapefile. Its attribute
	 * 		table must include columns labeled "Id", "PosDirName", "NegDirName", and
	 * 		"Descr".
	 * @return
	 */
	public static Map<Id, Screenline> openShapefile(String shapefile){
		HashMap<Id, Screenline> result = new HashMap<Id, Screenline>();
		
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = reader.getFeatureSet();
		
		for (SimpleFeature f : features){
			
			Id sid;
			try{
				sid = new IdImpl((Integer) f.getAttribute(Screenline.ID));
			}catch (ClassCastException c){
				sid = new IdImpl((String) f.getAttribute(Screenline.ID));
			}
				
			Screenline s = new Screenline(sid,
						(MultiLineString) f.getAttribute(0),
						(String) f.getAttribute(Screenline.POSDIRNAME),
						(String) f.getAttribute(Screenline.NEGDIRNAME));
			
			s.description = (String) f.getAttribute(Screenline.DESCRNAME);
			
			//Non-existant attributes will be returned as null. This is fine, except for the
			// geometry. But that should throw different errors.
			
			result.put(s.getID(), s);
		}
		
		return result;
	}
	
	/**
	 * Loads a Map of {@link Screenline} (Id : Screenline) from a shapefile, initializing
	 * each Screenline with links from the network.
	 * @param shapefile - The address of a properly-formatted shapefile. Its attribute
	 * 		table must include columns labeled "Id", "PosDirName", "NegDirName", and
	 * 		"Descr".
	 * @param network - The {@link Network} to load links from. 
	 * @return
	 */
	public static Map<Id, Screenline> openShapefile(String shapefile, Network network){
		Map<Id, Screenline> result = Screenline.openShapefile(shapefile);
		
		for (Screenline s : result.values()){
			s.loadLinksFromNetwork(network);
		}
		
		return result;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//Attributes
	private final Id id;
	public String description;
	public String plusName;
	public String minusName;
	
	private List<Id> plusLinks;
	private List<Id> minusLinks;
	//private LineString geom;
	private final MultiLineString geom2;
	private GeometryFactory factory;
	
	
	public Screenline(Id id, MultiLineString geometry, String plusName, String minusName){
		this.id = id;
		this.plusName = plusName;
		this.minusName = minusName;
		this.geom2 = geometry;
		
		this.plusLinks = new ArrayList<Id>();
		this.minusLinks = new ArrayList<Id>();
		
		this.factory = new GeometryFactory();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Loads the screenline's links from a given {@link Network}. By
	 * default, invoking this method will clear any stored link IDs.
	 * @param network The {@link Network} to load links from.
	 */
	public void loadLinksFromNetwork(Network network){
		this.loadLinksFromNetwork(network, true);
	}
	
	/**
	 * Loads the screenline's links from a given {@link Network}.
	 * 
	 * @param network The {@link Network} to load links from.
	 * @param initializeLinks If <b>true</b>, this method will first clear
	 * any stored link IDs.
	 */
	public void loadLinksFromNetwork(Network network, boolean initializeLinks){
		if (initializeLinks){
			this.plusLinks.clear();
			this.minusLinks.clear();
		}
		
		for (Link l : network.getLinks().values()){
			this.checkAddLink(l);
		}
	}
	
	private void checkAddLink(Link link){
		
		Coordinate coord1 = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Coordinate coord2 = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		LineString gLink = factory.createLineString(new Coordinate[]{coord1, coord2});
		
		if (gLink.crosses(geom2)){
			//Figure out which of this screenline's segments the link crosses
			
			Coordinate[] points = geom2.getCoordinates();
			Coordinate prevPoint = points[0];
			for(int i = 1; i <  points.length; i++){
				LineString temp = factory.createLineString(new Coordinate[]{prevPoint, points[i]});
				
				if (gLink.crosses(temp)){
					//Take the dot-product of the segment with the link
					double dp = dotProduct(prevPoint, points[i], coord1, coord2);
					
					if (dp < 0 )
						minusLinks.add(link.getId());
					else if (dp > 0 )
						plusLinks.add(link.getId());
					//Do nothing if dp == 0; this implies the link is parallel.
					
					break;
				}
				prevPoint = points[i];
			}
		}
	}
	
	private double dotProduct(Coordinate from1, Coordinate to1, Coordinate from2, Coordinate to2){		
		return ((to1.x - from1.x) * (to2.x - from2.x)) + ((to1.y - to1.y) * (to2.y - to2.y));
	}
	
	public Id getID(){
		return this.id;
	}
	
	public List<Id> getPlusLinks(){
		return this.plusLinks;
	}
	
	public List<Id> getMinusLinks(){
		return this.minusLinks;
	}
	
}
