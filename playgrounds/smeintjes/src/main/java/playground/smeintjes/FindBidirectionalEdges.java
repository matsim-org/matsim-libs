package playground.smeintjes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;
import org.apache.log4j.Logger;

import playground.southafrica.utilities.Header;


/**
 * This class takes as input an edge list consisting of unidirectional and 
 * bidirectional edges, removes the bidirectional edges and writes it to a 
 * separate bidirectional edge list. The edge lists have with one edge per line, 
 * and each source and destination node is separated by a space or tab (specified 
 * by user).
 * 
 * This is done to create two edge lists for ISMAGS: one containing all uni-
 * directional edges, and one containing all bidirectional edges.
 * 
 * @param String the input file name including extension (.txt)
 * @param String the path to the input and output files, including trailing "/"
 * @param String the separator separating the source and destination nodes in the
 * INPUT edge list
 * @param String the separator that should be used in the OUTPUT edge lists.
 * @author sumarie
 *
 */
public class FindBidirectionalEdges {

	private static Logger log = Logger.getLogger(FindBidirectionalEdges.class.toString());
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(FindBidirectionalEdges.class.toString(), args);
		String inputEdgeFile= args[0];
		String path = args[1];
		String inputSeparator = args[2];
		String outputSeparator = args[3];
		
		List<Tuple<Integer, Integer>> edgeList = readInputList(inputEdgeFile, path, inputSeparator);
		findBidirectionalEdges(edgeList, path, outputSeparator);
		Header.printFooter();
	}
	
	/**
	 * This method iterates through the edge list looking for bidirectional edges.
	 * For each edge, it determines its reverse edge (swaps source and destination
	 * nodes). It then iterates through the rest of the edge list to see if any
	 * of the edges matches the reverse edge. If it does, it means that this edge
	 * is bidirectional. It removes both the current edge and the identified
	 * reverse edge and places the edge (only one copy) in a separate list, the 
	 * bidirectional edge list. 
	 * @param edgeList
	 */
	private static void findBidirectionalEdges(
			List<Tuple<Integer, Integer>> edgeList, String path,
			String outputSeparator) {
		
		log.info("Looking for bidirectional edges.");
		List<Tuple<Integer, Integer>> bidirectionalList = new ArrayList<Tuple<Integer, Integer>>();
		List<Tuple<Integer, Integer>> toRemoveList = new ArrayList<Tuple<Integer, Integer>>();
		for(int i = 0; i < edgeList.size(); i++){
			Tuple<Integer, Integer> thisEdge = edgeList.get(i);
			int source = thisEdge.getFirst();
			int destination = thisEdge.getSecond();
			Tuple<Integer, Integer> reverseEdge = new Tuple<Integer, Integer>(destination, source);
			int reverseSource = reverseEdge.getFirst();
			int reverseDestination = reverseEdge.getSecond();
				if(toRemoveList.contains(reverseEdge)){
					log.info("Edge " + reverseSource + ", " + reverseDestination + " has already been identified as bidirectional.");	
				} else{	
					for(int j = 0; j < edgeList.size(); j++){
					Tuple<Integer, Integer> nextEdge = edgeList.get(j);
					int nextSource = nextEdge.getFirst();
					int nextDestination = nextEdge.getSecond();
						if((reverseSource == nextSource) && (reverseDestination == nextDestination)){
							bidirectionalList.add(thisEdge);
							toRemoveList.add(thisEdge);
							toRemoveList.add(nextEdge);
						}
				}
			}
		}
		edgeList.removeAll(toRemoveList);
		writeOutput(bidirectionalList, path, "bidirectional.txt", outputSeparator);
		writeOutput(edgeList, path, "unidirectional.txt", outputSeparator);
		
		
	}

	private static void writeOutput(List<Tuple<Integer, Integer>> edgeList,
			String inputPath, String fileName, String separator) {
		
		log.info("Writing " + fileName + " to file.");
		try {
			BufferedWriter output = new BufferedWriter(
					new FileWriter(new File(inputPath + "/" + fileName)));
			try {
					for (Tuple<Integer, Integer> edge : edgeList) {
						String source = Integer.toString(edge.getFirst());
						String destination = Integer.toString(edge.getSecond());
						output.write(source);
						output.write("\t");
						output.write(destination);
						output.newLine();
					}
			} finally {
				output.close();
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * This method reads in the edge list containing both unidirectional and
	 * bidirectional edges. 
	 * 
	 * NOTE: The input list should not contain a header.
	 * 
	 * @param the path to the input edge list
	 * @param the separator used to separate the source and destination nodes
	 */
	public static List<Tuple<Integer, Integer>> readInputList(String inputEdgeList,
			String path, String separator) {
		
		log.info("Reading edge list list from " + path + inputEdgeList);
		List<Tuple<Integer, Integer>> edgeList = new ArrayList<Tuple<Integer, Integer>>();
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(path + "/" + inputEdgeList);
			String lines;
			while ((lines = br.readLine()) != null) {
				String[] inputString = lines.split(separator);
				int source = Integer.parseInt(inputString[0]);
				int destination = Integer.parseInt(inputString[1]);
				Tuple<Integer, Integer> thisEdgeTuple = 
						new Tuple<Integer, Integer>(source, destination);
				edgeList.add(thisEdgeTuple);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return edgeList;
	}

}
