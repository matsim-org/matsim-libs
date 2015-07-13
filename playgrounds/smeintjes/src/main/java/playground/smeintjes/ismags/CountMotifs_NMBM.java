package playground.smeintjes.ismags;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;
//TODO rewrite code to read NMBM files
/**
 * This class calculates, for all random networks, the number of each type of
 * motif and writes these frequencies to file.
 * 
 * @param String input motif specification file (a list containing all possible 
 * motif specifications for a motif of certain size) (example "motifSpecifications/3-node-motifSpecifications.txt")
 * @param String path to folder containing random networks ("20_20_NMBM/")
 * @param String path to output file (example "nmbm-3-node-frequencies.txt")
 * @param String the name of the subfolder containing the motif results (example "3-node-motifs/" when looking at 3-node motifs)
 * 
 * @author sumarie
 *
 9+*/
public class CountMotifs_NMBM {

	private static Logger log = Logger.getLogger(CountMotifs_NMBM.class);
	/** 
	 * @param args
	 */
	public static void main(String[] args) {

		Header.printHeader(CountMotifs_NMBM.class.toString(), args);
		
		String motifSpecificationFile = args[0];
		String networkFolder = args[1];
		String outputFile = args[2];
		String inputSubFolder = args[3];
		
		
		ArrayList<String> motifSpecifications = readMotifSpecifications(motifSpecificationFile);
		
		int[][] motifFrequencyBlock = new int[motifSpecifications.size()][1];

		motifFrequencyBlock = readMotifOutput(motifSpecifications, networkFolder, inputSubFolder, motifFrequencyBlock);

		writeOutput(motifSpecifications, motifFrequencyBlock, outputFile);
		
		Header.printFooter();
		
	}
	
	private static int[][] readMotifOutput(
			ArrayList<String> motifSpecifications, String nmbmNetworksFolder,
			String inputSubFolder, int[][] motifFrequencyBlock) {
		
		for(int j = 0; j < motifSpecifications.size(); j++){

			String thisOutputFile = nmbmNetworksFolder + inputSubFolder + "20_20_NMBM_" + motifSpecifications.get(j) + ".txt";
			int k = 0;
			try {
				log.info("Reading motif output file: " + thisOutputFile);
				
					BufferedReader br = IOUtils.getBufferedReader(thisOutputFile);
					
					String lines;
					while ((lines = br.readLine()) != null) {
						k++;
					}
 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			motifFrequencyBlock[j][0] = k;
			
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
			int[][] motifFrequencyBlock, String outputFile) {
		
		log.info("Writing " + outputFile + " to file.");
		try {
			BufferedWriter output = new BufferedWriter(
					new FileWriter(new File(outputFile)));
			try {
					for (int i = 0; i < motifSpecifications.size(); i++) {
						String motif = motifSpecifications.get(i);
						output.write(motif);
						int frequency = motifFrequencyBlock[i][0];
						output.write(",");
						output.write(Integer.toString(frequency));	
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
