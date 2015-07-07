package playground.smeintjes.ismags;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;



/**
 * This class takes adjacency matrices as input and converts it into the 
 * equivalent motif specifications needed by ISMAGS. The motif specifications 
 * are described in detail in Demeyer, S. et al. (2013). The index-based subgraph 
 * matching algorithm (ISMA): Fast subgraph enumeration in large networks using 
 * optimized search trees. PLOS ONE. Vol. 8, Issue 4.
 * 
 * Currently this class is set up to convert 5-node motifs. If a different motif size
 * is required, change the createMatrices method accordingly.
 * 
 * @author sumarie
 *
 */
public class ConvertMotifMatrixToSpecification {

	private static Logger log = Logger.getLogger(ConvertMotifMatrixToSpecification.class);
	
	
	/**
	 * @param adjacencyInputFile file containing the adjacency matrices to be converted
	 * @param specificationOutput file containing the output motif specifications
	 */
	public static void main(String[] args) {
		Header.printHeader(ConvertMotifMatrixToSpecification.class.toString(), args);
		String adjacencyInputFile = args[0];
		String specificationOutput = args[1];

		ArrayList<char[]> adjacencyList = readInput(adjacencyInputFile);
		ArrayList<char[][]> listOfMatrices = createMatrices(adjacencyList);
		ArrayList<ArrayList<String>> motifSpecificationList = getMotifSpecifications(listOfMatrices);
		writeOutput(motifSpecificationList, specificationOutput);
		Header.printFooter();
	}
	
	private static void writeOutput(ArrayList<ArrayList<String>> motifSpecificationList,
			String specificationOutput) {
		
		try{
			BufferedWriter bw = IOUtils.getBufferedWriter(specificationOutput);
			try{
				for (ArrayList<String> motifSpecification : motifSpecificationList) {
					for (String string : motifSpecification) {
						bw.write(string);
					}
					bw.newLine();
				}
				
			}  finally{
					bw.close();
			}
		}
		 catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot close " + specificationOutput);
		}
	}

	private static ArrayList<char[][]> createMatrices(ArrayList<char[]> adjacencyList){
		
		ArrayList<char[][]> listOfMatrices = new ArrayList<char[][]>();
		
		for (char[] adjacencyArray : adjacencyList) {
			char[][] adjacencyMatrix = new char[5][5];
			
			adjacencyMatrix[0][0] = adjacencyArray[0];
			adjacencyMatrix[0][1] = adjacencyArray[1];
			adjacencyMatrix[0][2] = adjacencyArray[2];
			adjacencyMatrix[0][3] = adjacencyArray[3];
			adjacencyMatrix[0][4] = adjacencyArray[4];
			
			adjacencyMatrix[1][0] = adjacencyArray[5];
			adjacencyMatrix[1][1] = adjacencyArray[6];
			adjacencyMatrix[1][2] = adjacencyArray[7];
			adjacencyMatrix[1][3] = adjacencyArray[8];
			adjacencyMatrix[1][4] = adjacencyArray[9];
			
			adjacencyMatrix[2][0] = adjacencyArray[10];
			adjacencyMatrix[2][1] = adjacencyArray[11];
			adjacencyMatrix[2][2] = adjacencyArray[12];
			adjacencyMatrix[2][3] = adjacencyArray[13];
			adjacencyMatrix[2][4] = adjacencyArray[14];
			
			adjacencyMatrix[3][0] = adjacencyArray[15];
			adjacencyMatrix[3][1] = adjacencyArray[16];
			adjacencyMatrix[3][2] = adjacencyArray[17];
			adjacencyMatrix[3][3] = adjacencyArray[18];
			adjacencyMatrix[3][4] = adjacencyArray[19];
			
			adjacencyMatrix[4][0] = adjacencyArray[20];
			adjacencyMatrix[4][1] = adjacencyArray[21];
			adjacencyMatrix[4][2] = adjacencyArray[22];
			adjacencyMatrix[4][3] = adjacencyArray[23];
			adjacencyMatrix[4][4] = adjacencyArray[24];
			
			listOfMatrices.add(adjacencyMatrix);			
		}
		
		return listOfMatrices;
		
	}

	private static ArrayList<ArrayList<String>> getMotifSpecifications(
			ArrayList<char[][]> listOfMatrices) {
		
		ArrayList<ArrayList<String>> motifSpecificationList = new ArrayList<ArrayList<String>>();
		
		for (char[][] adjacencyMatrix : listOfMatrices) {
			ArrayList<String> motifSpecification = new ArrayList<String>();
			for(int i = 1; i < adjacencyMatrix.length; i++){
				for(int j = 0; j < i; j++){
					
					char thisValue = adjacencyMatrix[j][i];
					char oppositeValue = adjacencyMatrix[i][j];
					if ((thisValue == '1') && (oppositeValue == '1')) {
						motifSpecification.add("X");
					} else if((thisValue == '1') && (oppositeValue == '0')){
						motifSpecification.add("A");
					} else if((thisValue == '0') && (oppositeValue == '1')){
						motifSpecification.add("a");
					} else if((thisValue == '0') && (oppositeValue == '0')){
						motifSpecification.add("0");
					} else
						log.info("None of the if statements were satisfied. [i][j] = " + thisValue + ", [j][i] = " + oppositeValue);
					
				}
			}
			
			motifSpecificationList.add(motifSpecification);
		}
		
		return motifSpecificationList;
	}

	private static ArrayList<char[]> readInput(String adjacencyInputFile) {
		
		ArrayList<char[]> adjacencyList = new ArrayList<char[]>();
		int i = 0;
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(adjacencyInputFile);
			String lines;
			while ((lines = br.readLine()) != null) {
			
				char[] stringArray = lines.toCharArray();
				
				adjacencyList.add(stringArray);
				i++;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("There were " + i + " adjacency matrices.");
		
		return adjacencyList;
	} 

}
