package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class ExpandHourOfDay {

	final static String DELIMETER = ",";
	
	public static void main ( String args[] ){
		System.out.print("Starting the analysis... ");
		
		ArrayList<Integer> hourOfDay = new ArrayList<Integer>();
		for (int i = 0; i < 24; i++) {
			hourOfDay.add(i, 0);
		}
		
		
		try{
			Scanner inFile = new Scanner( new BufferedReader( new FileReader( new File("/Users/johanwjoubert/MATSim/workspace/MATSimData/GautengVehicles/Activities/20090214-2230/hourOfDayInGautengStats.txt"))));
			String[] header = inFile.nextLine().split(DELIMETER);
			

//			BufferedWriter outFile = new BufferedWriter( new FileWriter( new File("/Users/johanwjoubert/MATSim/workspace/MATSimData/GautengVehicles/Activities/20090214-2230/hourOfDayInGautengStatsForGIS-D.txt")));
//			outFile.write(expandedHeaderString(header) );
//			outFile.newLine();

			while( inFile.hasNextLine() ){
				String[] theString = inFile.nextLine().split(DELIMETER);
				if( theString.length == 5 ){
					int theValue = hourOfDay.get( Integer.parseInt( theString[3] ) );
					theValue++;
					hourOfDay.set(Integer.parseInt( theString[3] ), theValue);
//					expandedString(outFile, theString);				
				} else{
					System.out.println("Oops... not length 5");
				}
			}
//			outFile.close();
			
		} catch(Exception e){
			e.printStackTrace();
		} finally{
			System.out.println("Done ");
			System.out.println();
		}
		for(int i = 0; i < hourOfDay.size(); i++ ){
			System.out.println(i + hourOfDay.get(i) );
		}
	}

	private static void expandedString(BufferedWriter outFile,
			String[] theString) throws IOException {
		String VehID = theString[0];
		String Long = theString[1];
		String Lat = theString[2];
		int h = Integer.parseInt( theString[3] );
		int d = Integer.parseInt( theString[4] );
//					Integer [] H = new Integer[24];
		Integer [] D = new Integer[24];
//					for (int i = 0; i < H.length; i++) {
//						 H[i] = 0;
//					};
		for (int j = 0; j < D.length; j++) {
			D[j] = 0;
		}
//					H[h] = 1;
		D[h] = d;
		
		outFile.write( VehID + DELIMETER +
					   Long + DELIMETER +
					   Lat + DELIMETER +
					   h + DELIMETER +
					   d + DELIMETER );
//					for( int i = 0; i < 23; i++ ){
//						outFile.write( H[i] + DELIMETER );
//					}
		for( int j = 0; j < 23; j++ ){
			outFile.write( D[j] + DELIMETER );
		}
//					outFile.write( H[23] );
		outFile.write( D[23] );
		
		outFile.newLine();
	}

	private static String expandedHeaderString(String[] header) {
		return header[0] + DELIMETER +
					  header[1] + DELIMETER +
					  header[2] + DELIMETER +
					  header[3] + DELIMETER +
					  header[4] + DELIMETER +
//						  "H00" + DELIMETER +
//						  "H01" + DELIMETER +
//						  "H02" + DELIMETER +
//						  "H03" + DELIMETER +
//						  "H04" + DELIMETER +
//						  "H05" + DELIMETER +
//						  "H06" + DELIMETER +
//						  "H07" + DELIMETER +
//						  "H08" + DELIMETER +
//						  "H09" + DELIMETER +
//						  "H10" + DELIMETER +
//						  "H11" + DELIMETER +
//						  "H12" + DELIMETER +
//						  "H13" + DELIMETER +
//						  "H14" + DELIMETER +
//						  "H15" + DELIMETER +
//						  "H16" + DELIMETER +
//						  "H17" + DELIMETER +
//						  "H18" + DELIMETER +
//						  "H19" + DELIMETER +
//						  "H20" + DELIMETER +
//						  "H21" + DELIMETER +
//						  "H22" + DELIMETER +
//						  "H23" );

					  "D00" + DELIMETER +
					  "D01" + DELIMETER +
					  "D02" + DELIMETER +
					  "D03" + DELIMETER +
					  "D04" + DELIMETER +
					  "D05" + DELIMETER +
					  "D06" + DELIMETER +
					  "D07" + DELIMETER +
					  "D08" + DELIMETER +
					  "D09" + DELIMETER +
					  "D10" + DELIMETER +
					  "D11" + DELIMETER +
					  "D12" + DELIMETER +
					  "D13" + DELIMETER +
					  "D14" + DELIMETER +
					  "D15" + DELIMETER +
					  "D16" + DELIMETER +
					  "D17" + DELIMETER +
					  "D18" + DELIMETER +
					  "D19" + DELIMETER +
					  "D20" + DELIMETER +
					  "D21" + DELIMETER +
					  "D22" + DELIMETER +
					  "D23";
	}	
}
