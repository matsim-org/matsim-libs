package playground.dhosse.bachelorarbeit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.gis.Zone;
import org.matsim.contrib.accessibility.gis.ZoneLayer;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.contrib.matsim4urbansim.utils.io.misc.ProgressBar;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class NetworkInspector {//TODO pfade ändern
	
	@Deprecated // please do not public static variables in matsim. kai, mar'14
	private static Scenario scenario = null;
	
	private Map<Id,Double> geometricLengths = new HashMap<Id,Double>();
	private Map<Id,String> nodeTypes = new HashMap<Id,String>();
	
	private List<Id> nodesWithHighDegrees = new ArrayList<Id>();
	
	private Map<String,Class<? extends Geometry>> filesForExportInQGisProject = new HashMap<String,Class<? extends Geometry>>();
	
	private List<Link> lengthBelowStorageCapacity = new ArrayList<Link>();
	
	private Logger logger = Logger.getLogger(NetworkInspector.class);
	
	private double totalLength = 0;
	private double totalGLength = 0;
	
	private SimpleFeatureBuilder builder;
	
	private Geometry envelope;
	
	private String outputFolder = "./NetworkInspector.output/";
	
	/**
	 * 
	 * @param args network file
	 */
	public static void main(String args[]){
		
		NetworkInspector.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig()); 
		
		new MatsimNetworkReader(NetworkInspector.scenario).readFile(args[0]);
		
		new NetworkInspector().run();
		
	}
	
	public NetworkInspector(){
		
		try {
			Runtime.getRuntime().exec("mkdir " + this.outputFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.envelope = MinimumEnvelope.run(NetworkInspector.scenario.getNetwork()).buffer(-50);
		
	}
	
	private void run() {
		
		this.filesForExportInQGisProject.clear();
		
		if(isRoutable()){
			
			continueWithRoutableNetwork();
			
		}
		
		try {
			logger.info("creating r statistics...");
			Runtime.getRuntime().exec("C:/Program Files/R/R-2.15.2/bin/Rscript " + "./R/createStatistics.R");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.filesForExportInQGisProject.put("envelope", Polygon.class);
		
		logger.info("writing QuantumGIS project file (*.qgs) with generated layers...");
		
		new QGisExport(this.filesForExportInQGisProject).write("networkInspectorOutput.qgs");
				
		logger.info("exiting NetworkInspector...");
		
	}

	private boolean isRoutable(){
		
		//aus klasse "NetworkCleaner"
		final Map<Id,Node> visitedNodes = new TreeMap<Id,Node>();
		Map<Id,Node> biggestCluster = new TreeMap<Id,Node>();
		
		Map<Id,Link> smallClusterLinks = new TreeMap<Id,Link>();
		
		logger.info("  checking " + NetworkInspector.scenario.getNetwork().getNodes().size() + " nodes and " +
				NetworkInspector.scenario.getNetwork().getLinks().size() + " links for dead-ends...");
		
		boolean stillSearching = true;
		Iterator<? extends Node> iter = NetworkInspector.scenario.getNetwork().getNodes().values().iterator();
		while (iter.hasNext() && stillSearching) {
			Node startNode = iter.next();
			if (!visitedNodes.containsKey(startNode.getId())) {
				Map<Id, Node> cluster = this.findCluster(startNode, NetworkInspector.scenario.getNetwork());
				visitedNodes.putAll(cluster);
				if (cluster.size() > biggestCluster.size()) {
					biggestCluster = cluster;
				}
				//eigener teil
				if(biggestCluster.size()==NetworkInspector.scenario.getNetwork().getNodes().size()){
					logger.info("size of the biggest cluster equals network size...");
					logger.info("network is routable.");
					writeClustersAndNetwork2ESRIShape(smallClusterLinks);
					return true;
				}
				if (biggestCluster.size() >= (NetworkInspector.scenario.getNetwork().getNodes().size() - visitedNodes.size())) {
					stillSearching = false;
				}
			}
		}
		
		//eigener teil
		logger.warn("size of the biggest cluster is " + biggestCluster.size() +
				" but network contains " + NetworkInspector.scenario.getNetwork().getNodes().size() + " nodes...");
		logger.warn("network is not routable. run " + NetworkCleaner.class.getName() + " first.");
		
		for(Id nodeId : NetworkInspector.scenario.getNetwork().getNodes().keySet()){
			
			if(!biggestCluster.containsKey(nodeId)){
				Node node = NetworkInspector.scenario.getNetwork().getNodes().get(nodeId);
				for(Link l : node.getOutLinks().values()){
					if(!smallClusterLinks.containsKey(l.getId())){
						smallClusterLinks.put(l.getId(), l);
					}
				}
			}
			
		}
		
		writeClustersAndNetwork2ESRIShape(smallClusterLinks);
		
		return false;
		
	}
	
	//aus klasse NetworkCleaner
	private Map<Id, Node> findCluster(final Node startNode, final Network network) {

		final Map<Node, DoubleFlagRole> nodeRoles = new HashMap<Node, DoubleFlagRole>(network.getNodes().size());

		ArrayList<Node> pendingForward = new ArrayList<Node>();
		ArrayList<Node> pendingBackward = new ArrayList<Node>();

		TreeMap<Id, Node> clusterNodes = new TreeMap<Id, Node>();
		clusterNodes.put(startNode.getId(), startNode);
		DoubleFlagRole r = getDoubleFlag(startNode, nodeRoles);
		r.forwardFlag = true;
		r.backwardFlag = true;

		pendingForward.add(startNode);
		pendingBackward.add(startNode);

		while (pendingForward.size() > 0) {
			int idx = pendingForward.size() - 1;
			Node currNode = pendingForward.remove(idx);
			for (Link link : currNode.getOutLinks().values()) {
				Node node = link.getToNode();
				r = getDoubleFlag(node, nodeRoles);
				if (!r.forwardFlag) {
					r.forwardFlag = true;
					pendingForward.add(node);
				}
			}
		}

		while (pendingBackward.size() > 0) {
			int idx = pendingBackward.size()-1;
			Node currNode = pendingBackward.remove(idx);
			for (Link link : currNode.getInLinks().values()) {
				Node node = link.getFromNode();
				r = getDoubleFlag(node, nodeRoles);
				if (!r.backwardFlag) {
					r.backwardFlag = true;
					pendingBackward.add(node);
					if (r.forwardFlag) {
						clusterNodes.put(node.getId(), node);
					}
				}
			}
		}

		return clusterNodes;
	}

	private void writeClustersAndNetwork2ESRIShape(
			Map<Id, Link> smallClusterLinks) {
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",LineString.class);
		typeBuilder.add("ID",String.class);
		typeBuilder.add("length",Double.class);
		typeBuilder.add("freespeed",Double.class);
		typeBuilder.add("capacity",Double.class);
		typeBuilder.add("nlanes", String.class);
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		logger.info("writing network into ESRI shapefile...");
		
		for(Link link : NetworkInspector.scenario.getNetwork().getLinks().values()){
			
			SimpleFeature feature = this.builder.buildFeature(null, new Object[]{
				new GeometryFactory().createLineString(new Coordinate[]{
							new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()),
							new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY())
				}),
				link.getId(),
				link.getLength(),
				link.getFreespeed(),
				link.getCapacity(),
				link.getNumberOfLanes()
				
			});
			
			features.add(feature);
			
		}
		
		String destination = "network";
		
		this.filesForExportInQGisProject.put(destination,LineString.class);
		
		ShapeFileWriter.writeGeometries(features, this.outputFolder+destination+".shp");
		
		if(smallClusterLinks.size()>0){
			
			features.clear();
			
			logger.info("writing small clusters into ESRI shapefile...");
			
			for(Link link : smallClusterLinks.values()){
				
				SimpleFeature feature = this.builder.buildFeature(null, new Object[]{
						new GeometryFactory().createLineString(new Coordinate[]{
								new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()),
								new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY())
						}),
						link.getId(),
						link.getLength(),
						link.getFreespeed(),
						link.getCapacity(),
						link.getNumberOfLanes()
				});
				
				features.add(feature);
				
			}
			
			destination = "smallClusters";
			
			this.filesForExportInQGisProject.put(destination,LineString.class);
			
			ShapeFileWriter.writeGeometries(features, this.outputFolder+destination+".shp");
			
		}
	}
	
	private void continueWithRoutableNetwork() {
		
		checkNodeAttributes();
		checkLinkAttributes();
		
		if(!(this.nodeTypes.size()<1))
			exportNodesToShape();
		
		BoundingBox bbox = BoundingBox.createBoundingBox(NetworkInspector.scenario.getNetwork());
		
		ZoneLayer<Id> measuringPoints = NetworkInspector.createGridLayerByGridSizeByNetwork(50, bbox.getBoundingBox());
		// tnicolai: ich habe die GridUtils auskommentiert, da es sonst nicht mehr kompiliert.

		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getXMin(),bbox.getYMin(),bbox.getXMax(),bbox.getYMax(), 50, Double.NaN);
		MutableScenario sc = (MutableScenario) NetworkInspector.scenario;
		
		new AccessibilityCalcV2(measuringPoints, freeSpeedGrid, sc, this.outputFolder).runAccessibilityComputation();
		
	}
	
	//eigene methode
	private void checkLinkAttributes() {
		
		logger.info("checking link attributes...");
		
		int writerIndex = 0;
		double cellWidth = ((NetworkImpl)NetworkInspector.scenario.getNetwork()).getEffectiveCellSize();
		for(Link link : NetworkInspector.scenario.getNetwork().getLinks().values()){
			double geometricLength = 
					Math.sqrt(Math.pow(link.getToNode().getCoord().getX() -
							link.getFromNode().getCoord().getX(),2) +
							Math.pow(link.getToNode().getCoord().getY() -
							link.getFromNode().getCoord().getY(), 2));
			
			this.geometricLengths.put(link.getId(), geometricLength);
			
			this.totalLength += link.getLength();
			this.totalGLength += geometricLength;
			if(link.getLength()<cellWidth){
				this.lengthBelowStorageCapacity.add(link);
				writerIndex++;
			}
					
		}
		
		
		logger.info("done.");
		
		createLinkStatisticsFile();
		
		File file = new File(this.outputFolder+"lengthBelowStorageCapacity.txt");
		FileWriter writer;
		
		try {
			
			writer = new FileWriter(file);
			for(Link link : this.lengthBelowStorageCapacity){
				writer.write("length of link " + link.getId() + " below min length for storage capacity of one vehicle (" +
						link.getLength() + "m instead of "+ cellWidth +" m)\n");
			}
			writer.close();
						
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		if(writerIndex>0)
			logger.warn(writerIndex + " warnings about storage capacity. written to file " + file.getName());
		
		logger.info("total length of network: " + this.totalLength + "m, total geom. length: " + this.totalGLength + "m");
	
		logger.info("done.");

	}
	
	//eigene methode
	private void checkNodeAttributes() {
		
		logger.info("checking node attributes...");
		
		int exit = 0, red = 0, de = 0;
		
		for(Node node : NetworkInspector.scenario.getNetwork().getNodes().values()){

			if(node.getInLinks().size()>0&&node.getOutLinks().size()>0){
				
				if(node.getInLinks().size()>5||node.getOutLinks().size()>5)
					this.nodesWithHighDegrees.add(node.getId());
					
				if(node.getInLinks().size()==1&&node.getOutLinks().size()==1){
					
					Link inLink = node.getInLinks().values().iterator().next();
					Link outLink = node.getOutLinks().values().iterator().next();
					
					if(inLink.getFromNode().equals(outLink.getToNode())){
						if(!this.envelope.contains(new Point(new CoordinateArraySequence(new Coordinate[]{
								new Coordinate(node.getCoord().getX(),node.getCoord().getY())}),new GeometryFactory()))){
							this.nodeTypes.put(node.getId(), "exit");
							exit++;
							continue;
						}
						this.nodeTypes.put(node.getId(), "deadEnd");
						de++;
						
					} else{
						
						if(inLink.getCapacity()==outLink.getCapacity()&&
								inLink.getFreespeed()==outLink.getFreespeed()&&
								inLink.getNumberOfLanes()==outLink.getNumberOfLanes()&&
								inLink.getAllowedModes()==outLink.getAllowedModes()){
							this.nodeTypes.put(node.getId(), "redundant");
							red++;
								
						}
					}
				}
			}
			
		}
		
		logger.info("done.");
		
		logger.info(de + " dead end nodes, " + exit + " exit road nodes and " + red + " redundant nodes found.");
		
		
		createNodeStatisticsFile();
		
		logger.info("done.");
		
	}
		
	private void createLinkStatisticsFile(){
		
		logger.info("writing link statistics...");
		
		File file = new File(this.outputFolder+"linkStatistics.txt");
		
		try {
			
			FileWriter writer = new FileWriter(file);
			
			writer.write("ID \t length \t geometricLength \t nlanes \t capacity");
			
			for(Link l : NetworkInspector.scenario.getNetwork().getLinks().values()){
				
				writer.write("\n"+ l.getId() +
						"\t"+l.getLength() +
						"\t" + this.geometricLengths.get(l.getId()) +
						"\t" + l.getNumberOfLanes() +
						"\t" + l.getCapacity());
				
			}
			
			writer.close();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}
	
	//eigene methode
	private void createNodeStatisticsFile() {
		
		logger.info("writing node statistics...");
		
		File file = new File(this.outputFolder+"nodeStatistics.txt");
		FileWriter writer;
		try {
			writer = new FileWriter(file);
			
			writer.write("ID\tinDegree\toutDegree");
			
			for(Node n : NetworkInspector.scenario.getNetwork().getNodes().values()){
				
				writer.write("\n"+n.getId()+"\t"+n.getInLinks().size()+"\t"+n.getOutLinks().size());
				
			}
			
			writer.close();
			
		} catch (IOException e1) {
			
			e1.printStackTrace();
			
		}

	}
	
	private void exportNodesToShape(){
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("node");
		typeBuilder.add("location",Point.class);
		typeBuilder.add("ID",String.class);
		typeBuilder.add("type",String.class);
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(Id nodeId : this.nodeTypes.keySet()){
			
			Point p = MGC.coord2Point(NetworkInspector.scenario.getNetwork().getNodes().get(nodeId).getCoord());
			try {
				features.add(this.builder.buildFeature(null, new Object[]{p,nodeId.toString(),this.nodeTypes.get(nodeId)}));
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			}
			
		}
		
		String destination = "nodeTypes";
		
		this.filesForExportInQGisProject.put(destination,Point.class);
		
		ShapeFileWriter.writeGeometries(features, this.outputFolder+destination+".shp");
		
		if(!(this.nodesWithHighDegrees.size()<1)){
			features.clear();
		
			for(Id nodeId : this.nodesWithHighDegrees){
			
				typeBuilder.setName("node");
				typeBuilder.add("location",Point.class);
				typeBuilder.add("ID",String.class);
				typeBuilder.add("in-degree",Integer.class);
				typeBuilder.add("out-degree",Integer.class);
				this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
			
				Point p = MGC.coord2Point(NetworkInspector.scenario.getNetwork().getNodes().get(nodeId).getCoord());
				try {
					features.add(this.builder.buildFeature(null, new Object[]{p,nodeId.toString(),
							NetworkInspector.scenario.getNetwork().getNodes().get(nodeId).getInLinks().size(),
							NetworkInspector.scenario.getNetwork().getNodes().get(nodeId).getOutLinks().size()}));
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				}
			}
		
			destination = "highDegreeNodes";
		
			this.filesForExportInQGisProject.put(destination, Point.class);
			ShapeFileWriter.writeGeometries(features, this.outputFolder+destination+".shp");
		}
		
	}
	
	//aus klasse networkcleaner
	private static DoubleFlagRole getDoubleFlag(final Node n, final Map<Node, DoubleFlagRole> nodeRoles) {
		DoubleFlagRole r = nodeRoles.get(n);
		if (null == r) {
			r = new DoubleFlagRole();
			nodeRoles.put(n, r);
		}
		return r;
	}
	
	//aus klasse networkcleaner
	static class DoubleFlagRole {
		protected boolean forwardFlag = false;
		protected boolean backwardFlag = false;
	}
	
	public Geometry getArea() {
		return envelope;
	}

	public void setArea(Geometry area) {
		this.envelope = area;
	}
	
	public static ZoneLayer<Id> createGridLayerByGridSizeByNetwork(double gridSize, double [] boundingBox) {
		
//		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();

		double xmin = boundingBox[0];
		double ymin = boundingBox[1];
		double xmax = boundingBox[2];
		double ymax = boundingBox[3];
		
		
		ProgressBar bar = new ProgressBar( (xmax-xmin)/gridSize );
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = xmin; x <xmax; x += gridSize) {
			
			bar.update();
						
			for(double y = ymin; y < ymax; y += gridSize) {
				
				// check first if cell centroid is within study area
				double center_X = x + (gridSize/2);
				double center_Y = y + (gridSize/2);
				
				// check if x, y is within network boundary
				if (center_X <= xmax && center_X >= xmin && 
					center_Y <= ymax && center_Y >= ymin) {
				
					Point point = factory.createPoint(new Coordinate(x, y));
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + gridSize);
					coords[2] = new Coordinate(x + gridSize, y + gridSize);
					coords[3] = new Coordinate(x + gridSize, y);
					coords[4] = point.getCoordinate();
					// Linear Ring defines an artificial zone
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					// polygon.setSRID( srid ); // tnicolai: this is not needed to match the grid layer with locations / facilities from UrbanSim
					
					Zone<Id> zone = new Zone<Id>(polygon);
					zone.setAttribute( Id.create( setPoints, Zone.class ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

//		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
//		log.info("Done with setting starting points!");
		
		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);
		return layer;
	}

}
