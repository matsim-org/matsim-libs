package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class ProduceTimeOfDayHistogram {
	final static String [] areaList = { "Gauteng", "KZN", "WesternCape" };
	final static String [] fileType = { "MinorWithin", "MinorThrough", "MajorWithin", "MajorThrough" };
	// Mac
	final static String ROOT = "~/MATSim/workspace/MATSimData/";
	// IVT-Sim0
//	final static String ROOT = "~/";
	// Derived string values:
	final static String OUTPUT = ROOT + "Temp/TimeOfDayHistogram.txt";
	
	public static void main( String [] args ){
		System.out.println("Generating histogram data for...");
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter(new File( OUTPUT ) ) );

			try{
				for (String area : areaList) {
					for (String file : fileType) {
						int hist[] = new int[24];
						System.out.printf("... %s %s\n", area, file);
						
						String filename = area + file + ".txt";
						Scanner input = new Scanner(new BufferedReader(new FileReader(
								new File( ROOT + area + "/Activities/WithinThrough/" + filename))));
						@SuppressWarnings("unused")
						String header = input.nextLine();
						
						while( input.hasNextLine() ){
							String [] line = input.nextLine().split(",");
							if(line.length == 6){
								int hour = Integer.parseInt(line[5]);
								hist[hour]++;
							}
						}
						output.write("Histogram data for file " + filename);
						output.newLine();
						for(int i = 0; i < 24; i++){
							output.write(i + "," + hist[i] );
							output.newLine();
						}
						output.newLine();
					}
				}
			} finally{
				output.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
