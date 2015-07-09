package playground.smeintjes.ismags;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

/**
 * This class calculates, for all random networks, the number of each type of
 * motif and writes these frequencies to file.
 * 
 * @param String input motif specification file (a list containing all possible 
 * motif specifications for a motif of certain size) (example "motifSpecifications/3-node-motifSpecifications.txt")
 * @param String path to folder containing random networks ("randomNetworks/")
 * @param String path to output file (example "random-3-node-frequencies.txt")
 * @param Integer number of random networks ("100")
 * @param String the name of the subfolder containing the motif results (example "3-node-motifs/" when looking at 3-node motifs)
 * 
 * @author sumarie
 *
 */
public class CountMotifs {

	private static Logger log = Logger.getLogger(CountMotifs.class);
	/** 
	 * @param args
	 */
	public static void main(String[] args) {

		Header.printHeader(CountMotifs.class.toString(), args);
		
		String motifSpecificationFile = args[0];
		String randomNetworksFolder = args[1];
		String outputFile = args[2];
		Integer numberNetworks = Integer.parseInt(args[3]);
		String inputSubFolder = args[4];
		
		
		ArrayList<String> motifSpecifications = readMotifSpecifications(motifSpecificationFile);
		ArrayList<Integer> thisMotifFrequencies = new ArrayList<Integer>();
		TreeMap<String, ArrayList<Integer>> motifFrequencyMap = new TreeMap<String, ArrayList<Integer>>();
		int[][] motifFrequencyBlock = new int[motifSpecifications.size()][numberNetworks.intValue()];
		
		for(int i = 0; i < numberNetworks; i++){
			log.info("Counting network " + i + "'s frequencies.");
			motifFrequencyBlock = readMotifOutput(motifSpecifications, randomNetworksFolder, inputSubFolder, i, motifFrequencyBlock);
		}
		writeOutput(motifSpecifications, motifFrequencyBlock, outputFile, numberNetworks);
		
		Header.printFooter();
		
	}
	
	private static int[][] readMotifOutput(
			ArrayList<String> motifSpecifications, String randomNetworksFolder,
			String inputSubFolder, int i, int[][] motifFrequencyBlock) {
		
		String thisNetworkOutputFolder = randomNetworksFolder + "random" + i + "/";
		
		for(int j = 0; j < motifSpecifications.size(); j++){
			
			String thisOutputFile = inputSubFolder + "random" + i + "_" + motifSpecifications.get(j) + ".txt";
			int k = 0;
			try {
				log.info("Reading motif output file: " + thisOutputFile);
				
				BufferedReader br = IOUtils.getBufferedReader(thisNetworkOutputFolder + thisOutputFile);
				String lines;
				while ((lines = br.readLine()) != null) {
					k++;
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			motifFrequencyBlock[j][i] = k;
			
		}
		
		return motifFrequencyBlock;
	}

	private static ArrayList<String> readMotifSpecifications(
			String motifSpecificationFile) {

		ArrayList<String> motifSpecifications = new ArrayList<String>();
		try {
			log.info("Reading motif specification file: " + motifSpecificationFile);
			BufferedReader br = IOUtils.getBufferedReader(motifSpecificationFile);
			String lines;
			while ((lines = br.readLine()) != null) {
				motifSpecifications.add(lines);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return motifSpecifications;
	}

	

	private static void writeOutput(
			ArrayList<String> motifSpecifications,
			int[][] motifFrequencyBlock, String outputFile, int numberNetworks) {
		
		log.info("Writing " + outputFile + " to file.");
		try {
			BufferedWriter output = new BufferedWriter(
					new FileWriter(new File(outputFile)));
			try {
					for (int i = 0; i < motifSpecifications.size(); i++) {
						String motif = motifSpecifications.get(i);
						output.write(motif);
						for(int j = 0; j < numberNetworks; j++){
							int frequency = motifFrequencyBlock[i][j];
							output.write(",");
							output.write(Integer.toString(frequency));
							
						}
						output.newLine();
						
					}
			} finally {
				output.close();
				
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}

}
