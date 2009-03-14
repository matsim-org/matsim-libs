package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.TreeSet;

public class QueryWithinAndThroughTraffic {

	// String value that must be set
	final static String PROVINCE = "Gauteng";
	final static double THRESHOLD = 0.9;
	// Mac
	final static String ROOT = "/Users/johanwjoubert/MATSim/workspace/MATSimData/";
	// IVT-Sim0
//	final static String ROOT = "/home/jjoubert/";
	// Derived string values:
	final static String SOURCE = ROOT + PROVINCE + "/Activities/";
	final static String VEHICLE = SOURCE + PROVINCE + "VehicleStats.txt";
	final static String MINOR_IN = SOURCE + PROVINCE + "MinorLocations.txt";
	final static String MAJOR_IN = SOURCE + PROVINCE + "MajorLocations.txt";
	
	final static String OUT_FOLDER = SOURCE + "WithinThrough/";
	final static String MINOR_WITHIN_OUT = OUT_FOLDER + PROVINCE + "MinorWithin.txt";
	final static String MAJOR_WITHIN_OUT = OUT_FOLDER + PROVINCE + "MajorWithin.txt";
	final static String MINOR_THROUGH_OUT = OUT_FOLDER + PROVINCE + "MinorThrough.txt";
	final static String MAJOR_THROUGH_OUT = OUT_FOLDER + PROVINCE + "MajorThrough.txt";
	
	static TreeSet<Integer> withinTree;

	/**
	 * Class to determine the "Within" vehicles for a given province. 
	 * 
	 * The class reads the VehicleStats file of the province, and adds the vehicle ID to
	 * a {@code TreeSet} if the percentage exceeds a preset {@code THRESHOLD} supplied
	 * by the modeller. 
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("==========================================================================================");
		System.out.println("Extracting 'within' and 'through' vehicles for: " + PROVINCE );
		System.out.println();
		
		File OutFolder = new File ( OUT_FOLDER );
		OutFolder.mkdirs();
		
		withinTree = new TreeSet<Integer>();
		
		// Build tree holding 'within' vehicles
		buildTreeSet();

		// Analyze minor activities
		splitInputFile( "minor", MINOR_IN, MINOR_WITHIN_OUT, MINOR_THROUGH_OUT ); 
		
		// Analyse major activities
		splitInputFile( "major", MAJOR_IN, MAJOR_WITHIN_OUT, MAJOR_THROUGH_OUT );
	}

	private static void buildTreeSet() {
		System.out.print("Building TreeSet... ");
		long start = System.currentTimeMillis();
		
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File( VEHICLE ) ) ) );
			@SuppressWarnings("unused")
			String header = input.nextLine();

			while(input.hasNextLine() ){
				String [] line = input.nextLine().split( "," );
				double percentage = Double.parseDouble( line[8] );
				if( percentage >= THRESHOLD ){
					withinTree.add( Integer.parseInt( line[0] ) );
				}
			}			
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		
		long end = System.currentTimeMillis();
		System.out.printf("Done, with %d leaves, in %d millisec.\n",withinTree.size(), end-start );
	}

	private static void splitInputFile(String type, String inputFile, String withinOut, String throughOut ) {
		System.out.printf("Processing %s activities... ", type );
		long start = System.currentTimeMillis();
		try {
			Scanner input = new Scanner(new BufferedReader(new FileReader(new File( inputFile ) ) ) );
			String header = input.nextLine();
			
			BufferedWriter withinOutput = new BufferedWriter(new FileWriter(new File( withinOut ) ) );
			BufferedWriter throughOutput = new BufferedWriter(new FileWriter(new File( throughOut ) ) );
			try{
				withinOutput.write( header + ",HourOfDay");
				withinOutput.newLine();
				throughOutput.write( header + ",HourOfDay");
				throughOutput.newLine();
				
				while(input.hasNextLine() ){
					String line = input.nextLine();
					String [] lineSplit = line.split( "," );
					if(lineSplit.length == 5 ){
						String timeOfDay = lineSplit[3];
						int h = timeOfDay.indexOf("H");
						String hour = timeOfDay.substring(h-2, h);
						if( withinTree.contains( Integer.parseInt( lineSplit[0] ) ) ){
							withinOutput.write( line + "," + hour);
							withinOutput.newLine();
						} else{
							throughOutput.write( line + "," + hour);
							throughOutput.newLine();
						}
					}
				}
				
			} finally{
				withinOutput.close();
				throughOutput.close();
			}
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		long end = System.currentTimeMillis();
		System.out.printf("Done, in %d millisec\n", end-start);
	}

}
