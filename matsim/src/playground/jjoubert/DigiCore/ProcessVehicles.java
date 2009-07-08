package playground.jjoubert.DigiCore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import playground.jjoubert.DateString;

public class ProcessVehicles {
	private static String delimiter = ","; 
	private static String root = "/home/jjoubert/DigiCore/";
	private static long startLine = 0;
	private static long numberOfLinesToRead = Long.MAX_VALUE;
	
	// main method
	public static void main( String[] args){

		System.out.println("=================================================================");
		System.out.println("  Splitting the DigiCore data file into seperate vehicle files.");
		System.out.println("=================================================================");
		System.out.println();
		long line = 0;
		long linesRead = 0;
		long reportValue = 1;

		File outputFoler = new File(root + "Vehicles/");
		if(outputFoler.exists()){
			System.err.printf("The folder %s already exists! Delete, and rerun.", outputFoler.getPath());
		} else{
			outputFoler.mkdirs();
		}		

		Scanner input = null;
		BufferedWriter output = null;
		String vehicleFile = null;
		
		DateString ds = new DateString();
		try {
			BufferedWriter logRecords = new BufferedWriter(new FileWriter(new File(root + "logRecordsRead_" + ds.toString() + ".txt")));
			try{
				try {
					input = new Scanner(new BufferedReader(new FileReader(new File( root + "Poslog_Research_Data.csv"))));

					if( input.hasNextLine() ){			

						// Read the first line, and split the fields based on the comma separator.
						String [] inputString = input.nextLine().split(delimiter);
						line++;

						if(line >= startLine || linesRead <= numberOfLinesToRead){
							if(inputString.length == 6){
								// Open the file for the vehicle 
								vehicleFile = root + "Vehicles/" + inputString[0] + ".txt";
								output = new BufferedWriter(new FileWriter(vehicleFile, true) , 10000 );

								// Write the record to the associated file
								output.write(inputString[0]); // Vehicle ID
								output.write(delimiter);
								output.write(inputString[1]); // Time stamp
								output.write(delimiter);
								output.write(inputString[2]); // X (longitude)
								output.write(delimiter);
								output.write(inputString[3]); // Y (latitude)
								output.write(delimiter);
								output.write(inputString[4]); // Status
								output.write(delimiter);
								output.write(inputString[5]); // Speed
								output.newLine();

								linesRead++;
								logRecords.write(String.valueOf(linesRead));
								logRecords.newLine();

								// Update report
								if(linesRead == reportValue){
									System.out.println(String.valueOf(linesRead));
									System.out.printf("   Lines read... ");
									reportValue *= 2;
								}
							}


							String vehID = inputString[0];
							while ( input.hasNextLine() ){

								// Read the next input line
								inputString = input.nextLine().split(delimiter);
								line++;

								if(line >= startLine || linesRead <= numberOfLinesToRead){
									if(inputString.length == 6){
										if ( !vehID.equalsIgnoreCase(inputString[0])) {
											// Close the file for the current vehicle.
											output.close();

											// Open the file for the new vehicle.
											vehicleFile = root + "Vehicles/" + inputString[0] + ".txt";
											try {
												output = new BufferedWriter(new FileWriter(vehicleFile, true) , 100000 );
											} catch (IOException e) {
												e.printStackTrace();
											}			
										}

										// Write the record to the new file
										output.write(inputString[0]); // Vehicle ID
										output.write(delimiter);
										output.write(inputString[1]); // Time stamp
										output.write(delimiter);
										output.write(inputString[2]); // X (longitude)
										output.write(delimiter);
										output.write(inputString[3]); // Y (latitude)
										output.write(delimiter);
										output.write(inputString[4]); // Status
										output.write(delimiter);
										output.write(inputString[5]); // Speed
										output.newLine();

										linesRead++;

										vehID = inputString[0];

										// Update report
										if(linesRead == reportValue){
											System.out.println(String.valueOf(linesRead));
											System.out.printf("   Lines read... ");
											reportValue *= 2;
										}
									}
								} else{
									line++;
								}

							}
						} else{
							line++;
						}
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} finally{
				logRecords.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
					
		System.out.print(String.valueOf(linesRead));
		System.out.printf(" (Done)\n");
	}
}
