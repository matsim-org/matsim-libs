package playground.artemc.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVReader {

	public static ArrayList<String[]> readCSV(String filePath, String splitString) throws IOException{
		BufferedReader CSVFile = new BufferedReader(new FileReader(filePath));
		String dataRow = CSVFile.readLine();
		ArrayList<String[]> lineList = new ArrayList<String[]>();
		while (dataRow != null){
			lineList.add(dataRow.substring(0, dataRow.length()).split(splitString));
			dataRow = CSVFile.readLine();
		}

		return lineList;
	}

	public static ArrayList<String[]> readCSVskip1stLine(String filePath, String splitString) throws IOException{
		BufferedReader CSVFile = new BufferedReader(new FileReader(filePath));
		String dataRow = CSVFile.readLine();
		dataRow = CSVFile.readLine();
		ArrayList<String[]> lineList = new ArrayList<String[]>();
		while (dataRow != null){
			lineList.add(dataRow.substring(0, dataRow.length()).split(splitString));
			dataRow = CSVFile.readLine();
		}

		return lineList;
	}
}