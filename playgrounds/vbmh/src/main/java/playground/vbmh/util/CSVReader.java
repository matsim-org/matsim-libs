package playground.vbmh.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

public class CSVReader {
	public  LinkedList<String[]> readCSV(String fileName){
		return readCSV(fileName, "\t");
	}
	public  LinkedList<String[]> readCSV(String fileName, String trennzeichen){
		BufferedReader reader = null;
		LinkedList<String[]> liste = new LinkedList<String[]>();
		try {
			reader = new BufferedReader(new FileReader(fileName));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String zeile = "";
		
		try {
			zeile = reader.readLine();
			while ((zeile = reader.readLine()) != null) {
				String[] felder = zeile.split(trennzeichen);
				liste.add(felder);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //header
		try {
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return liste;
	}
}
