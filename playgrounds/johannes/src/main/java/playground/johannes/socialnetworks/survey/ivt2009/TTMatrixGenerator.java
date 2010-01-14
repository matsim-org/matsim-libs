package playground.johannes.socialnetworks.survey.ivt2009;

import gnu.trove.TIntArrayList;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sna.graph.Graph;
import org.matsim.contrib.sna.graph.SparseGraphBuilder;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.SAXException;

import playground.johannes.socialnetworks.graph.Partitions;
import playground.johannes.socialnetworks.graph.matrix.EdgeWeight;
import playground.johannes.socialnetworks.graph.matrix.SparseMatrix;
import playground.johannes.socialnetworks.graph.matrix.WeightedDijkstra;
import playground.johannes.socialnetworks.graph.mcmc.AdjacencyMatrix;
import playground.johannes.socialnetworks.spatial.ZoneLayerLegacy;

public class TTMatrixGenerator {

	private static final Logger logger = Logger.getLogger(TTMatrixGenerator.class);
	
	public static void main(String[] args) throws SAXException,
			ParserConfigurationException, IOException {
		/*
		 * read network file
		 */
		NetworkLayer network = new NetworkLayer();
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(network);
		reader.parse("/Users/fearonni/vsp-work/shared-svn/studies/schweiz-ivtch/baseCase/network/ivtch-changed-with-GTF.xml");
		/*
		 * load swiss boundary
		 */
		logger.info("Loading system boundaries...");
		ZoneLayerLegacy boundary = ZoneLayerLegacy.createFromShapeFile("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/gemeindegrenzen2008.zip Folder/g1g08_shp_080606.zip Folder/G1L08.shp");
		/*
		 * create adjacency matrix
		 */
		logger.info("Creating adjacency matrix...");
		AdjacencyMatrix y = new AdjacencyMatrix();
		TIntObjectHashMap<Node> idx2Node = new TIntObjectHashMap<Node>();
		TObjectIntHashMap<Node> node2Idx = new TObjectIntHashMap<Node>();

		for (Node node : network.getNodes().values()) {
			if (boundary.getZone(node.getCoord()) != null) {
				int idx = y.addVertex();
				idx2Node.put(idx, node);
				node2Idx.put(node, idx);
			}
		}
		/*
		 * create free travel time matrix
		 */
		logger.info("Caching travel times...");
		int n = idx2Node.size();
		final SparseMatrix linkTTs = calcRealTravelTimes(network, node2Idx, y);
		
		Graph g = y.getGraph(new SparseGraphBuilder());
		logger.info("Disconnected components: " + Partitions.disconnectedComponents(g).size());
		/*
		 * calculate paths
		 */
		logger.info("Calculating travel times...");
		EdgeWeight weights = new EdgeWeight() {
			public double getEdgeWeight(int i, int j) {
				return linkTTs.get(i, j);
			}
		};

		int[][] ttmatrix = new int[n][n];
		WeightedDijkstra dijkstra = new WeightedDijkstra(y);

		int count = 0;
		for (int i = 0; i < n; i++) {
			dijkstra.run(i, -1, weights);
			for (int j = (i + 1); j < n; j++) {
				TIntArrayList path = dijkstra.getPath(i, j);
				if (path != null) {
					path.insert(0, i);
					int sum = 0;
					int current = path.get(0);
					for (int k = 1; k < path.size(); k++) {
						int next = path.get(k);
						sum += linkTTs.get(current, next);
						current = next;
					}
					ttmatrix[i][j] = sum;
					ttmatrix[j][i] = sum;
					
					if(sum == 0) {
						
						Node n_i = idx2Node.get(i);
						Node n_j = idx2Node.get(j);
						Link alink = null;
						for(Link link : n_i.getOutLinks().values()) {
							if(link.getToNode().equals(n_j)) {
								alink = link;
								break;
							}
						}
						if(alink == null) {
							logger.warn(String.format("Travel time is zero! Path length = %1$s,", path.size()));
						} else {
							logger.warn(String.format("Travel time is zero! Link length = %1$s", alink.getLength()));
						}
					}
				} else {
					ttmatrix[i][j] = Integer.MAX_VALUE;
					ttmatrix[j][i] = Integer.MAX_VALUE;
				}
			}
			count++;
			if(count % 100 == 0)
				logger.info(String.format("Processed %1$s of %2$s nodes. (%3$s)", count, n, count/(float)n));
		}
		/*
		 * dump
		 */
		logger.info("Writing file...");
		BufferedWriter writer = new BufferedWriter(new FileWriter("/Users/fearonni/vsp-work/work/socialnets/data/schweiz/complete/ttmatrix.real3.txt"));
		for (int i = 0; i < n; i++) {
			writer.write("\t");
			writer.write(idx2Node.get(i).getId().toString());
		}
		writer.newLine();
		for (int i = 0; i < n; i++) {
			writer.write(idx2Node.get(i).getId().toString());
			for (int j = 0; j < n; j++) {
				writer.write("\t");
				writer.write(String.valueOf(ttmatrix[i][j]));
			}
			writer.newLine();
		}
		writer.close();
		logger.info("Done.");
	}

	private static SparseMatrix calcFreeTravelTimes(NetworkLayer network, TObjectIntHashMap<Node> node2Idx, AdjacencyMatrix y) {
		int n = node2Idx.size();
		final SparseMatrix linkTTs = new SparseMatrix(n, n);
		for (Link link : network.getLinks().values()) {
			Node n_i = link.getFromNode();
			Node n_j = link.getToNode();

			int i = -1;
			if (node2Idx.containsKey(n_i))
				i = node2Idx.get(n_i);

			int j = -1;
			if (node2Idx.containsKey(n_j))
				j = node2Idx.get(n_j);

			if (i > -1 && j > -1) {
				if (!y.getEdge(i, j)) {
					y.addEdge(i, j);
				}
				linkTTs.set(i, j, (int) (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME)));
				
			}
		}
		
		return linkTTs;
	}
	
	private static SparseMatrix calcRealTravelTimes(NetworkLayer network, TObjectIntHashMap<Node> node2Idx, AdjacencyMatrix y) {
		logger.info("Loading events...");
		final TravelTimeCalculator ttCalculator = new TravelTimeCalculator(network, 900, 86400, new TravelTimeCalculatorConfigGroup());
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(ttCalculator);
		EventsReaderTXTv1 eReader = new EventsReaderTXTv1(events);
		eReader.readFile("/Users/fearonni/vsp-work/runs-svn/run669/it.1000/1000.events.txt.gz");
		
		int n = node2Idx.size();
		final SparseMatrix linkTTs = new SparseMatrix(n, n);
		for (Link link : network.getLinks().values()) {
			Node n_i = link.getFromNode();
			Node n_j = link.getToNode();

			int i = -1;
			if (node2Idx.containsKey(n_i))
				i = node2Idx.get(n_i);

			int j = -1;
			if (node2Idx.containsKey(n_j))
				j = node2Idx.get(n_j);

			if (i > -1 && j > -1) {
				if (!y.getEdge(i, j)) {
					y.addEdge(i, j);
				}
				double tt = linkTTs.get(j, i);
				if(tt > 0) {
					tt += (int)ttCalculator.getLinkTravelTime(link, 28800);
					tt = tt/2.0;
					linkTTs.set(i, j, (int)tt);
					linkTTs.set(j, i, (int)tt);
				} else {
					linkTTs.set(i, j, (int)ttCalculator.getLinkTravelTime(link, 28800));
					linkTTs.set(j, i, (int)ttCalculator.getLinkTravelTime(link, 28800));
				}
				
			}
		}
		
		return linkTTs;
	}

}
