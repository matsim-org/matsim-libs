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

public class ConvertSpaceToCommaSep {
	private final static Logger log = Logger.getLogger(ConvertSpaceToCommaSep.class);

	public static void main(String args[] ) {
		File inFolder = new File("~/MATSim/workspace/MATSimData/Temp");
		File tempFolder = new File("~/MATSim/workspace/MATSimData/Temp/TempMove");
		boolean checkCreate = tempFolder.mkdir();
		if(!checkCreate){
			log.warn("Could not create " + tempFolder.toString() + ", or it already existed!");
		}
		
		File inFiles[] = inFolder.listFiles(); 

		for(int i = 0; i < inFiles.length ; i++ ){
			if(inFiles[i].isFile() &&
					!inFiles[i].getName().startsWith(".") &&
					!(inFiles[i].getName().equalsIgnoreCase("schema.ini"))) {
					try {
						Scanner input = new Scanner(new BufferedReader(new FileReader( inFiles[i] ) ) );
						BufferedWriter output = new BufferedWriter(new FileWriter(new File(tempFolder.getPath() + "/" + inFiles[i].getName() )));
						try{
						while(input.hasNextLine() ){
							String inString = input.nextLine();
							if(inString.length() > 0) {
								inString = inString.replaceAll(" ", ",");
								output.write(inString);
								output.newLine();
							}
						}
						} finally{
							output.close();
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
		}	
	}
	
	
}
