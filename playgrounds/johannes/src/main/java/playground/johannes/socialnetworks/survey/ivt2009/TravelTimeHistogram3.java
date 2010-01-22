package playground.johannes.socialnetworks.survey.ivt2009;

import gnu.trove.TIntIntHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.graph.Vertex;
import org.matsim.contrib.sna.graph.spatial.SpatialGraph;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;
import org.matsim.contrib.sna.math.Distribution;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseGraph;
import org.matsim.contrib.sna.snowball.spatial.SampledSpatialSparseVertex;
import org.matsim.contrib.sna.snowball.spatial.io.SampledSpatialGraphMLReader;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.utils.geometry.CoordUtils;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.spatial.io.Population2SpatialGraph;
import playground.johannes.socialnetworks.snowball2.SnowballPartitions;
import playground.johannes.socialnetworks.spatial.ZoneLayerLegacy;

public class TravelTimeHistogram3 {
	
	private static final Logger logger = Logger.getLogger(TravelTimeHistogram3.class);
	
	private static final double normDescretization = 60.0;
	
	private static final double descretization = 60.0;
	

	public static void main(String[] args) throws SAXException, ParserConfigurationException, IOException {
		/*
		 * read network file
		 */
		ScenarioImpl scenario = new ScenarioImpl();
		NetworkLayer network = scenario.getNetwork();
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario);
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
			Node node = network.getNodes().get(new IdImpl(id));
			node2Idx.put(node, i-1);
		}
		
		int[][] ttmatrix = new int[node2Idx.size()][node2Idx.size()];
		Set<Node> nodes = new HashSet<Node>();
		while((line = matrixReader.readLine()) != null) {
			tokens = line.split("\t");
			String id = tokens[0];
			Node node = network.getNodes().get(new IdImpl(id));
			nodes.add(node);
			int i = node2Idx.get(node);
			for(int k = 1; k < tokens.length; k++) {
				int tt = Integer.parseInt(tokens[k]);
				ttmatrix[k-1][i] = tt;
			}
		}
		/*
		 * read graph
		 */
		SampledSpatialGraphMLReader graphReader = new SampledSpatialGraphMLReader();
		SampledSpatialSparseGraph graph = graphReader.readGraph("/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/graph/graph.graphml");
		/*
		 * read swiss boundary
		 */
		logger.info("Reading boundaries...");
		ZoneLayerLegacy boundaries = ZoneLayerLegacy.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/gemeindegrenzen2008.zip Folder/g1g08_shp_080606.zip Folder/G1L08.shp");
		/*
		 * create partition
		 */
		Set<? extends SampledSpatialSparseVertex> vertices = SnowballPartitions.createSampledPartition(graph.getVertices());
		/*
		 * cache nearest nodes
		 */
		Map<Vertex, Node> nearestNodes = new HashMap<Vertex, Node>();
		for(SampledSpatialSparseVertex v : vertices) {
			nearestNodes.put(v, getNearestNode(v.getCoordinate(), nodes));
		}
		TObjectIntHashMap<Vertex> vertex2Idx = new TObjectIntHashMap<Vertex>();
		for(SampledSpatialSparseVertex v : vertices) {
			Node n = nearestNodes.get(v);
			int i = node2Idx.get(n);
			vertex2Idx.put(v, i);
		}
		/*
		 * make normalization 
		 */
		int count = 0;
		SpatialGraph g2 = new Population2SpatialGraph(CRSUtils.getCRS(21781)).read("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/plans/plans.0.02.xml");
		logger.info("Caching nearest nodes...");
		for(Object v : g2.getVertices()) {
			Node n = getNearestNode(((SpatialVertex) v).getCoordinate(), nodes);
			nearestNodes.put((Vertex) v, n);
			int i = node2Idx.get(n);
			vertex2Idx.put((Vertex) v, i);
			
			count++;
			if(count % 1000 == 0) {
				logger.info(String.format("Processed %1$s of %2$s vertices. (%3$s)", count, g2.getVertices().size(), count/(float)g2.getVertices().size()));
			}
		}
		logger.info("Calculating normalization contants...");
		count = 0;
		Map<Vertex, TIntIntHashMap> numNodes_i = new HashMap<Vertex, TIntIntHashMap>();
		for(SampledSpatialSparseVertex v : vertices) {
			if(boundaries.getZone(v.getCoordinate()) != null) {
				TIntIntHashMap n_tt = new TIntIntHashMap();
				
				for (Object v_j : g2.getVertices()) {
					int i = vertex2Idx.get(v);
					int j = vertex2Idx.get((Vertex) v_j);

					int tt = ttmatrix[i][j];
					tt = (int) Math.ceil(tt / normDescretization);

					n_tt.adjustOrPutValue(tt, 1, 1);
				}
				
				numNodes_i.put(v, n_tt);
				
				count++;
				if(count % 10 == 0) {
					logger.info(String.format("Processed %1$s of %2$s vertices. (%3$s)", count, vertices.size(), count/(float)vertices.size()));
				}
			}
		}
		/*
		 * make histogram
		 */
		logger.info("Making histogram...");
		Distribution distr = new Distribution();
		Distribution distrNorm = new Distribution();
		
		count = 0;
		int samenode = 0;
		for(SampledSpatialSparseVertex v : vertices) {
			if(boundaries.getZone(v.getCoordinate()) != null) {
				TIntIntHashMap n_tt = numNodes_i.get(v);
				for(Object v_j : v.getNeighbours()) {
					if(boundaries.getZone(((SpatialVertex) v_j).getCoordinate()) != null) {
//						Coord c_i = v.getCoordinate();
//						Coord c_j = ((SpatialVertex) v_j).getCoordinate();
						
						Node n_i = nearestNodes.get(v);
						Node n_j = nearestNodes.get(v_j);
						
						int i = vertex2Idx.get(v);
						int j = vertex2Idx.get((Vertex) v_j);
						
						if(i == j)
							samenode++;
						
						int tt = ttmatrix[i][j];
						if(tt == 0 && i != j) {
							Link alink = null;
							for(Link link : n_i.getOutLinks().values()) {
								if(link.getToNode().equals(n_j)) {
									alink = link;
									break;
								}
							}
							logger.warn("Zero travel time!");
						}
						distr.add(tt);
						
						int bin = (int)Math.ceil(tt/normDescretization);
						int n = n_tt.get(bin);
						if(n == 0)
							logger.warn("No samples in tt bin " + tt);
//						n = Math.max(1, n);
						else
							distrNorm.add(tt, 1/(double)n);
					}
				}
			}
			count++;
			if(count % 10 == 0) {
				logger.info(String.format("Processed %1$s of %2$s vertices. (%3$s)", count, vertices.size(), count/(float)vertices.size()));
			}
		}
		
		logger.warn(String.format("%1$s edges do map to the same node.", samenode));
		Distribution.writeHistogram(distr.absoluteDistribution(descretization), "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/analysis/tmp/traveltime4.txt");
		Distribution.writeHistogram(distr.absoluteDistributionLog2(descretization), "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/analysis/tmp/traveltime4.log2.txt");
		Distribution.writeHistogram(distrNorm.absoluteDistribution(descretization), "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/analysis/tmp/traveltime.norm.txt");
		Distribution.writeHistogram(distrNorm.absoluteDistributionLog2(descretization), "/Users/fearonni/vsp-work/work/socialnets/data/ivt2009/analysis/tmp/traveltime.norm.log2.txt");
		logger.info("Done.");
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

}
