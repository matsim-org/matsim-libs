package playground.toronto.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * A cordon is some kind of administrative boundary, represented by a closed polygon. Cordons
 * are often used for modeling of 'congestion zone' type scenarios, but could also be used
 * for analysis. This class facilitates cordon analysis by loading in a polyon geometry and 
 * figuring out which network links cross it and which links are interior.
 * 
 * <br><br> In addition to metadata, the cordon contains three {@link Collection}s of link
 * {@link Id}s: links crossing <em>into</em> the cordon, links crossing <em>out from</em> 
 * the cordon, and interior links fully contained within the cordon.
 * 
 * <br><br>This class also implements the {@link NetworkLinkFilter} interface, which allows
 * it to also be used to extract subareas from the network.
 * 
 * @author pkucirek
 *
 */
public class Cordon implements NetworkLinkFilter {
	
	/////////////////////////////////////////////////////////////////////////////////////
		
	// Identifiers for shapefile attributes.
	public final static String ID = "Id";
	public final static String DESCRNAME = "Descr";
	
	/**
	 * Loads a set of cordons from a shapefile. 
	 * 
	 * @param shapefile - The file location of a properly-formatted shapefile. The
	 * 		shapefile can only include POLYGON geometries, and the attribute table
	 * 		MUST include columns labelled "Id" and "Descr".
	 * @return Map of {@link Id} : {@link Cordon}
	 */
	public static Map<Id<Cordon>, Cordon> openShapefile(String shapefile){
		HashMap<Id<Cordon>, Cordon> result = new HashMap<>();
		
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = reader.getFeatureSet();
		
		for (SimpleFeature f : features){
			
			Id<Cordon> cid;
			try{
				cid = Id.create((Integer) f.getAttribute(Cordon.ID), Cordon.class);
			}catch (ClassCastException c){
				cid = Id.create((String) f.getAttribute(Cordon.ID), Cordon.class);
			}
				
			Cordon c = new Cordon(cid,
						(MultiPolygon) f.getAttribute(0));
			
			c.description = (String) f.getAttribute(Cordon.DESCRNAME);
			
			//Non-existant attributes will be returned as null. This is fine, except for the
			// geometry. But that should throw different errors.
			
			result.put(c.getId(), c);
		}
		
		return result;
	}
	
	/**
	 * Loads a set of cordons from a shapefile, initializing each Cordon with links 
	 * from the network.
	 * 
	 * @param shapefile - The file location of a properly-formatted shapefile. The
	 * 		shapefile can only include POLYGON geometries, and the attribute table
	 * 		MUST include columns labelled "Id" and "Descr".
	 * @param network - The {@link Network} to load links from. 
	 * @return Map of {@link Id} : {@link Cordon}
	 */
	public static Map<Id<Cordon>, Cordon> openShapefile(String shapefile, Network network){
		Map<Id<Cordon>, Cordon> result = Cordon.openShapefile(shapefile);
		
		for (Cordon c : result.values()){
			c.loadLinksFromNetwork(network);
		}
		
		return result;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//Attributes
	private final Id<Cordon> id;
	public String description;
	
	private Collection<Id<Link>> inLinks;
	private Collection<Id<Link>> outLinks;
	private Collection<Id<Link>> interiorLinks;
	private final MultiPolygon geom;
	private GeometryFactory factory;
	
	public Cordon(Id<Cordon> id, MultiPolygon geometry){
		this.id = id;
		this.geom = geometry;
		
		this.inLinks = new ArrayList<>();
		this.outLinks = new ArrayList<>();
		this.interiorLinks = new ArrayList<>();
		
		this.factory = new GeometryFactory();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Loads the cordon's links from a given {@link Network}. By
	 * default, invoking this method will clear any stored link IDs.
	 * 
	 * @param network The {@link Network} to load links from.
	 */
	public void loadLinksFromNetwork(Network network){
		this.loadLinksFromNetwork(network, true);
	}
	
	/**
	 * Loads the cordon's links from a given {@link Network}.
	 * 
	 * @param network The {@link Network} to load links from.
	 * @param initializeLinks If <b>true</b>, this method will first clear
	 * any stored link IDs.
	 */
	public void loadLinksFromNetwork(Network network, boolean initializeLinks){
		if (initializeLinks){
			this.inLinks.clear();
			this.outLinks.clear();
			this.interiorLinks.clear();
		}
		
		for (Link l : network.getLinks().values()){
			this.checkAddLink(l);
		}
	}
	
	private void checkAddLink(Link link){
		Point fromPoint = this.factory.createPoint(new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()));
		Point toPoint = this.factory.createPoint(new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()));
		
		boolean fpIn = this.geom.contains(fromPoint);
		boolean tpIn = this.geom.contains(toPoint);
		
		if (fpIn && tpIn){
			this.interiorLinks.add(link.getId());
		}else if (fpIn){
			this.outLinks.add(link.getId());
		}else if(tpIn){
			this.inLinks.add(link.getId());
		}
	}
	
	public Id<Cordon> getId(){
		return this.id;
	}
	
	/**
	 * @return Ids of links crossing into the cordon
	 */
	public Collection<Id<Link>> getIncomingLinks(){
		return this.inLinks;
	}
	
	/**
	 * @return Ids of links crossing out from the cordon
	 */
	public Collection<Id<Link>> getOutgoingLinks(){
		return this.outLinks;
	}
	
	/**
	 * @return Ids of links containted within the cordon
	 */
	public Collection<Id<Link>> getInteriorLinks(){
		return this.interiorLinks;
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	@Override
	public boolean judgeLink(Link l) {
		if (inLinks.contains(l.getId()) ||
				outLinks.contains(l.getId()) ||
				interiorLinks.contains(l.getId()))
			return true;
		
		return false;
	}
}
