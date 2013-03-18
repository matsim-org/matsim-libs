package playground.dhosse.bachelorarbeit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.matsim4opus.gis.GridUtils;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;
import org.matsim.contrib.matsim4opus.gis.ZoneLayer;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;

public class NetworkInspector {//TODO pfade ändern
	
	private static Scenario scenario = null;
	
	private Map<Id,Double> geometricLengths = new HashMap<Id,Double>();
	private Map<Id,ArrayList<Activity>> mappedPopulation = new HashMap<Id,ArrayList<Activity>>();
	private Map<Id,String> nodeTypes = new HashMap<Id,String>();
	
	private List<Link> lengthBelowStorageCapacity = new ArrayList<Link>();
	
	private List<Node> deadEndNodes = new ArrayList<Node>();
	private List<Node> exitRoadNodes = new ArrayList<Node>();
	private List<Node> redundantNodes = new ArrayList<Node>();
	
	private Logger logger = Logger.getLogger(NetworkInspector.class);
	
	private double[] inDegrees = new double[7];
	private double[] outDegrees = new double[7];
	
	private double totalLength = 0;
	private double totalGLength = 0;
	
	private String outputFolder = "C:/Users/Daniel/Dropbox/bsc";
	
	private SimpleFeatureBuilder builder;
	
	private Geometry envelope;
	
	/**
	 * 
	 * @param args (i) network file (ii) population file (optional)
	 */
	public static void main(String args[]){
		
		NetworkInspector.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig()); 
		
		if(args.length==1)
			new MatsimNetworkReader(NetworkInspector.scenario).readFile(args[0]);
		else if(args.length==2){
			new MatsimNetworkReader(NetworkInspector.scenario).readFile(args[0]);
			new MatsimPopulationReader(NetworkInspector.scenario).readFile(args[1]);
		}
		
		new NetworkInspector().run();
		
	}
	
	public NetworkInspector(){
		
		this.envelope = MinimumEnvelope.main(NetworkInspector.scenario.getNetwork()).buffer(-10);
		
	}
	
	private void run() {
		
		boolean routable = isRoutable();
		checkNodeAttributes();
		checkLinkAttributes();
		
		if(!(this.nodeTypes.size()<1))
			exportNodesToShape();
		
		if(!(NetworkInspector.scenario.getPopulation().getPersons().size()<1))
			populationLinking();
		
//		if(!routable)
//			new NetworkCleaner().run(NetworkInspector.scenario.getNetwork());
		
		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
		bbox.setDefaultBoundaryBox(NetworkInspector.scenario.getNetwork());
		
		ZoneLayer<Id> measuringPoints = GridUtils.createGridLayerByGridSizeByNetwork(100, bbox.getBoundingBox());
		SpatialGrid freeSpeedGrid = new SpatialGrid(bbox.getBoundingBox(), 100);
		ScenarioImpl sc = (ScenarioImpl) NetworkInspector.scenario;
		
		new AccessibilityCalcV2(measuringPoints, freeSpeedGrid, sc).runAccessibilityComputation();
		
	}

	private boolean isRoutable(){
		
		//aus klasse "NetworkCleaner"
		final Map<Id,Node> visitedNodes = new TreeMap<Id,Node>();
		Map<Id,Node> biggestCluster = new TreeMap<Id,Node>();
		
		Map<Id,Node> smallClusterNodes = new TreeMap<Id,Node>();
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
				} else {
					smallClusterNodes.putAll(cluster); //eigener teil
				}
				if(biggestCluster.size()==NetworkInspector.scenario.getNetwork().getNodes().size()){
					logger.info("size of the biggest cluster equals network size...");
					logger.info("network is routable.");
					return true;
				}
			}
		}
		
		//eigener teil		
		logger.warn("size of the biggest cluster is " + biggestCluster.size() +
				" but network contains " + NetworkInspector.scenario.getNetwork().getNodes().size() + " nodes...");
		logger.warn("network is not routable");
		
		for(Id nodeId : smallClusterNodes.keySet()){
			
			Node node = NetworkInspector.scenario.getNetwork().getNodes().get(nodeId);
			
			for(Link l : node.getOutLinks().values()){
				if(!smallClusterLinks.containsKey(l.getId()))
					smallClusterLinks.put(l.getId(), l);
			}
			
		}
		
		logger.info("writing small clusters into ESRI shapefile...");
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("link",LineString.class);
		typeBuilder.add("ID",String.class);
		typeBuilder.add("length",Double.class);
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(Link link : smallClusterLinks.values()){
			
			SimpleFeature feature = this.builder.buildFeature(null, new Object[]{
					new GeometryFactory().createLineString(new Coordinate[]{
							new Coordinate(link.getFromNode().getCoord().getX(),link.getFromNode().getCoord().getY()),
							new Coordinate(link.getToNode().getCoord().getX(),link.getToNode().getCoord().getY())
					}),
					link.getId(),
					link.getLength()
			});
			
			features.add(feature);
			
		}
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Dropbox/bsc/output/smallClusters.shp");		
		
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
	
	//eigene methode
	private void checkLinkAttributes() {
		
		logger.info("checking link attributes...");
		
		int writerIndex = 0;
		
		for(Link link : NetworkInspector.scenario.getNetwork().getLinks().values()){
			double geometricLength = 
					Math.sqrt(Math.pow(link.getToNode().getCoord().getX() -
							link.getFromNode().getCoord().getX(),2) +
							Math.pow(link.getToNode().getCoord().getY() -
							link.getFromNode().getCoord().getY(), 2));
			
			this.geometricLengths.put(link.getId(), geometricLength);
			
			this.totalLength += link.getLength();
			this.totalGLength += geometricLength;
			
			if(link.getLength()<7||geometricLength<7){
				this.lengthBelowStorageCapacity.add(link);
				writerIndex++;
			}
					
		}
		
		
		logger.info("done.");
		
		logger.info("writing link statistics files...");
		
		createLinkStatisticsFile();
		
		File file = new File(this.outputFolder+"/test/lengthBelowStorageCapacity.txt");
		FileWriter writer;
		
		try {
			
			writer = new FileWriter(file);
			for(Link link : this.lengthBelowStorageCapacity){
				writer.write("length of link " + link.getId() + " below min length for storage capacity of one vehicle (" +
						link.getLength() + "m instead of 7 m)\n");
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
		
		for(Node node : NetworkInspector.scenario.getNetwork().getNodes().values()){
			
			if(node.getInLinks().size()>0&&node.getOutLinks().size()>0){
			
				this.inDegrees[node.getInLinks().size()-1]++;
				this.outDegrees[node.getOutLinks().size()-1]++;
				
				if(node.getInLinks().size()==1&&node.getOutLinks().size()==1){
				
					Link inLink = node.getInLinks().values().iterator().next();
					Link outLink = node.getOutLinks().values().iterator().next();
				
					if(inLink.getFromNode().equals(outLink.getToNode())){
						if(this.envelope.contains(new Point(new CoordinateArraySequence(new Coordinate[]{
								new Coordinate(node.getCoord().getX(),node.getCoord().getY())}),new GeometryFactory()))){
							this.deadEndNodes.add(node);
							this.nodeTypes.put(node.getId(), "deadEnd");
						}
						else{
							this.exitRoadNodes.add(node);
							this.nodeTypes.put(node.getId(), "exit");
						}
					} else{
						if(inLink.getCapacity()==outLink.getCapacity()&&
								inLink.getFreespeed()==outLink.getFreespeed()&&
								inLink.getNumberOfLanes()==outLink.getNumberOfLanes()&&
								inLink.getAllowedModes()==outLink.getAllowedModes()){
							this.redundantNodes.add(node);
							this.nodeTypes.put(node.getId(), "redundant");
						}
					}
				}
			}
			
		}
		
		logger.info("done.");
		
		logger.info(this.deadEndNodes.size() + " dead end nodes, "
				+ this.exitRoadNodes.size() + " exit road nodes and " + this.redundantNodes.size() + " redundant nodes found.");
		
		logger.info("writing node statistics files...");
		
		
		createNodeStatisticsFile();
		
		logger.info("done.");
		
	}
	
	//eigene methode
	private void populationLinking(){
		
		logger.info("checking where the population is linked on the network...");
		
		for(Person p : NetworkInspector.scenario.getPopulation().getPersons().values()){
			
			Iterator<PlanElement> planElementIterator = p.getSelectedPlan().getPlanElements().iterator();
			
			while(planElementIterator.hasNext()){
				
				PlanElement pe = planElementIterator.next();
				
				if(pe instanceof Activity){
					
					Coord fromCoord = ((Activity)pe).getCoord();
					
					Id toLinkId = ((NetworkImpl)NetworkInspector.scenario.getNetwork()).getNearestLink(fromCoord).getId();
					
					if(!this.mappedPopulation.containsKey(toLinkId))
						this.mappedPopulation.put(toLinkId, new ArrayList<Activity>());
					
					this.mappedPopulation.get(toLinkId).add((Activity)pe);
					
				}
				
			}
			
		}
		
		logger.info("writing pointers from activities to nearest links...");
		
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("shape");
		typeBuilder.add("pointer",LineString.class);
		typeBuilder.add("ID",String.class);
		typeBuilder.add("LAENGE",Double.class);
		this.builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
		
		ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		Iterator<Id> keyIterator = this.mappedPopulation.keySet().iterator();
		
		GeometryFactory gf = new GeometryFactory();
		
		while(keyIterator.hasNext()){
			
			Id linkId = keyIterator.next();
			Link link = NetworkInspector.scenario.getNetwork().getLinks().get(linkId);
			
			ArrayList<Activity> activities = this.mappedPopulation.get(linkId);
			
			int i = 0;
			
			for(Activity act : activities){
				
				double length = NetworkUtil.getEuclidianDistance(act.getCoord(), link.getCoord());
				
				SimpleFeature feature = this.builder.buildFeature(null, new Object[]{
						gf.createLineString(new Coordinate[]{
								new Coordinate(act.getCoord().getX(),act.getCoord().getY()),
								new Coordinate(link.getCoord().getX(),link.getCoord().getY())
						}),
						new IdImpl(linkId+"_"+Integer.toString(i)).toString(),
						length
				});
				
				features.add(feature);
				i++;
				
			}
			
		}
		
		logger.info("writing pointers from activities to nearest links to ESRI shapefile...");
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Dropbox/bsc/output/mapping/pointers.shp");
		
		logger.info("...done.");
		
	}
	
	private void createLinkStatisticsFile(){
		
		File file = new File(this.outputFolder+"/test/linkStatistics.txt");
		
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
		
		File file = new File(this.outputFolder+"/test/nodeStatistics.txt");
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
		
		ShapeFileWriter.writeGeometries(features, "C:/Users/Daniel/Dropbox/bsc/pres/nodeTypes.shp");
		
	}
	
	private static DoubleFlagRole getDoubleFlag(final Node n, final Map<Node, DoubleFlagRole> nodeRoles) {
		DoubleFlagRole r = nodeRoles.get(n);
		if (null == r) {
			r = new DoubleFlagRole();
			nodeRoles.put(n, r);
		}
		return r;
	}
	
	public Geometry getArea() {
		return envelope;
	}

	public void setArea(Geometry area) {
		this.envelope = area;
	}

	static class DoubleFlagRole {
		protected boolean forwardFlag = false;
		protected boolean backwardFlag = false;
	}
	
}
