package playground.jjoubert.DigiCore;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Formatter;
import java.util.Scanner;

public class ReadOneVehicle {
	
	// main method
	public void main( String[] args) throws FileNotFoundException{
		Scanner input = new Scanner( new File( "/Volumes/Data/DigiCore/Poslog_Research_Data.csv") );
		Formatter output = new Formatter( "/Volumes/Data/DigiCore/102076.txt" );
			
		int numRecords = 0;
		int writtenRecords = 0;
				
		while ( input.hasNextLine() ){
			numRecords += 1;
			
			int vehID = input.nextInt();
			int time = input.nextInt();
			double xCoord = input.nextDouble();
			double yCoord = input.nextDouble();
			int status = input.nextInt();
			double speed = input.nextDouble();
			
			if (vehID == 102076) {
				writtenRecords += 1;
				output.format("%d %d %f %f %d %d\n", vehID, time, xCoord, yCoord, status, speed);					
			}
		}
		
		if( input != null ) {
			input.close();
		}
		if( output != null ){
			output.close();
		}
		
		String outputString1 = "There was a total of " + numRecords + " log records.";
		String outputString2 = "There was a total of " + writtenRecords + " printed to file.";
		System.out.println( outputString1 );
		System.out.println( outputString2 );
	}

}
