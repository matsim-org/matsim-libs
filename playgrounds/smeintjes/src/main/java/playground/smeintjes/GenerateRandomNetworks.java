package playground.smeintjes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;


/** 
 * This class is used to generate random networks, given an input network. 
 * It reads in the edge list of a network (each line containing a "from_node" 
 * and a "to_node". It then randomly shuffles all "to_node"s so that a random 
 * network with the same degree distribution can be generated. The random 
 * network(s) are then written to file.
 * @author sumarie
 *
 */
public class GenerateRandomNetworks {

	private static Logger log = Logger.getLogger(GenerateRandomNetworks.class.toString());
	/**
	 * @param String input edge list
	 * @param String path to folder where output networks should be written (empty string on Hobbes)
	 * @param int number of random networks to generate
	 */
	public static void main(String[] args) {

		Header.printHeader(GenerateRandomNetworks.class.toString(), args);
		String inputFile = args[0];
		String outputFolder = args[1];
		Integer numberRandomNetworks = Integer.parseInt(args[2]);
		
		ArrayList<Tuple<Integer, Integer>> arrayEdgeList = readInputNetwork(inputFile);
		
		Tuple<ArrayList<Integer>, ArrayList<Integer>> tupleEdgeList = arrayListToTuple(arrayEdgeList);
		ArrayList<Integer> fromNodes = tupleEdgeList.getFirst();
		ArrayList<Integer> toNodes = tupleEdgeList.getSecond();
		
		for(int i = 0; i<numberRandomNetworks; i++){
//			log.info("========================================");
			log.info("Generating random network " + i + ".");
//			log.info("========================================");
			ArrayList<Integer> randomToNodes = generateRandomNetworks(toNodes, fromNodes);
			log.info("Checking for self loops.");
			ArrayList<Tuple<Integer, Integer>> randomNetworkNoSelfEdges = checkNoSelfEdges(randomToNodes, fromNodes);
			log.info("Checking for duplicate edges.");
			List<Tuple<Integer, Integer>> randomNoDuplicates = checkNoDuplicates(randomNetworkNoSelfEdges);
			log.info("Sorting network.");
//			List<Tuple<Integer, Integer>> randomSorted = sortNetwork(randomNoDuplicates);
			String networkName = "random" + i;
			writeRandomNetwork(outputFolder, networkName, randomNoDuplicates, i);
		}
		
		Header.printFooter();
	}

	private static List<Tuple<Integer, Integer>> sortNetwork(
			List<Tuple<Integer, Integer>> randomNoDuplicates) {
		
		IntegerTupleComparator itc = new IntegerTupleComparator();
		Collections.sort(randomNoDuplicates, itc);
		
		return randomNoDuplicates;
	}

	private static Tuple<ArrayList<Integer>, ArrayList<Integer>> arrayListToTuple (
			ArrayList<Tuple<Integer, Integer>> inputEdgeList) {
		
		ArrayList<Integer> fromNodes = new ArrayList<Integer>();
		ArrayList<Integer> toNodes = new ArrayList<Integer>();
		
		for(int i = 0; i < inputEdgeList.size(); i++){
			Integer thisSource = inputEdgeList.get(i).getFirst();
			Integer thisDestination = inputEdgeList.get(i).getSecond();
			fromNodes.add(thisSource);
			toNodes.add(thisDestination);
		}
		
 		Tuple<ArrayList<Integer>, ArrayList<Integer>> fromToTuple = 
				new Tuple<ArrayList<Integer>, ArrayList<Integer>>(fromNodes, toNodes);
		
		return fromToTuple;
	}
	
	//TODO implement somewhere
	private static ArrayList<Tuple<Integer, Integer>> tupleToArrayList (
			Tuple<ArrayList<Integer>, ArrayList<Integer>> fromToArrayList){
		
		ArrayList<Integer> fromNodes = fromToArrayList.getFirst();
		ArrayList<Integer> toNodes = fromToArrayList.getSecond();
		
		ArrayList<Tuple<Integer, Integer>> edgeArrayList = 
				new ArrayList<Tuple<Integer, Integer>>();

		for(int i = 0; i < fromNodes.size(); i++){
			Integer thisSource = fromNodes.get(i);
			Integer thisDestination = toNodes.get(i);
			Tuple<Integer, Integer> thisEdgeTuple = new Tuple<Integer, Integer>(thisSource, thisDestination);
			edgeArrayList.add(thisEdgeTuple);
		}
		
		return edgeArrayList;
		
	}


	private static ArrayList<Tuple<Integer, Integer>> checkNoDuplicates(
			ArrayList<Tuple<Integer, Integer>> newRandomNetwork) {

		for (int i = 0; i < newRandomNetwork.size(); i++) {
			Tuple<Integer, Integer> thisEdge = newRandomNetwork.get(i);
			Integer thisSource = thisEdge.getFirst();
			Integer thisDestination = thisEdge.getSecond();
			for(int j = 0; j < newRandomNetwork.size(); j++){
				if(i != j){
					Tuple<Integer, Integer> nextEdge = newRandomNetwork.get(j);
					if(nextEdge.equals(thisEdge)){
//						log.info("Duplicate edge found at index " + j + ": " + thisEdge.getFirst() + ", " + thisEdge.getSecond());
						Random random = new Random();
						int min = 0;
						int max = newRandomNetwork.size()-1;
						int randomIndex = random.nextInt(max - min + 1) + min;
						
						Tuple<Integer, Integer> edgeAtRandomIndex = newRandomNetwork.get(randomIndex);
						Integer randomSource = edgeAtRandomIndex.getFirst();
						Integer randomDestination = edgeAtRandomIndex.getSecond();
						if(thisEdge.equals(edgeAtRandomIndex)){
							break;
						} else{
							if(!thisSource.equals(randomDestination) && !thisDestination.equals(randomSource)){
							Tuple<Integer, Integer> newEdgeHere = new Tuple<Integer, Integer>(thisSource, randomDestination);
							Tuple<Integer, Integer> newRandomEdge = new Tuple<Integer, Integer>(randomSource, thisDestination);
							newRandomNetwork.set(i, newEdgeHere);
							newRandomNetwork.set(j, newRandomEdge);
							} else{
								break;
							}
						}
					} else{
						continue;
					}
				} else{
					continue;
				}
			}
		}
		
		return newRandomNetwork;
	}

	private static void writeRandomNetwork(String outputPath, String networkName,
			List<Tuple<Integer, Integer>> randomSorted, int i) {
		
		log.info("Writing " + outputPath + networkName + "/" + networkName + " to file.");
		try {
			BufferedWriter output = new BufferedWriter(
					new FileWriter(new File(outputPath + networkName + "/" + networkName + ".txt")));
			try {
				
					for (int j = 0; j < randomSorted.size(); j++) {
						Integer source = randomSorted.get(j).getFirst();
						Integer destination = randomSorted.get(j).getSecond();
						output.write(String.valueOf(source));
						output.write("	");
						output.write(String.valueOf(destination));
						output.newLine();
					}
			} finally {
				output.close();
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private static ArrayList<Integer> generateRandomNetworks(ArrayList<Integer> originalToNodes, ArrayList<Integer> fromNodes) {
		
		Collections.shuffle(originalToNodes);
		ArrayList<Integer> randomToNodes = new ArrayList<Integer>();
		randomToNodes.addAll(originalToNodes);
		
		return randomToNodes;
		
	}
	
	private static ArrayList<Tuple<Integer, Integer>> twoArrayListsToOneArrayList(
			ArrayList<Integer> fromNodeArrayList, ArrayList<Integer> toNodeArrayList){
		
		ArrayList<Tuple<Integer, Integer>> edgeArrayList = new ArrayList<Tuple<Integer, Integer>>();
		
		for(int i = 0; i < fromNodeArrayList.size(); i++){
			Integer thisSource = fromNodeArrayList.get(i);
			Integer thisDestination = toNodeArrayList.get(i);
			Tuple<Integer, Integer> thisEdgeTuple = new Tuple<Integer, Integer>(thisSource, thisDestination);
			edgeArrayList.add(thisEdgeTuple);
		}
		
		return edgeArrayList;
		
	}

	private static ArrayList<Tuple<Integer, Integer>> checkNoSelfEdges(
			ArrayList<Integer> randomToNodes, ArrayList<Integer> fromNodes) {
		
		ArrayList<Tuple<Integer, Integer>> edgeArrayList = 
				twoArrayListsToOneArrayList(fromNodes, randomToNodes);
		
		for(int i = 0; i < edgeArrayList.size(); i++){
			Tuple<Integer, Integer> thisEdge = edgeArrayList.get(i);
			Integer thisSource = thisEdge.getFirst();
			Integer thisDestination = thisEdge.getSecond();
			
			if(thisSource.equals(thisDestination)){
//				log.info("Self loop at index " + i + ": " + thisSource + " = " + thisDestination);
				Random random = new Random();
				int min = 0;
				int max = edgeArrayList.size()-1;
				int randomIndex = random.nextInt(max - min + 1) + min;
				
				Tuple<Integer, Integer> edgeAtRandomIndex = edgeArrayList.get(randomIndex);
				Integer randomSource = edgeAtRandomIndex.getFirst();
				Integer randomDestination = edgeAtRandomIndex.getSecond();
				if(!thisDestination.equals(randomDestination) && !thisDestination.equals(randomSource)){
					Tuple<Integer, Integer> newEdgeHere = new Tuple<Integer, Integer>(thisSource, randomDestination);
					Tuple<Integer, Integer> newRandomEdge = new Tuple<Integer, Integer>(randomSource, thisDestination);
					edgeArrayList.set(randomIndex, newRandomEdge);
					edgeArrayList.set(i, newEdgeHere);
				} else{
					break;
				}
				
			}
		}
		
		return edgeArrayList;
	}



	private static ArrayList<Tuple<Integer, Integer>>
						readInputNetwork(String inputFile) {
		
		log.info("Reading edge list from " + inputFile);
		
		ArrayList<Tuple<Integer, Integer>> edgeList = new ArrayList<Tuple<Integer, Integer>>();
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(inputFile);
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
