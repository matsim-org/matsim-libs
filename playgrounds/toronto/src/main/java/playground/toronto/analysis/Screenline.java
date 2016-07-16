package playground.toronto.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
	 * Loads a set of screenlines from a shapefile.
	 * 
	 * @param shapefile - The file location of a properly-formatted shapefile. The 
	 * 		shapefile can only include POLYLINE geometries and its attribute
	 * 		table MUST include columns labeled "Id", "PosDirName", "NegDirName", and
	 * 		"Descr".
	 * @return Map of {@link Id} : {@link Screenline}.
	 */
	public static Map<Id<Screenline>, Screenline> openShapefile(String shapefile){
		HashMap<Id<Screenline>, Screenline> result = new HashMap<>();
		
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = reader.getFeatureSet();
		
		for (SimpleFeature f : features){
			
			Id<Screenline> sid;
			try{
				sid = Id.create((Integer) f.getAttribute(Screenline.ID), Screenline.class);
			}catch (ClassCastException c){
				sid = Id.create((String) f.getAttribute(Screenline.ID), Screenline.class);
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
	 * Loads a set of screenlines from a shapefile, initializing each Screenline with links 
	 * from the network.
	 * 
	 * @param shapefile - The file location of a properly-formatted shapefile. The 
	 * 		shapefile can only include POLYLINE geometries and its attribute
	 * 		table MUST include columns labeled "Id", "PosDirName", "NegDirName", and
	 * 		"Descr".
	 * @param network - The {@link Network} to load links from. 
	 * @return Map of {@link Id} : {@link Screenline}.
	 */
	public static Map<Id<Screenline>, Screenline> openShapefile(String shapefile, Network network){
		Map<Id<Screenline>, Screenline> result = Screenline.openShapefile(shapefile);
		
		for (Screenline s : result.values()){
			s.loadLinksFromNetwork(network);
		}
		
		return result;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//Attributes
	private final Id<Screenline> id;
	public String description;
	public String plusName;
	public String minusName;
	
	private List<Id<Link>> plusLinks;
	private List<Id<Link>> minusLinks;
	//private LineString geom;
	private final MultiLineString geom2;
	private GeometryFactory factory;
	
	
	public Screenline(Id<Screenline> id, MultiLineString geometry, String plusName, String minusName){
		this.id = id;
		this.plusName = plusName;
		this.minusName = minusName;
		this.geom2 = geometry;
		
		this.plusLinks = new ArrayList<>();
		this.minusLinks = new ArrayList<>();
		
		this.factory = new GeometryFactory();
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Loads the screenline's links from a given {@link Network}. By
	 * default, invoking this method will clear any stored link IDs.
	 * 
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
					double cp = crossProduct(prevPoint, points[i], coord1, coord2);
					
					if (cp < 0 )
						minusLinks.add(link.getId());
					else if (cp > 0 )
						plusLinks.add(link.getId());
					//Do nothing if dp == 0; this implies the link is parallel.					
					break;
				}
				prevPoint = points[i];
			}
		}
	}
	
	private double crossProduct(Coordinate from1, Coordinate to1, Coordinate from2, Coordinate to2){
		return (to1.x - from1.x) * (to2.y - from2.y) - (to1.y - from1.y) * (to2.x - from2.x);
	}
	
	public Id<Screenline> getID(){
		return this.id;
	}
	
	/**
	 * @return Ids of links crossing in the positive direction (right-to-left)
	 */
	public List<Id<Link>> getPlusLinks(){
		return this.plusLinks;
	}
	
	/**
	 * @return Ids of links crossing in the negative direction (left-to-right)
	 */
	public List<Id<Link>> getMinusLinks(){
		return this.minusLinks;
	}
	
}
