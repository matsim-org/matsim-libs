package playground.artemc.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {

	public static ArrayList<String[]> readCSV(String filePath) throws IOException{
		BufferedReader CSVFile = new BufferedReader(new FileReader(filePath));
		String dataRow = CSVFile.readLine(); 
		ArrayList<String[]> lineList = new ArrayList<String[]>();		
		while (dataRow != null){
			lineList.add(dataRow.substring(0, dataRow.length()).split(","));
			dataRow = CSVFile.readLine(); 
		}

		return lineList;
	}
}