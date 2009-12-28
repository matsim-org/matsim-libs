package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class ExtractLocationDurations {

	// String value that must be set
	final static String PROVINCE = "SouthAfrica";
	// Mac
	final static String ROOT = "~/MATSim/workspace/MATSimData/";
	// IVT-Sim0
//	final static String ROOT = "/home/jjoubert/";
	// Derived string values:
	final static String INPUT1 = ROOT + PROVINCE + "/Activities/" + PROVINCE + "MajorLocations.txt";
	final static String INPUT2 = ROOT + PROVINCE + "/Activities/" + PROVINCE + "MinorLocations.txt";
	final static String OUTPUT = ROOT + PROVINCE + "/Activities/" + PROVINCE + "ActivityDurations.txt";
	
	final static int bucketSize = 10; // Expressed in MINUTES
	static int numBuckets = (2880 / bucketSize) + 1;
	static ArrayList<Integer> time;
	final static String DELIMITER = ",";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Create an empty time ArrayList
		time = new ArrayList<Integer>();
		for(int a = 1; a <= numBuckets; a++ ){
			time.add(0);
		}
		
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File(OUTPUT)));

			// Major locations
			System.out.print("Processing major activities... ");
			processFile( INPUT1 );
			System.out.printf("Done\nProcessing minor activities... ");
			processFile( INPUT2 );
			System.out.printf("Done\n\nWriting activity durations to file... ");
			try{
				for (int i = 0; i < time.size(); i++) {
					output.write((i+1)*10 + DELIMITER + time.get(i));
					output.newLine();
				}
			}finally{
				output.close();
			}
			System.out.printf("Done.");

		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

	private static void processFile(String inputFile)
			throws FileNotFoundException {
		Scanner input = new Scanner(new BufferedReader(new FileReader(new File( inputFile ))));
		input.nextLine();
		while(input.hasNextLine()){
			String nextLine = input.nextLine();
			if(nextLine.length() > 5){
				String line[] = nextLine.split(DELIMITER);
				int duration = Integer.parseInt(line[4]);
				int position = Math.min(numBuckets - 1, (duration / bucketSize) );
				int dummy = time.get(position);
				time.set(position, dummy + 1);
			}
		}
	}

}
