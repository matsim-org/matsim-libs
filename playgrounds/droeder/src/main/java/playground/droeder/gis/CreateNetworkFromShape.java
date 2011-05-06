package playground.droeder.gis;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.misc.ConfigUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;


public class CreateNetworkFromShape {
	
	private static final Logger log = Logger.getLogger(CreateNetworkFromShape.class);
	
	private ShapeFileReader myReader;
	private Set<Feature> features;
	private Integer id;
	private Integer geometry;
	private String shapeFile;
	private Network net;
	private NetworkFactory netFac;
	private Polygon area = null;
	private ScenarioImpl sc;
	
	
	public static void main(String[] args){
		String dir = "D:/VSP/output/BerlinShape/";
		String netOutFile = dir + "net.xml";
		String inFile = dir + "sechwy.shp";
		String linkOutShp = dir + "links.shp";
		String nodeOutShp = dir + "nodes.shp";
		
		Coordinate[] coord = new Coordinate[5];
		coord[0] = new Coordinate(13.24, 52.53);
		coord[1] = new Coordinate(13.24, 52.43);
		coord[2] = new Coordinate(13.40, 52.43);
		coord[3] = new Coordinate(13.40, 52.53);
		coord[4] = new Coordinate(13.24, 52.53);
		
		CreateNetworkFromShape creator = new CreateNetworkFromShape(1, 0, inFile);
		creator.specifyAreaToConvert(coord);
		creator.run(netOutFile);
		creator.write2Shape(linkOutShp, nodeOutShp);
	}
	
	/**
	 * run <code>AnalyzeMyShapeFile</code> to get position of Attributes in ShapeFile
	 * works only for <Code>LineString</code>
	 * @param id
	 * @param geometry
	 * @param shapeFile
	 */
	public CreateNetworkFromShape(Integer idPosition, Integer geometryPosition, String shapeFile){
		this.myReader = new ShapeFileReader();
		this.shapeFile = shapeFile;
		this.id = idPosition;
		this.geometry = geometryPosition;
	}
	
	public void run(String netOutFile){
		this.features = this.myReader.readFileAndInitialize(this.shapeFile);
		
		this.createNetwork();
		this.writeNetwork(netOutFile);
	}

	private void createNetwork() {
		this.sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.net = sc.getNetwork();
		this.netFac = this.net.getFactory();
		Geometry g;
		
		for(Feature f: this.features){
			g = (Geometry) f.getAttribute(this.geometry);
			
			this.createNodesFromGeometry(g);
			this.createLinksFromGeometry(g, f.getAttribute(this.id).toString(), null);
		}
	}

	//TODO get better method for nodeExists and better id's, won't work if coordinates not match exactly 
	private void createNodesFromGeometry(Geometry g) {
		if(areaContainsGeometry(g)){
			for(Coordinate c : g.getCoordinates()){
				if(!this.nodeExists(c) ){
					Id node = this.sc.createId(c.x + "_" + c.y);
					this.net.addNode(this.netFac.createNode(node, MGC.coordinate2Coord(c)));
				}
			}
		}
	}

	private boolean nodeExists(Coordinate c) {
		if(this.net.getNodes().containsKey(new IdImpl(c.x + "_" + c.y))){
			return true;
		}else{
			return false;
		}
	}

	//TODO set link attributes (use RouteType) and check direction
	private void createLinksFromGeometry(Geometry g, String origId, String RouteType) {
		Coordinate[] c = g.getCoordinates();
		Node from;
		Node to;
		Id id;
		Link l;
		
		if(areaContainsGeometry(g)){
			for(int i = 1; i < c.length; i++){
				id = this.sc.createId(origId + "_" + i);
				
				from = this.net.getNodes().get(new IdImpl(c[i-1].x + "_" + c[i-1].y));
				to = this.net.getNodes().get(new IdImpl(c[i].x + "_" + c[i].y));
				
				if(!this.net.getLinks().containsKey(id)){
					l = this.netFac.createLink(id, from, to);
//					l.setCapacity(capacity);
//					l.setFreespeed(freespeed);
					l.setLength(this.calcLength(from, to));
					this.net.addLink(l);
				}else{
					// TODO don't know why some links appear so often
					log.error("link " + id.toString() + " already exists!");
				}
			}
		}
		
	}
	
	private double calcLength(Node from, Node to){
		Double length = null;
		Double x = Math.pow(from.getCoord().getX() - to.getCoord().getX(), 2);
		Double y = Math.pow(from.getCoord().getY() - to.getCoord().getY(), 2);
		
		length = Math.sqrt(x + y);
		
		return length;
	}
	
	
	/**
	 * use this Method, if you don't want to convert your whole shapeFile
	 * @param coords
	 */
	public void specifyAreaToConvert(Coordinate[] coords){
		GeometryFactory geoFac = new GeometryFactory();
		LinearRing ring = geoFac.createLinearRing(coords);
		this.area = geoFac.createPolygon(ring, null);
		
	}
	
	private boolean areaContainsGeometry(Geometry g){
		if(this.area == null){
			return true;
		}else if(this.area.contains(g)){
			return true;
		}else{
			return false;
		}
	}
	
	private void writeNetwork(String file) {
		new NetworkWriter(this.net).writeFileV1(file);
	}
	
	@SuppressWarnings("unchecked")
	public void write2Shape(String linkFile, String nodeFile){
		DaShapeWriter.writeLinks2Shape(linkFile, (Map<Id, Link>) this.net.getLinks(), null);
		DaShapeWriter.writeNodes2Shape(nodeFile, (Map<Id, Node>) this.net.getNodes());
	}

}
