package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.log4j.Logger;

public class JoinOutput {

	private final static Logger log = Logger.getLogger(JoinOutput.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Simple class to join the output files for all vehicles
		String fileType = "MinorLocations";
		
		log.info("Joining the two '" + fileType + "' files:");
		log.info("Part 1...");
		
		String inputFile1 = "~/MATSim/workspace/MATSimData/SouthAfrica/Part1/SouthAfrica" + fileType + ".txt";
		String inputFile2 = "~/MATSim/workspace/MATSimData/SouthAfrica/Part2/SouthAfrica" + fileType + ".txt";
		String outputFile = "~/MATSim/workspace/MATSimData/SouthAfrica/Activities/SouthAfrica" + fileType + ".txt";
		int count = 0;

		try {
			Scanner input1 = new Scanner(new BufferedReader(new FileReader(new File(inputFile1))));
			Scanner input2 = new Scanner(new BufferedReader(new FileReader(new File(inputFile2))));
			
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFile)));
			

			try{
				String header = input1.nextLine();
				output.write(header);
				output.newLine();

				while(input1.hasNextLine()){
					String line = input1.nextLine();
					if(line.length() > 5){
						output.write(line);
						output.newLine();
						count++;
					}
				}
				log.info("Done with Part 1.");
				log.info("Part 2... ");
				input2.nextLine();
				while(input2.hasNextLine()){
					String line = input2.nextLine();
					if(line.length() > 5){
						output.write(line);
						output.newLine();
						count++;
					}
				}
				log.info("Done with Part 2.");
			} finally{
				output.close();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		log.info("Total number of records: " + count);
	}
}
