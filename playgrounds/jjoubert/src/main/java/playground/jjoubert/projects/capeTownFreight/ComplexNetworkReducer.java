/**
 * 
 */
package playground.jjoubert.projects.capeTownFreight;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.opengis.feature.simple.SimpleFeature;

import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;
import playground.nmviljoen.network.salience.SampleNetworkBuilder;
import playground.southafrica.projects.complexNetworks.pathDependence.DigicorePathDependentNetworkReader_v2;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork;
import playground.southafrica.projects.complexNetworks.pathDependence.PathDependentNetwork.PathDependentNode;
import playground.southafrica.utilities.Header;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;

/**
 * Class to read in a {@link PathDependentNetwork} and only keep those nodes
 * that are associated with (via a link) or inside a given {@link Geometry}.
 * 
 * @author jwjoubert
 */
public class ComplexNetworkReducer {
	final private static Logger LOG = Logger.getLogger(ComplexNetworkReducer.class);
	private static Geometry CAPETOWN = null;
	private static Geometry CAPETOWN_ENVELOPE = null;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ComplexNetworkReducer.class.toString(), args);
		String networkFile = args[0];
		String shapefile = args[1];
		String output = args[2];
		String degreeFile = args[3];
		int numberOfThreads = Integer.parseInt(args[4]);
		
		/* Read complex network. */
		DigicorePathDependentNetworkReader_v2 nr = new DigicorePathDependentNetworkReader_v2();
		nr.parse(networkFile);
		PathDependentNetwork network = nr.getPathDependentNetwork();
		network.writeNetworkStatisticsToConsole();
		
		/* Read Cape Town */
		ShapeFileReader ctReader = new ShapeFileReader();
		ctReader.readFileAndInitialize(shapefile);
		SimpleFeature ctFeature = ctReader.getFeatureSet().iterator().next(); /* Just get the first one. */
		if(ctFeature.getDefaultGeometry() instanceof MultiPolygon){
			CAPETOWN = (MultiPolygon)ctFeature.getDefaultGeometry();
			CAPETOWN_ENVELOPE = CAPETOWN.getEnvelope();
		}
		
		ComplexNetworkReducer cnr = new ComplexNetworkReducer();
		Map<Id<Node>, Map<Id<Node>, Double>> map = cnr.cleanUp(network);
		Graph<NmvNode, NmvLink> graph = cnr.convertToGraph(network, map);
		cnr.writeGraphStatistics(graph);
		cnr.writeDegreeStats(graph, degreeFile);
		SampleNetworkBuilder.writeGraphML(graph, output);
		
		Header.printFooter();
	}

	
	public ComplexNetworkReducer() {
		
	}
	
	public void writeDegreeStats(Graph<NmvNode, NmvLink> graph, String filename){
		LOG.info("Writing degree values to file.");
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("NodeId,Lon,Lat,X,Y,InDegree,OutDegree,Degree");
			bw.newLine();

			Iterator<NmvNode> it = graph.getVertices().iterator();
			while(it.hasNext()){
				NmvNode node = it.next();
				Coord c = new Coord(node.X, node.Y);
				Coord wgs84 = ct.transform(c);
				int inDegree = graph.getInEdges(node).size();
				int outDegree = graph.getOutEdges(node).size();
				bw.write(String.format("%s,%.6f,%.6f,%.0f,%.0f,%d,%d,%d\n", 
						node.id, 
						wgs84.getX(), 
						wgs84.getY(),
						c.getX(),
						c.getY(),
						inDegree,
						outDegree,
						inDegree+outDegree));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
		LOG.info("Done writing degree values to file.");
	}
	
	public void writeGraphStatistics(Graph<NmvNode, NmvLink> graph){
		LOG.info("==============  Graph statistics  ================");
		LOG.info(" Number of nodes: " + graph.getVertexCount());
		LOG.info(" Number of edges: " + graph.getEdgeCount());
		
		/* Get maximum edge weight. */
		double max = Double.NEGATIVE_INFINITY;
		Iterator<NmvLink> it = graph.getEdges().iterator();
		while(it.hasNext()){
			NmvLink link = it.next();
			max = Math.max(max, link.getWeight());
		}
		LOG.info(" Maximum edge weight: " + max);
		LOG.info("==================================================");
	}
	
	public Graph<NmvNode, NmvLink> convertToGraph(PathDependentNetwork network, Map<Id<Node>, Map<Id<Node>, Double>> map){
		LOG.info("Converting map to formal graph...");
		
		Map<Id<Node>, NmvNode> nodeMap = new HashMap<>();
		
		
		Graph<NmvNode, NmvLink> graph = new DirectedSparseGraph<NmvNode, NmvLink>();
		for(Id<Node> o : map.keySet()){
			Map<Id<Node>, Double> thisMap = map.get(o);
			for(Id<Node> d : thisMap.keySet()){
				
				/* Create the nodes, but only if they don't exist yet! */
				NmvNode n1;
				if(!nodeMap.containsKey(o)){
					n1 = new NmvNode(o.toString(), 
							o.toString(), 
							network.getPathDependentNode(o).getCoord().getX(),
							network.getPathDependentNode(o).getCoord().getY());
					nodeMap.put(o, n1);
					graph.addVertex(n1);
				} else{
					n1 = nodeMap.get(o);
				}

				NmvNode n2;
				if(!nodeMap.containsKey(d)){
					n2 = new NmvNode(d.toString(), 
							d.toString(), 
							network.getPathDependentNode(d).getCoord().getX(),
							network.getPathDependentNode(d).getCoord().getY());
					nodeMap.put(d, n2);
					graph.addVertex(n2);
				} else{
					n2 = nodeMap.get(d);
				}
				double weight = thisMap.get(d);
				
				NmvLink l = new NmvLink(o.toString() + "_" + d.toString(), weight);
				graph.addEdge(l, n1, n2);
			}
		}
		LOG.info("Done converting to graph.");
		return graph;
	}
	
	public Map<Id<Node>, Map<Id<Node>, Double>> cleanUp(PathDependentNetwork network){
		LOG.info("Cleaning up the path dependent network.");
		GeometryFactory gf = new GeometryFactory();
		
		Map<Id<Node>, Map<Id<Node>, Double>> edges = network.getEdges();
		LOG.info("Total number of origin nodes to consider: " + edges.size());
		Map<Id<Node>, Map<Id<Node>, Double>> keepers = new HashMap<Id<Node>, Map<Id<Node>,Double>>(edges.size());
		
		Counter counter = new Counter("  origin nodes # ");
		for(Id<Node> o : edges.keySet()){
			Map<Id<Node>, Double> map = edges.get(o);

			PathDependentNode n1 = network.getPathDependentNode(o);
			Point p1 = gf.createPoint(new Coordinate(n1.getCoord().getX(), n1.getCoord().getY()));
			boolean is1Inside = false;
			if(CAPETOWN_ENVELOPE.contains(p1)){
				if(CAPETOWN.contains(p1)){
					is1Inside = true;
				}
			}
			
			if(is1Inside){
				if(!keepers.containsKey(o)){
					keepers.put(o, map);
				} else{
					Map<Id<Node>, Double> existingMap = keepers.get(o);
					for(Id<Node> d : map.keySet()){
						if(!existingMap.containsKey(d)){
							existingMap.put(d, map.get(d));
						} else{
							double oldValue = map.get(d);
							existingMap.put(d, oldValue + map.get(d));
						}
					}
				}
			} else{
				for(Id<Node> d : map.keySet()){
					PathDependentNode n2 = network.getPathDependentNode(d);
					
					Point p2 = gf.createPoint(new Coordinate(n2.getCoord().getX(), n2.getCoord().getY()));
					boolean is2Inside = false;
					if(CAPETOWN_ENVELOPE.contains(p2)){
						if(CAPETOWN.contains(p2)){
							is2Inside = true;
						}
					}
					
					if(is2Inside){
						if(!keepers.containsKey(o)){
							keepers.put(o, new HashMap<Id<Node>, Double>());
							keepers.get(o).put(d, map.get(d));
						} else{
							Map<Id<Node>, Double> existingMap = keepers.get(o);
							if(!existingMap.containsKey(d)){
								existingMap.put(d, map.get(d));
							} else{
								double oldValue = existingMap.get(d);
								existingMap.put(d, oldValue + map.get(d));
							}
						}
					}
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		return keepers;
	}
}
