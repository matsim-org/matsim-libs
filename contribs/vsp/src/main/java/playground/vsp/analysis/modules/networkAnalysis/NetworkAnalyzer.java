package playground.vsp.analysis.modules.networkAnalysis;

import java.io.BufferedWriter;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.gis.SpatialGridTableWriter;
import org.matsim.contrib.accessibility.gis.Zone;
import org.matsim.contrib.accessibility.gis.ZoneLayer;
import org.matsim.contrib.accessibility.utils.ProgressBar;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.vsp.analysis.modules.AbstractAnalysisModule;
import playground.vsp.analysis.modules.networkAnalysis.utils.AccessibilityCalc;
import playground.vsp.analysis.modules.networkAnalysis.utils.BoundingPolygon;
import playground.vsp.analysis.modules.networkAnalysis.utils.QGisProjectFileWriter;

/**
 * This class analyzes the properties of a given network file, such as:
 * (1) node statistics
 * 	(i) degrees
 * 	(ii) types (redundant, dead end, exit road)
 * (2) link statistics
 * (3) accessibility computation (depending on walk travel time to the network)
 * 
 * @author dhosse
 * 
 */

public class NetworkAnalyzer extends AbstractAnalysisModule{

	private Logger log = Logger.getLogger(NetworkAnalyzer.class);
	
	private Scenario scenario;
	private Network network;
	
	private Map<String,List<Id>> nodeTypes;
	private Map<Id,Double> geometricLengths;
	
	private List<Id> nodesWithHighDegrees;
	private List<Link> lengthBelowStorageCapacity;
	private Map<Id,Link> smallClusterLinks;
	
	private Map<String,Class<? extends Geometry>> filesForExportInQGisProject;
	
	private double totalLength = 0;
	private double totalGLength = 0;
	
	private SimpleFeatureBuilder builder;
	
	private Geometry envelope;
	
	private final String deadEnd = "deadEnd";
	private final String exit = "exit";
	private final String redundant = "redundant";
	
	private final String TXTfile = ".txt";
	private final String QGSfile = ".qgs";
	private final String SHPfile = ".shp";
	
	private String targetCoordinateSystem;
	
	private boolean nodesChecked = false;
	private boolean linksChecked = false;
	
	private SpatialGrid freeSpeedGrid = null;
	
	/**
	 * Creates a new analyzer for properties of a given network file.
	 * 
	 * @param networkInputFile The MATSim-Network file to analyse.
	 * @param targetCoordinateSystem The coordinate system your network data is transformed to.
	 */
	public NetworkAnalyzer(String networkInputFile, String targetCoordinateSystem) {
		
		super(NetworkAnalyzer.class.getSimpleName());
		this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.targetCoordinateSystem = targetCoordinateSystem;
		
		if(networkInputFile!=null){
			new MatsimNetworkReader(this.scenario.getNetwork()).readFile(networkInputFile);
			this.network = this.scenario.getNetwork();
		}
		
	}

	@Override
	public List<EventHandler> getEventHandler() {
		return null;
	}

	@Override
	public void preProcessData() {
		//nothing to do...
	}

	@Override
	public void postProcessData() {
		
		this.nodeTypes = new TreeMap<String, List<Id>>();
		this.geometricLengths = new HashMap<Id,Double>();
		this.nodesWithHighDegrees = new ArrayList<Id>();
		this.filesForExportInQGisProject = new HashMap<String,Class<? extends Geometry>>();
		this.lengthBelowStorageCapacity = new ArrayList<Link>();
		this.smallClusterLinks = new TreeMap<Id, Link>();
		
		this.envelope = new BoundingPolygon(network, 200).returnPolygon();
		this.filesForExportInQGisProject.put("envelope", Polygon.class);
		
		if(isRoutable()){
			continueWithRoutableNetwork();
			
			log.info("total length of all network links: " + this.totalLength + " m");
			log.info("total geometric length of all network links: " + this.totalGLength+ " m");
		}
		
	}

	@Override
	public void writeResults(String outputFolder) {
	
		try {
			exportEnvelopeToShape(outputFolder);
			writeNodesFiles(outputFolder);
			writeLinkFiles(outputFolder);
			writeAccessibilityMap(outputFolder);
			new QGisProjectFileWriter(this.filesForExportInQGisProject).write(outputFolder+"project"+this.QGSfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void exportEnvelopeToShape(String outputFolder) {

		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("envelope",Polygon.class);
		typeBuilder.add("area",Double.class);
		
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		SimpleFeature feature = this.builder.buildFeature(null, new Object[]{
				this.envelope,
				this.envelope.getArea()
		});
		
		features.add(feature);

		ShapeFileWriter.writeGeometries(features, outputFolder+"envelope"+this.SHPfile);

	}

	/**
	 * Checks whether the given network file is routable or not.
	 * @return True if the given network contains of only one cluster and false, if there is more than one.
	 */
	private boolean isRoutable(){
		
		//the nodes that have already been inspected
		final Map<Id,Node> visitedNodes = new TreeMap<Id,Node>();

		//a map to store the biggest cluster of the network
		Map<Id,Node> biggestCluster = new TreeMap<Id,Node>();
		
		boolean stillSearching = true;
		Iterator<? extends Node> iter = this.network.getNodes().values().iterator();
		
		log.info("  checking " + this.network.getNodes().size() + " nodes and " + this.network.getLinks().size() + " links for dead-ends...");
		
		//run as long as there are still unvisited nodes
		while (iter.hasNext() && stillSearching) {
			Node startNode = iter.next();
			if (!visitedNodes.containsKey(startNode.getId())) {
				Map<Id, Node> cluster = this.findCluster(startNode, this.network);
				visitedNodes.putAll(cluster);
				if (cluster.size() > biggestCluster.size()) {
					biggestCluster = cluster;
				}
				if(biggestCluster.size()==this.network.getNodes().size()){
					log.info("size of biggest cluster equals network size... continuing with routable network...");
					return true;
				}
				if (biggestCluster.size() >= (this.network.getNodes().size() - visitedNodes.size())) {
					log.warn("at least one small cluster found... network is not routable");
					stillSearching = false;
				}
			}
		}
		
		for(Id<Node> nodeId : this.network.getNodes().keySet()){
			
			if(!biggestCluster.containsKey(nodeId)){
				Node node = this.network.getNodes().get(nodeId);
				for(Link l : node.getOutLinks().values()){
					if(!this.smallClusterLinks.containsKey(l.getId())){
						this.smallClusterLinks.put(l.getId(), l);
					}
				}
			}
			
		}
		
		return false;
		
	}
	
	/**
	 * If the network is routable, the analysis methods are running over the network.
	 */
	private void continueWithRoutableNetwork() {
		
		checkNodeAttributes();
		checkLinkAttributes();
		
		//create a bounding box for accessibility computation
		BoundingBox bbox = BoundingBox.createBoundingBox(this.scenario.getNetwork());
		
		double resolution = 100;
		
		//set measuring points from which the accessibility is measured
		ZoneLayer<Id<Zone>> measuringPoints = NetworkAnalyzer.createGridLayerByGridSizeByNetwork(resolution, bbox.getBoundingBox());

		//initialize grid that stores the accessibility values
		this.freeSpeedGrid = new SpatialGrid(bbox.getXMin(),bbox.getYMin(),bbox.getXMax(),bbox.getYMax(), resolution, Double.NaN);
		MutableScenario sc = (MutableScenario) this.scenario;
		
		//run accessibility computation
		new AccessibilityCalc(measuringPoints, freeSpeedGrid, sc,envelope).runAccessibilityComputation();
		
	}
	
	/**
	 * Parses all network nodes and classifies them into either exit road, dead end or redundant nodes.
	 * Currently only for nodes with degree = 1.
	 */
	private void checkNodeAttributes() {
		
		log.info("analysing network nodes...");
		
		int exit=0,red=0,de=0;
		
		this.nodeTypes.put(this.exit, new ArrayList<Id>());
		this.nodeTypes.put(this.deadEnd, new ArrayList<Id>());
		this.nodeTypes.put(this.redundant, new ArrayList<Id>());
		
		//iterate over all network nodes
		for(Node node : this.network.getNodes().values()){

			//if the node has any ingoing and outgoing links (at least one of a kind) investigate the node
			if(node.getInLinks().size()>0&&node.getOutLinks().size()>0){
				
				//catches nodes with high degrees (here: more than five in- or outgoing links)
				if(node.getInLinks().size()>5||node.getOutLinks().size()>5)
					this.nodesWithHighDegrees.add(node.getId());
					
				//if the node's degree equals 1, then classify it
				if(node.getInLinks().size()==1&&node.getOutLinks().size()==1){

					//get the in- and the outgoing link of the node
					Link inLink = node.getInLinks().values().iterator().next();
					Link outLink = node.getOutLinks().values().iterator().next();
					
					//if the only possible moving direction from this node is backwards
					//(to the node you came from) and it lies outside the bounding polygon
					//then it's a node of an exit road
					if(inLink.getFromNode().equals(outLink.getToNode())){
						if(!this.envelope.contains(new Point(new CoordinateArraySequence(new Coordinate[]{
								new Coordinate(node.getCoord().getX(),node.getCoord().getY())}),new GeometryFactory()))){
							this.nodeTypes.get(this.exit).add(node.getId());
							exit++;
							continue;
						}
						//if it's inside the polygon, it's a node of a dead end road
						this.nodeTypes.get(this.deadEnd).add(node.getId());
						de++;
						
					} else{
						//if all attributes of the ingoing link are equal to the
						//attributes of the outgoing link, then the node is redundant
						if(inLink.getCapacity()==outLink.getCapacity()&&
								inLink.getFreespeed()==outLink.getFreespeed()&&
								inLink.getNumberOfLanes()==outLink.getNumberOfLanes()&&
								inLink.getAllowedModes()==outLink.getAllowedModes()){
							this.nodeTypes.get(this.redundant).add(node.getId());
							red++;
						}
					}
				}
			}
		}
		
		log.info("found " + exit + " exit road nodes, " + de + " dead end nodes and " + red + " redundant nodes...");
		log.info("...done");
		
		this.nodesChecked = true;
		
	}
	
	/**
	 *Parses all network links to compute their geometric length (fromNode to toNode) and check if their storage capacity is sufficient for
	 *storing at least one vehicle (link length >= effektiveCellSize).  
	 */
	private void checkLinkAttributes() {
		
		log.info("analyzing network links...");
		
		//the space needed to store a vehicle (+ additional space for the gaps to the vehicle ahead and the one following
		double cellWidth = ((Network)this.network).getEffectiveCellSize();
		int writerIndex = 0;
		
		//iterate over all links
		for(Link link : this.network.getLinks().values()){
			//compute geometric length (distance between from and to node)
			double geometricLength = 
					Math.sqrt(Math.pow(link.getToNode().getCoord().getX() -
							link.getFromNode().getCoord().getX(),2) +
							Math.pow(link.getToNode().getCoord().getY() -
							link.getFromNode().getCoord().getY(), 2));
			
			this.geometricLengths.put(link.getId(), geometricLength);
			
			this.totalLength += link.getLength();
			this.totalGLength += geometricLength;
			//if the length of the link is less than the effectiveCellSize then store the link and write a warning about that later
			if(link.getLength()<cellWidth){
				this.lengthBelowStorageCapacity.add(link);
				writerIndex++;
			}
		}
		log.info(writerIndex + " warnings about storage capacity written...");
		log.info("...done");
		this.linksChecked = true;
	}
	
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
	
	private static DoubleFlagRole getDoubleFlag(final Node n, final Map<Node, DoubleFlagRole> nodeRoles) {
		DoubleFlagRole r = nodeRoles.get(n);
		if (null == r) {
			r = new DoubleFlagRole();
			nodeRoles.put(n, r);
		}
		return r;
	}
		
	static class DoubleFlagRole {
		protected boolean forwardFlag = false;
		protected boolean backwardFlag = false;
	}
	
	/**
	 * 
	 * Creates a grid layer with zones for accessibility computation.
	 * 
	 * @param gridSize The resolution of the grid (distance between measring points).
	 * @param boundingBox The corners of the bounding box for the given network.
	 * @return
	 */
	public static ZoneLayer<Id<Zone>> createGridLayerByGridSizeByNetwork(double gridSize, double [] boundingBox) {
		
//		log.info("Setting starting points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<Id<Zone>>> zones = new HashSet<>();

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
					
					Zone<Id<Zone>> zone = new Zone<Id<Zone>>(polygon);
					zone.setAttribute( Id.create( setPoints, Zone.class ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

//		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
//		log.info("Done with setting starting points!");
		
		ZoneLayer<Id<Zone>> layer = new ZoneLayer<>(zones);
		return layer;
	}
	
	/**
	 * Calls the writing methods for node statistics after the analysis and classification
	 * 
	 * @param outputFolder 
	 * @throws IOException
	 */
	private void writeNodesFiles(String outputFolder) throws IOException {
		
		if(this.nodesChecked){
			writeNodeStatisticsFile(outputFolder);
			exportNodesToShape(outputFolder);
		}
		
	}

	/**
	 * Writes the node statistics to a plain text file.
	 * @param outputFolder
	 * @throws IOException
	 */
	private void writeNodeStatisticsFile(String outputFolder) throws IOException {
		File file = new File(outputFolder+"nodeStatistics"+this.TXTfile);
		FileWriter writer = new FileWriter(file);;
		
		writer.write("ID\tinDegree\toutDegree");
			
		for(Node n : this.network.getNodes().values()){
				
			writer.write("\n"+n.getId()+"\t"+n.getInLinks().size()+"\t"+n.getOutLinks().size());
				
		}
		writer.flush();
		writer.close();
	}
	
	/**
	 * Writes the classified nodes to a shape file for visualisation in QGis.
	 * 
	 * @param outputFolder
	 */
	private void exportNodesToShape(String outputFolder){
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("node");
		typeBuilder.add("location",Point.class);
		typeBuilder.add("ID",String.class);
		typeBuilder.add("type",String.class);
		typeBuilder.setCRS(MGC.getCRS(this.targetCoordinateSystem));
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(String nodeType : this.nodeTypes.keySet()){
			for(Id nodeId : this.nodeTypes.get(nodeType)){

				Point p = MGC.coord2Point(this.network.getNodes().get(nodeId).getCoord());
				features.add(this.builder.buildFeature(null, new Object[]{p,nodeId.toString(),nodeType}));
				
			}
			
		}
		
		String destination = "";
		
		if(features.size() > 0){
		destination = "nodeTypes";
		
		this.filesForExportInQGisProject.put(destination,Point.class);
		
			ShapeFileWriter.writeGeometries(features, outputFolder+destination+this.SHPfile);
		}
		
		if(!(this.nodesWithHighDegrees.size() < 1)){
			features.clear();
			
			typeBuilder.setName("node");
			typeBuilder.add("location",Point.class);
			typeBuilder.add("ID",String.class);
			typeBuilder.add("in-degree",Integer.class);
			typeBuilder.add("out-degree",Integer.class);
			typeBuilder.setCRS(MGC.getCRS(this.targetCoordinateSystem));
			this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
			for(Id nodeId : this.nodesWithHighDegrees){
			
				Point p = MGC.coord2Point(this.network.getNodes().get(nodeId).getCoord());
					features.add(this.builder.buildFeature(null, new Object[]{p,nodeId.toString(),
							this.network.getNodes().get(nodeId).getInLinks().size(),
							this.network.getNodes().get(nodeId).getOutLinks().size()}));
			}
		
			destination = "highDegreeNodes";
		
			this.filesForExportInQGisProject.put(destination, Point.class);
			ShapeFileWriter.writeGeometries(features, outputFolder+destination+this.SHPfile);
		}
		
	}

	/**
	 * Calls the writing methods for link statistics after analysis.
	 * 
	 * @param outputFolder
	 * @throws IOException
	 */
	private void writeLinkFiles(String outputFolder) throws IOException {
		
		if(this.linksChecked){
			writeLinkStatisticsFile(outputFolder);
			writeStorageCapacityWarningFile(outputFolder);
		}
		writeClustersAndNetwork2ESRIShape(outputFolder);
		
	}

	/**
	 * Writes a logfile with all warnings about links with insufficient storage capacity (link length < effectiveCellSize). 
	 * 
	 * @param outputFolder
	 * @throws IOException
	 */
	private void writeStorageCapacityWarningFile(String outputFolder) throws IOException {
		File file = new File(outputFolder+"storageCapacityWarning"+this.TXTfile);
		FileWriter writer;

		double cellWidth = ((Network)this.network).getEffectiveCellSize();
		
			writer = new FileWriter(file);
			for(Link link : this.lengthBelowStorageCapacity){
				writer.write("length of link " + link.getId() + " below min length for storage capacity of one vehicle (" +
						link.getLength() + "m instead of "+ cellWidth +" m)\n");
			}
			writer.flush();
			writer.close();
	}
	
	/**
	 * Writes the link statistics (length, geometric length, number of lanes, capacity) to a plain text file.
	 * 
	 * @param outputFolder
	 * @throws IOException
	 */
	private void writeLinkStatisticsFile(String outputFolder) throws IOException{
		
		File file = new File(outputFolder+"linkStatistics"+this.TXTfile);
		
			
			FileWriter writer = new FileWriter(file);
			
			writer.write("ID \t length \t geometricLength \t nlanes \t capacity");
			
			for(Link l : this.network.getLinks().values()){
				
				writer.write("\n"+ l.getId() +
						"\t"+l.getLength() +
						"\t" + this.geometricLengths.get(l.getId()) +
						"\t" + l.getNumberOfLanes() +
						"\t" + l.getCapacity());
				
			}
			writer.flush();
			writer.close();
			
	}
	
	/**
	 * Exports the network links and (if existing) the links contained in small clusters to an ESRI shapefile.
	 * @param outputFolder
	 */
	private void writeClustersAndNetwork2ESRIShape(String outputFolder) {
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",LineString.class);
		typeBuilder.add("ID",String.class);
		typeBuilder.add("length",Double.class);
		typeBuilder.add("freespeed",Double.class);
		typeBuilder.add("capacity",Double.class);
		typeBuilder.add("nlanes", String.class);
		typeBuilder.setCRS(MGC.getCRS(this.targetCoordinateSystem));
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(Link link : this.network.getLinks().values()){
			
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
		
		ShapeFileWriter.writeGeometries(features, outputFolder+destination+this.SHPfile);
		
		if(this.smallClusterLinks.size()>0){
			
			features.clear();
			
			for(Link link : this.smallClusterLinks.values()){
				
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
			
			ShapeFileWriter.writeGeometries(features, outputFolder+destination+this.SHPfile);
			
		}
	}
	
	private void writeAccessibilityMap(String outputFolder){
		
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFolder+"/freeSpeedAccessibility.txt"));
			
			for(double x = this.freeSpeedGrid.getXmin(); x <= this.freeSpeedGrid.getXmax(); x += this.freeSpeedGrid.getResolution()) {
				writer.write(SpatialGridTableWriter.separator);
				writer.write(String.valueOf(x));
			}
			writer.newLine();
			
			for(double y = this.freeSpeedGrid.getYmin(); y <= this.freeSpeedGrid.getYmax() ; y += this.freeSpeedGrid.getResolution()) {
				writer.write(String.valueOf(y));
				for(double x = this.freeSpeedGrid.getXmin(); x <= this.freeSpeedGrid.getXmax(); x += this.freeSpeedGrid.getResolution()) {
					writer.write(SpatialGridTableWriter.separator);
					Double val = this.freeSpeedGrid.getValue(x, y);
					if(!Double.isNaN(val))
						writer.write(String.valueOf(val));
					else
						writer.write("NaN");
				}
				writer.newLine();
			}
			writer.flush();
			writer.close();
			}
			catch(IOException e){
				e.printStackTrace();
			}
		
	}
	
}
