package playgroundMeng.ptAccessabilityAnalysis.run;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class cleanKPI {
	public static void main(String[] args) throws IOException {
	File inputFile = new File("C:/Users/VW3RCOM/Desktop/MengptAnalysisOutputFileGrid_3600.0_10000/Time2District2KPIForTimeManager.csv");
	File outputFile = new File("C:/Users/VW3RCOM/Desktop/MengptAnalysisOutputFileGrid_3600.0_10000/CleanedTime2District2KPIForTimeManager.csv");
	String line = "";
	
	FileReader fileReader = new FileReader(inputFile);
	BufferedReader bufferedReader = new BufferedReader(fileReader);
	
	FileWriter fileWriter = new FileWriter(outputFile);
	BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
	line = bufferedReader.readLine();
	bufferedWriter.write(line);
	
	
	while ((line = bufferedReader.readLine()) != null) {
		
		String[] splitStrings = line.split("/");
		int a = splitStrings.length -1;
		if(Double.valueOf(splitStrings[a]) != -1.0) {
			bufferedWriter.newLine();
			bufferedWriter.write(line);
		
		}
		
	}
	bufferedWriter.close();
	System.out.println("finish");
	}
}
