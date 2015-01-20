package playground.nmviljoen.network.salience;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;

import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;
import playground.southafrica.utilities.Header;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraDistance;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Pair;

/**
 * Class to calculate the salient links in a weighted and directed complex
 * network using a {@link DirectedSparseMultigraph}.
 *
 * @author jwjoubert
 */
public class SalienceCalculator {
	final private static Logger LOG = Logger.getLogger(SalienceCalculator.class);
	private Graph<NmvNode, NmvLink> graph;

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(SalienceCalculator.class.toString(), args);
		
		String graphFilename = args[0];
		String outputFilename = args[1];
		
		Graph<NmvNode, NmvLink> graph = SampleNetworkBuilder.readGraphML(graphFilename);
		
		SalienceCalculator sc = new SalienceCalculator(graph);
		Map<NmvLink, Double> salienceMap = sc.calculateShortestPathTrees();
		sc.writeSalienceToFile(outputFilename, salienceMap);

		Header.printFooter();
	}
	
	
	public SalienceCalculator(Graph<NmvNode, NmvLink> graph) {
		this.graph = graph;
	}
	
	private Map<NmvLink, Double> calculateShortestPathTrees(){
		Transformer<NmvLink, Double> weightTransformer = new Transformer<NmvLink, Double>() {
			@Override
			public Double transform(NmvLink arg0) {
				return 1.0/arg0.getWeight();
			}
		};
		
		/* Initialise the spt matrix container. */
		LOG.info("Calculating the shortest path trees for all root nodes.");
		Matrices matrices = new Matrices();
		
		DijkstraShortestPath<NmvNode, NmvLink> dsp = new DijkstraShortestPath<NmvNode, NmvLink>(this.graph, weightTransformer);
		Iterator<NmvNode> iteratorO = graph.getVertices().iterator();
		while(iteratorO.hasNext()){
			NmvNode source = iteratorO.next();
//			LOG.info("Source node: " + source.getId());
			Matrix matrix = matrices.createMatrix(source.getId(), "");
			
			Iterator<NmvNode> iteratorD = graph.getVertices().iterator();
			while(iteratorD.hasNext()){
				NmvNode target = iteratorD.next();
//				LOG.info("Target node: " + target.getId());
				List<NmvLink> path = dsp.getPath(source, target);
				for(NmvLink link : path){
					Pair<NmvNode> nodes = graph.getEndpoints(link);
					
					/* Check if the entry already exists. */
					String o = nodes.getFirst().getId();
					String d = nodes.getSecond().getId();
					if(matrix.getEntry(o, d) == null){
						matrix.createEntry(o, d, 1.0);
					} 
				}
			}
		}
		LOG.info("Done calculating the shortest path trees.");
		
		/* Calculate the salience for each link. */
		LOG.info("Calculating link salience for each link...");
		Counter counter = new Counter("  links # ");
		Map<NmvLink, Double> salienceMap = new TreeMap<NmvLink, Double>();
		Iterator<NmvLink> linkIterator = graph.getEdges().iterator();
		while(linkIterator.hasNext()){
			NmvLink link = linkIterator.next();
			Pair<NmvNode> nodes = graph.getEndpoints(link);
			String o = nodes.getFirst().getId();
			String d = nodes.getSecond().getId();
			double total = 0.0;
			for(Matrix m : matrices.getMatrices().values()){
				Entry entry = m.getEntry(o, d);
				if(entry != null){
					total += entry.getValue();
				}
			}
			
			double salience = total / ((double) graph.getVertexCount());
			salienceMap.put(link, salience);
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("Done calculating the salience.");
		return salienceMap;
	}
	
	private class sptCallable implements Callable<Matrix>{
		private DijkstraShortestPath<NmvNode, NmvLink> dsp;
		
		public sptCallable(DijkstraShortestPath<NmvNode, NmvLink> dijkstra) {
			this.dsp = dijkstra;
		}
		
		@Override
		public Matrix call() throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	public void writeSalienceToFile(String filename, Map<NmvLink, Double> salience){
		LOG.info("Writing salience results to " + filename);
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("Id,from,to,weight,salience");
			bw.newLine();
			
			for(NmvLink link : this.graph.getEdges()){
				Pair<NmvNode> nodes = this.graph.getEndpoints(link);
				bw.write(String.format("%s,%s,%s,%.2f,%.4f\n", link.getId(), nodes.getFirst().getId(), nodes.getSecond().getId(), link.getWeight(), salience.get(link)));
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
		
		LOG.info("Done writing salience results to file.");
	}
	
	
}
