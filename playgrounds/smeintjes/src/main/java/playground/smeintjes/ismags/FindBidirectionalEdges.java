package playground.smeintjes.ismags;

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
 * This class takes as input an edge list consisting of both one-directional and 
 * bi-directional edges, removes the bi-directional edges and writes it to a 
 * separate bi-directional edge list. The edge lists have with one edge per line, 
 * and each source and destination node is separated by a space or tab. This
 * class assumes that the input edges are separated by a space, and the output
 * edges will be separated by a tab.
 * 
 * This is done to create two edge lists for ISMAGS: one containing all one-
 * directional edges, and one containing all bi-directional edges.
 * 
 * Use this class if you only need to perform this activity for one network. If you
 * want to perform it on multiple networks, use FindBidirectionalEdgesMultipleNetworks.
 * 
 * The network should be located in a subfolder of the same name as the network file.
 * 
 * @param inputPath path to the folder containing the input file, including trailing '/'
 * @param fileName the name of the network file (should be same as its parent folder)
 * @param extension file extension
 * @param outputPath the path to the folder where output files will be written, including trailing "/"
 * (can be the same as the inputPath)
 * 
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
		String inputEdgePath = args[0];
		String fileName = args[1];
		String extension = args[2];
		String outputPath = args[3];
//		String inputSeparator = args[4];
//		String outputSeparator = args[5];
//		
		List<Tuple<Integer, Integer>> edgeList = readInputList(inputEdgePath, fileName, extension);
		findBidirectionalEdges(edgeList, outputPath, fileName);
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
			List<Tuple<Integer, Integer>> edgeList, String path, String fileName) {
		
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
//					log.info("Edge " + reverseSource + ", " + reverseDestination + " has already been identified as bidirectional.");	
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
		String bidirectionalFileName = fileName + "_bidirectional.txt";
		String onedirectionalFileName = fileName + "_onedirectional.txt";
		writeOutput(bidirectionalList, path, fileName, bidirectionalFileName);
		writeOutput(edgeList, path, fileName, onedirectionalFileName);
		
		
	}

	private static void writeOutput(List<Tuple<Integer, Integer>> edgeList,
			String outputPath, String folderName, String fileName) {
		
		log.info("Writing " + fileName + " to file.");
		try {
			BufferedWriter output = new BufferedWriter(
					new FileWriter(new File(outputPath + folderName + "/" + fileName)));
			try {
					for (Tuple<Integer, Integer> edge : edgeList) {
						String source = Integer.toString(edge.getFirst());
						String destination = Integer.toString(edge.getSecond());
						output.write(source);
						output.write("	");
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
	 * This method reads in the edge list containing both one-directional and
	 * bi-directional edges. 
	 * 
	 * NOTE: The input list should not contain a header.
	 * @param the path to the input edge list
	 * @param the filename
	 * @param the separator used to separate the source and destination nodes
	 */
	public static List<Tuple<Integer, Integer>> readInputList(String inputPath,
			String fileName, String extension) {
		
		String filePathAndName = inputPath + fileName + "/" + fileName + extension;
		log.info("Reading edge list list from " + filePathAndName);
		List<Tuple<Integer, Integer>> edgeList = new ArrayList<Tuple<Integer, Integer>>();
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(filePathAndName);
			String lines;
			while ((lines = br.readLine()) != null) {
				String[] inputString = lines.split(" ");
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
