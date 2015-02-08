package playground.nmviljoen.network.salience;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatsimMatricesReader;

import playground.nmviljoen.network.NmvLink;
import playground.nmviljoen.network.NmvNode;
import playground.southafrica.utilities.FileUtils;
import playground.southafrica.utilities.Header;
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
		String salienceFilename = args[1];
		String degreeFilename = args[2];
		int numberOfThreads = Integer.parseInt(args[3]);
		
		Graph<NmvNode, NmvLink> graph = SampleNetworkBuilder.readGraphML(graphFilename);
		
		SalienceCalculator sc = new SalienceCalculator(graph);
		sc.writeDegreeDistributionToFile(degreeFilename, graph);
		Map<NmvLink, Double> salienceMap = sc.calculateSalience(numberOfThreads);
		sc.writeSalienceToFile(salienceFilename, salienceMap);

		Header.printFooter();
	}
	
	
	public SalienceCalculator(Graph<NmvNode, NmvLink> graph) {
		this.graph = graph;
	}
	
	private Map<NmvLink, Double> calculateSalience(int numberOfThreads){
		Transformer<NmvLink, Double> weightTransformer = new Transformer<NmvLink, Double>() {
			@Override
			public Double transform(NmvLink arg0) {
				return 1.0/arg0.getWeight();
			}
		};
		
		/* Initialise the spt matrix container. */
		LOG.info("Calculating the shortest path trees for all root nodes.");
		Matrices matrices = new Matrices();
		
		/* Create a temporary folder for the matrices. See if this actually 
		 * reduced the enormous memory burden. */
		File tmpFolder = new File("./tmp/");
		FileUtils.delete(tmpFolder);
		boolean tmpFolderCreated = tmpFolder.mkdir();
		if(!tmpFolderCreated){
			throw new RuntimeException("Cannot create a temporary folder './tmp/' ");
		}
		
		/* Set up the multi-threaded infrastructure. */
		ExecutorService threadExecutorSpt = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<Boolean>> listOfJobsSpt = new ArrayList<Future<Boolean>>();
		
		DijkstraShortestPath<NmvNode, NmvLink> dsp = new DijkstraShortestPath<NmvNode, NmvLink>(this.graph, weightTransformer);
		Iterator<NmvNode> iteratorO = graph.getVertices().iterator();
		
		Counter sptCounter = new Counter("   root node # ");
		while(iteratorO.hasNext()){
			NmvNode node = iteratorO.next();
			Callable<Boolean> job = new sptCallable(this.graph, node, dsp, sptCounter);
			Future<Boolean> result = threadExecutorSpt.submit(job);
			listOfJobsSpt.add(result);
		}

		threadExecutorSpt.shutdown();
		while(!threadExecutorSpt.isTerminated()){
		}
		sptCounter.printCounter();
		
		/* Aggregate the output. */
//		for(Future<Boolean> job : listOfJobsSpt){
//			Matrix matrix = null;
//			try {
//				matrix = job.get();
//			} catch (InterruptedException | ExecutionException e) {
//				e.printStackTrace();
//				throw new RuntimeException("Cannot get multithreaded shortest path tree for root node.");
//			}
//			matrices.getMatrices().put(matrix.getId(), matrix);
//		}
//		LOG.info("Done calculating the shortest path trees.");
//		LOG.info("  |_ Number of matrices: " + matrices.getMatrices().size());
		
		
		/* Calculate the salience for each link. First set up the multi-threaded 
		 * infrastructure*/
		LOG.info("Calculating link salience for each link...");
		ExecutorService threadExecutorSalience = Executors.newFixedThreadPool(numberOfThreads);
		List<Future<Map<NmvLink, Double>>> listOfJobsSalience = new ArrayList<Future<Map<NmvLink, Double>>>();
		
		Counter salienceCounter = new Counter("   links # ");
		Iterator<NmvLink> linkIterator = this.graph.getEdges().iterator();
		while(linkIterator.hasNext()){
			NmvLink link = linkIterator.next();
			Callable<Map<NmvLink, Double>> job = new salienceCallable(this.graph, link, salienceCounter);
			Future<Map<NmvLink, Double>> result = threadExecutorSalience.submit(job);
			listOfJobsSalience.add(result);
		}
		
		threadExecutorSalience.shutdown();
		while(!threadExecutorSalience.isTerminated()){
		}
		salienceCounter.printCounter();
		
		/* Aggregate the output. */
		Map<NmvLink, Double> salienceMap = new TreeMap<NmvLink, Double>();
		for(Future<Map<NmvLink, Double>> job : listOfJobsSalience){
			Map<NmvLink, Double> result = null;
			try {
				result = job.get();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot get multithreaded salience results.");
			}
			for(NmvLink link : result.keySet()){
				salienceMap.put(link, result.get(link));
			}
		}
		
		LOG.info("Done calculating the salience.");
		
		LOG.info("Cleaning up the temporary folder...");
		FileUtils.delete(tmpFolder);
		return salienceMap;
	}
	
	private class salienceCallable implements Callable<Map<NmvLink, Double>>{
		private Graph<NmvNode, NmvLink> graph;
		private NmvLink link;
		private Counter counter;
		
		public salienceCallable(Graph<NmvNode, NmvLink> graph, NmvLink link, Counter counter) {
			this.graph = graph;
			this.link = link;
			this.counter = counter;
		}
		
		@Override
		public Map<NmvLink, Double> call() throws Exception {
			Map<NmvLink, Double> result = new TreeMap<NmvLink, Double>();
			
			/* Get the list of all temporary matrices. */
			List<File> matrixFiles = FileUtils.sampleFiles(new File("./tmp/"), Integer.MAX_VALUE, FileUtils.getFileFilter(".xml.gz"));
			Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			
			Pair<NmvNode> nodes = this.graph.getEndpoints(this.link);
			String o = nodes.getFirst().getId();
			String d = nodes.getSecond().getId();
			double total = 0.0;
			
			for(File f : matrixFiles){
				String matrixName = f.getAbsolutePath().substring(
						f.getAbsolutePath().lastIndexOf("/")+1, 
						f.getAbsolutePath().length()-7);
				Matrices m = new Matrices();
				new MatsimMatricesReader(m, sc).parse(f.getAbsolutePath());
				Matrix matrix = m.getMatrix(matrixName);
				Entry entry = matrix.getEntry(o, d);
				if(entry != null){
					total += entry.getValue();
				}
			}
			
			double salience = total / ((double) this.graph.getVertexCount());
			result.put(this.link, salience);
			
			this.counter.incCounter();
			return result;
		}
	}
	
	private class sptCallable implements Callable<Boolean>{
		private DijkstraShortestPath<NmvNode, NmvLink> dsp;
		private Graph<NmvNode, NmvLink> graph;
		private NmvNode node;
		private Counter counter;
		
		public sptCallable(Graph<NmvNode, NmvLink> graph, NmvNode node, DijkstraShortestPath<NmvNode, NmvLink> dijkstra, Counter counter) {
			this.graph = graph;
			this.node = node;
			this.dsp = dijkstra;
			this.counter = counter;
		}
		
		@Override
		public Boolean call() throws Exception {
			NmvNode source = this.node;
			Matrix matrix = new Matrix(this.node.getId(), "");
			
			Iterator<NmvNode> iteratorD = this.graph.getVertices().iterator();
			while(iteratorD.hasNext()){
				NmvNode target = iteratorD.next();
				List<NmvLink> path = this.dsp.getPath(source, target);
				for(NmvLink link : path){
					Pair<NmvNode> nodes = this.graph.getEndpoints(link);
					
					/* Check if the entry already exists. */
					String o = nodes.getFirst().getId();
					String d = nodes.getSecond().getId();
					if(matrix.getEntry(o, d) == null){
						matrix.createEntry(o, d, 1.0);
					} 
				}
			}
			
			/* Write the matrix to file. */
			Matrices m = new Matrices();
			m.getMatrices().put(matrix.getId(), matrix);
			new MatricesWriter(m).write("./tmp/" + matrix.getId() + ".xml.gz");
			
			this.counter.incCounter();
			return true;
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
	
	public void writeDegreeDistributionToFile(String filename, Graph<NmvNode, NmvLink> graph){
		LOG.info("Writing degree data to " + filename);
		
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try{
			bw.write("Id,inDegree,outDegree,degree");
			bw.newLine();
			
			for(NmvNode node : graph.getVertices()){
				int in = graph.getPredecessorCount(node);
				int out = graph.getSuccessorCount(node);
				bw.write(String.format("%s,%d,%d,%d\n", node.getId(), in, out, in+out));
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
		
		
	}
	
	
}
