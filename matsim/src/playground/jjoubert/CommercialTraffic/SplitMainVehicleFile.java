package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
/**
 * @author johanwjoubert
 */
public class SplitMainVehicleFile {
	
	// main method
	public static void main( String[] args) throws IOException{
		Scanner input = new Scanner( new BufferedReader( new FileReader( 
				new File( "/Volumes/Data/DigiCore/Poslog_Research_Data.csv") ) ) );
			
		int numRecords = 1;
		
		if( input.hasNextLine() ){			
			
			// Read the first line, and split the fields based on the comma separator.
			String [] inputString = input.nextLine().split(",");		
						
			// Open the file for the vehicle 
			String vehFile = "/Volumes/Data/DigiCore/VehicleFiles/" + inputString[0] + ".txt";
			BufferedWriter output = new BufferedWriter(new FileWriter(vehFile, true) , 100000 );
			
			// Write the record to the associated file
			String outputString = 	inputString[0] + " " + // vehID
									inputString[1] + " " + // time
									inputString[2] + " " + // xCoord, longitude
									inputString[3] + " " + // yCoord, latitude 
									inputString[4] + " " + // status 
									inputString[5];        // speed
			output.write(outputString);
			String vehID = inputString[0];

			while ( input.hasNextLine() ){
				numRecords += 1;
				
				// Read the next input line
				inputString = input.nextLine().split(",");
								
				if ( !vehID.equals(inputString[0]) ) {
					// Close the file for the current vehicle.
					output.close();
					
					// Open the file for the new vehicle .
					vehFile = "/Volumes/Data/DigiCore/VehicleFiles/" + inputString[0] + ".txt";
					output = new BufferedWriter(new FileWriter(vehFile, true) , 100000 );			
				}		
				// Write the record to the new file
				outputString = 	inputString[0] + " " + // vehID
								inputString[1] + " " + // time
								inputString[2] + " " + // xCoord 
								inputString[3] + " " + // yCoord 
								inputString[4] + " " + // status 
								inputString[5];        // speed
				output.write(outputString);
				output.newLine();

				vehID = inputString[0];	
			}
		}					
		
		if( input != null ) {
			input.close();
		}
		
		String outputString = "There was a total of " + numRecords + " log records.";
		System.out.println( outputString );
	}

}
