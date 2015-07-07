package playground.smeintjes.ismags;

import java.io.BufferedReader;
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
 * at a time using the ISMAGS algorithm. By default, ISMAGS only looks for one 
 * specified motif per run. 
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
public class RunMultipleMotifs {

	private static Logger log = Logger.getLogger(RunMultipleMotifs.class);
	
	/**
	 * @param String path to file containing list of all motif specifications 
	 * to look for (including file name and extension)
	 * @param String path to the folder containing one- and bidirectional edges 
	 * @param String name of the output file (will be written to same folder)
	 */
	public static void main(String[] args) {

		Header.printHeader(RunMultipleMotifs.class.toString(), args);
		String motifSpecificationPath = args[0];
		String pathToFolder = args[1];
		String outputPath = args[2];
		String networkNumber = args[3];

		ArrayList<String> motifList = readMotifs(motifSpecificationPath);
		runMotifs(motifList, pathToFolder, outputPath, networkNumber);
		
		Header.printFooter();
	}
	
	private static int countMotifs(String pathToFolder, String networkNumber, String motif, int j) {
log.info("Reading motif list from " + pathToFolder + networkNumber);
		
		int i = 0;
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(pathToFolder + networkNumber + "_" + motif + "_" + j + ".txt");
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
			String pathToFolder, String outputPath, String networkNumber) {
		
		String foldername = pathToFolder;
		/* When network contains both onedirectional and bidirectional edges */
		String linkfiles  = "\"A d A A onedirectional.txt X u X X bidirectional.txt\""; //Remember to change motif file!
		/* When network only contains onedirectional edges */
//		String linkfiles  = "\"A d A A onedirectional.txt\""; //Remember to change motif file!
		int totalMotifs = 0;
		int j = 0;
		for (String motif : motifList) {
			String output = outputPath + networkNumber + "_" + motif + "_" + j + ".txt";
			 String[] ar = new String[]{"-folder", foldername, "-linkfiles", linkfiles, "-output", output, "-motif", motif};
	        try {
				CommandLineInterface.main(ar);
				totalMotifs = totalMotifs + countMotifs(pathToFolder, networkNumber, motif, j);
			} catch (IOException e) {
				log.info("Cannot run CommandLineInterface.");
				e.printStackTrace();
			}
	        j++;
		}
		log.info("Total number of motifs ---------------> " + totalMotifs);
		
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
