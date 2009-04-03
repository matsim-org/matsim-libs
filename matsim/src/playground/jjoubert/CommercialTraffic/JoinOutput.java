package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class JoinOutput {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Simple class to join the output files for all vehicles
		String fileType = "VehicleStats";
		
		System.out.println("Joining the two '" + fileType + "' files:");
		System.out.print("   Part 1... ");
		
		String inputFile1 = "/Users/johanwjoubert/MATSim/workspace/MATSimData/SouthAfrica/Part1/SouthAfrica" + fileType + ".txt";
		String inputFile2 = "/Users/johanwjoubert/MATSim/workspace/MATSimData/SouthAfrica/Part2/SouthAfrica" + fileType + ".txt";
		String outputFile = "/Users/johanwjoubert/MATSim/workspace/MATSimData/SouthAfrica/Activities/SouthAfrica" + fileType + ".txt";
		int count = 0;

		try {
			Scanner input1 = new Scanner(new BufferedReader(new FileReader(new File(inputFile1))));
			Scanner input2 = new Scanner(new BufferedReader(new FileReader(new File(inputFile2))));
			
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputFile)));
			

			try{
				String header = input1.nextLine();
				output.write(header);

				while(input1.hasNextLine()){
					String line = input1.nextLine();
					if(line.length() > 5){
						output.write(line);
						output.newLine();
						count++;
					}
				}
				System.out.println("Done.");
				System.out.print("   Part 2... ");
				header = input2.nextLine();
				while(input2.hasNextLine()){
					String line = input2.nextLine();
					if(line.length() > 5){
						output.write(line);
						output.newLine();
						count++;
					}
				}
				System.out.println("Done.");
			} finally{
				output.close();
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
		System.out.printf("\nTotal number of records: %d\n", count);
	}
}
