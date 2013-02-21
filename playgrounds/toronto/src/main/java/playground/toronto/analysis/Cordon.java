package playground.toronto.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author pkucirek
 *
 */
public class Cordon {
	
	/////////////////////////////////////////////////////////////////////////////////////
		
	// Identifiers for shapefile attributes.
	public final static String ID = "Id";
	public final static String DESCRNAME = "Descr";
	
	public static Map<Id, Cordon> openShapefile(String shapefile){
		HashMap<Id, Cordon> result = new HashMap<Id, Cordon>();
		
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(shapefile);
		Collection<SimpleFeature> features = reader.getFeatureSet();
		
		for (SimpleFeature f : features){
			
			Id cid;
			try{
				cid = new IdImpl((Integer) f.getAttribute(Cordon.ID));
			}catch (ClassCastException c){
				cid = new IdImpl((String) f.getAttribute(Cordon.ID));
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
	
	public static Map<Id, Cordon> openShapefile(String shapefile, Network network){
		Map<Id, Cordon> result = Cordon.openShapefile(shapefile);
		
		for (Cordon c : result.values()){
			c.loadLinksFromNetwork(network);
		}
		
		return result;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//Attributes
	private final Id id;
	public String description;
	
	private List<Id> inLinks;
	private List<Id> outLinks;
	private List<Id> interiorLinks;
	private final MultiPolygon geom;
	private GeometryFactory factory;
	
	public Cordon(Id id, MultiPolygon geometry){
		this.id = id;
		this.geom = geometry;
		
		this.inLinks = new ArrayList<Id>();
		this.outLinks = new ArrayList<Id>();
		this.interiorLinks = new ArrayList<Id>();
		
		this.factory = new GeometryFactory();
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public void loadLinksFromNetwork(Network network){
		this.loadLinksFromNetwork(network, true);
	}
	
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
	
	public Id getId(){
		return this.id;
	}
	
	public List<Id> getInLinks(){
		return this.inLinks;
	}
	
	public List<Id> getOutLinks(){
		return this.outLinks;
	}
	
	public List<Id> getInteriorLinks(){
		return this.interiorLinks;
	}
}
