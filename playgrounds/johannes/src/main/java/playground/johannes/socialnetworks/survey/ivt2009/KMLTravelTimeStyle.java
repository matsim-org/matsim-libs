package playground.johannes.socialnetworks.survey.ivt2009;

import gnu.trove.TDoubleObjectHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import net.opengis.kml._2.LinkType;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

import playground.johannes.socialnetworks.graph.spatial.SpatialGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialSparseGraph;
import playground.johannes.socialnetworks.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.graph.spatial.io.KMLVertexColorStyle;
import playground.johannes.socialnetworks.graph.spatial.io.KMLWriter;
import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;

public class KMLTravelTimeStyle extends KMLVertexColorStyle<Graph, Vertex> {
	
	private static final Logger logger = Logger.getLogger(KMLTravelTimeStyle.class);

	private TObjectIntHashMap<Vertex> vertexValues = new TObjectIntHashMap<Vertex>();
	
	public KMLTravelTimeStyle(LinkType vertexIconLink) {
		super(vertexIconLink);
	}

	@Override
	protected double getValue(Vertex vertex) {
		return vertexValues.get(vertex);
	}

	@Override
	protected TDoubleObjectHashMap<String> getValues(Graph graph) {
		TDoubleObjectHashMap<String> styles = new TDoubleObjectHashMap<String>();
		try {
		/*
		 * read network file
		 */
		NetworkLayer network = new NetworkLayer();
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(network);
		reader.parse("/Users/fearonni/vsp-work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-changed-with-GTF.xml");
		/*
		 * read travel time matrix
		 */
		logger.info("Loading travel time matrix...");
		TObjectIntHashMap<Node> node2Idx = new TObjectIntHashMap<Node>();
		
		BufferedReader matrixReader = new BufferedReader(new FileReader("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/ttmatrix.real3.txt"));
		String line = matrixReader.readLine();
		String[] tokens = line.split("\t");
		for(int i = 1; i < tokens.length; i++) {
			String id = tokens[i];
			Node node = network.getNode(id);
			node2Idx.put(node, i-1);
		}
		
		int[][] ttmatrix = new int[node2Idx.size()][node2Idx.size()];
		Set<Node> nodes = new HashSet<Node>();
		while((line = matrixReader.readLine()) != null) {
			tokens = line.split("\t");
			String id = tokens[0];
			Node node = network.getNode(id);
			nodes.add(node);
			int i = node2Idx.get(node);
			for(int k = 1; k < tokens.length; k++) {
				int tt = Integer.parseInt(tokens[k]);
				ttmatrix[k-1][i] = tt;
			}
		}
		/*
		 * get travel times
		 */
		logger.info("Calculating travel times...");
		Vertex source = graph.getVertices().iterator().next();
		Node sourceNode = getNearestNode(((SpatialVertex) source).getCoordinate(), nodes);
		int i = node2Idx.get(sourceNode);
		
		int maxTT = 0;
		for(Vertex target : graph.getVertices()) {
			Node tragetNode = getNearestNode(((SpatialVertex) target).getCoordinate(), nodes);
			int j = node2Idx.get(tragetNode);
			
			int tt = ttmatrix[i][j];
			
			if(tt < Integer.MAX_VALUE) {
				maxTT = Math.max(maxTT, tt);
			tt = (int)Math.ceil(tt/120.0);
			styles.put(tt, "vertex.style."+tt);
			vertexValues.put(target, tt);
			}
		}
		System.err.println("Max tt = " + maxTT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return styles;
	}
	
	private static Node getNearestNode(Coord c, Set<Node> nodes) {
		Node theNode = null;
		double d_min = Double.MAX_VALUE; 
		for(Node node : nodes) {
			Coord c_n = node.getCoord();
			double d = CoordUtils.calcDistance(c, c_n);
			if(d < d_min) {
				theNode = node;
				d_min = d;
			}
		}
		
		return theNode;
	}
	
	public static void main(String args[]) throws IOException {
		SpatialGraph graph = new Population2SpatialGraph().read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.001.xml");
		
		KMLWriter writer = new KMLWriter();
		writer.setCoordinateTransformation(new CH1903LV03toWGS84());
		writer.setDrawEdges(false);
		writer.setDrawNames(false);
		writer.setEdgeDescriptor(null);
		writer.setVertexStyle(new KMLTravelTimeStyle(writer.getVertexIconLink()));
		writer.write((SpatialSparseGraph) graph, "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/analysis/tmp/traveltimes.kmz");
	}

}
