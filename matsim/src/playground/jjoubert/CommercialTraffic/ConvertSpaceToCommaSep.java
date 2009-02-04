package playground.jjoubert.CommercialTraffic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

public class ConvertSpaceToCommaSep {
	
	public static void main(String args[] ) {
		File inFolder = new File("/Users/johanwjoubert/MATSim/workspace/MATSimData/Temp");
		File tempFolder = new File("/Users/johanwjoubert/MATSim/workspace/MATSimData/Temp/TempMove");
		tempFolder.mkdir();
		
		File inFiles[] = inFolder.listFiles(); 

		for(int i = 0; i < inFiles.length ; i++ ){
			if(inFiles[i].isFile() &&
					!inFiles[i].getName().startsWith(".") &&
					!(inFiles[i].getName() == "schema.ini") ) {
				try {
					Scanner input = new Scanner(new BufferedReader(new FileReader( inFiles[i] ) ) );
					BufferedWriter output = new BufferedWriter(new FileWriter(new File(tempFolder.getPath() + "/" + inFiles[i].getName() )));

					while(input.hasNextLine() ){
						String inString = input.nextLine();
						if(inString.length() > 0) {
							inString = inString.replaceAll(" ", ",");
							output.write(inString);
							output.newLine();
						}
					}
					output.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}	
	}
	
	
}
