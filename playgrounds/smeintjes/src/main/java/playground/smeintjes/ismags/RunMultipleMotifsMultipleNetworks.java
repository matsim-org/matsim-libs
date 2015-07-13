package playground.smeintjes.ismags;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.geom.Coordinate;



import playground.southafrica.utilities.Header;

/**
 * This class can be used when looking for more than one motif configuration 
 * in multiple networks. By default, ISMAGS only looks for one specified motif 
 * per run, taking one network as input.
 * 
 *  By adding a for loop to the RunMultipleMotifs class, this class is able to
 *  look for all motifs of a specified size, for all random networks (currently,
 *  there are 100 on Hobbes).
 * 
 * This class takes as input a list of motif specifications (one per line) that should be 
 * searched for. Motifs should be in motif specification format, as described in 
 * Demeyer, S. et al. (2013). The index-based subgraph matching algorithm (ISMA):
 * Fast subgraph enumeration in large networks using optimized search trees. PLOS 
 * ONE. Vol. 8, Issue 4.
 * 
 * @author sumarie
 *
 */
public class RunMultipleMotifsMultipleNetworks {

	private static Logger log = Logger.getLogger(RunMultipleMotifsMultipleNetworks.class);
	
	/**
	 * @param String path to file containing list of all motif specifications 
	 * to look for (including file name and extension) ("motifSpecifications/3-node-motifSpecification.txt", for example,
	 * when in run10-ismags/ on Hobbes)
	 * @param String path to parent folder containing the random network subfolders 
	 * (which contain the one- and bidirectional edges files) ("randomNetworks/" on Hobbes)
	 * @param String the name of the output folder (on the same path as second argument) to where
	 * output files will be written ("3-node-motifs/" on Hobbes, for example)
	 * @param Integer number of random networks on which class must be run (currently, 100 on Hobbes)
	 * @param Integer the integer of the first network that should be analysed
	 */
	public static void main(String[] args) {

		Header.printHeader(RunMultipleMotifsMultipleNetworks.class.toString(), args);
		String motifSpecificationPath = args[0];
		String pathToParentFolder = args[1];
		String outputFolder = args[2]; //In each random network folder, there are two folders, one for 3-node-motifs and one for 4-node-motifs
		Integer numberNetworks = Integer.parseInt(args[3]);
		Integer startNetwork = Integer.parseInt(args[4]);

		ArrayList<String> motifList = readMotifs(motifSpecificationPath);
		for(int i = startNetwork.intValue(); i < numberNetworks; i++){
			log.info("Looking for motifs in network " + i);
			runMotifs(motifList, pathToParentFolder, outputFolder, i);
		}
		
		Header.printFooter();
	}
	
	private static int countMotifs(String pathToFolder, int networkNumber, String motif, int j) {
		
		log.info("Reading motif list from " + pathToFolder + networkNumber);
		
		int i = 0;
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(pathToFolder + "random" + networkNumber + "_" + motif + ".txt");
			String lines;
			while ((lines = br.readLine()) != null) {
				i++;
			}
			j++;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return i;
	}

	private static void runMotifs(ArrayList<String> motifList,
			String pathToFolder, String outputFolder, int networkNumber) {
		
		String foldername = pathToFolder + "random" + networkNumber + "/";
		/* When network contains both onedirectional and bidirectional edges */
		String linkfiles  = "\"A d A A random" + networkNumber + "_onedirectional.txt X u X X random" + networkNumber + "_bidirectional.txt\""; //Remember to change motif file!
		/* When network only contains onedirectional edges */
//		String linkfiles  = "\"A d A A onedirectional.txt\""; //Remember to change motif file!
		int totalMotifs = 0;
		int j = 0;
		for (String motif : motifList) {
			String output = foldername + outputFolder + "random" + networkNumber + "_" + motif + ".txt";
			 String[] ar = new String[]{"-folder", foldername, "-linkfiles", linkfiles, "-output", output, "-motif", motif};
	        try {
				CommandLineInterface.main(ar);
//				totalMotifs = totalMotifs + countMotifs(foldername, networkNumber, motif, j);
			} catch (IOException e) {
				log.info("Cannot run CommandLineInterface.");
				e.printStackTrace();
			}
	        j++;
		}
//		log.info("Total number of motifs ---------------> " + totalMotifs);
		
	}

	public static ArrayList<String> readMotifs(String motifPath) {
		
		ArrayList<String> motifList = new ArrayList<String>();
		
		log.info("Reading motif lists from " + motifPath);
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(motifPath);
			String lines;
			while ((lines = br.readLine()) != null) {
				motifList.add(lines);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return motifList;
		
	}

}
