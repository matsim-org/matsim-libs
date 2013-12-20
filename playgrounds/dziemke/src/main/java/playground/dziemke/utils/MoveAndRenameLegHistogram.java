package playground.dziemke.utils;

import java.io.File;
import java.io.IOException;


import org.apache.log4j.Logger;

public class MoveAndRenameLegHistogram {
	private final static Logger log = Logger.getLogger(MoveAndRenameLegHistogram.class);
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// Parameters
		String runId = "run_132c";
		int iterationNumber = 150;
		
		// Input file and output directory
		String inputFile = "D:/Workspace/container/demand/output/" + runId + "/ITERS/it." + iterationNumber
				+ "/" + runId + "." + iterationNumber + ".legHistogram_all.png";
		String outputDirectory = "D:/VSP/Masterarbeit/Images/" + runId + "/";
		String outputFile = outputDirectory + "legHistogram.png";
		
		// Copy the file
		new File(outputDirectory).mkdir();
		//TODO Copy the file
		
		log.info("Done creating the copied file " + outputFile + ".");
	}
}
