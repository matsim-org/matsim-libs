package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

import java.util.HashMap;
import java.util.LinkedList;

import playground.wrashid.bsc.vbmh.util.CSVReader;
import playground.wrashid.bsc.vbmh.util.ReadParkhistory;

public class Diagnose {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ReadParkhistory hist = new ReadParkhistory();
		hist.readXML("parkhistory_67.xml");
		CSVReader csv = new CSVReader();
		LinkedList<String[]> walking = csv.readCSV("walking.csv", ",");
		ReadParkhistory subHist =new ReadParkhistory();
		
		
		
		
		
		
//		
//		for (String[] line : walking) {
//			if (Double.parseDouble(line[0]) > 64000 && Double.parseDouble(line[0]) < 66000 && Double.parseDouble(line[1]) > 675) {
//				System.out.println(line[0] + ", " + line[1]);
//				LinkedList<HashMap<String, String>> lines = hist.getAllEventByAttribute("time", line[0]);
//				for (HashMap<String, String> values : lines) {
//					for(String string : values.values()){
//						if(string.contains("parked")){
//							//System.out.println(values.values());
//							 subHist = hist.getSubHist(lines);
//						}
//					}
//				}
//				//System.out.println(subHist.getEventByAttribute("parking", "39190"));
//			}
//
//		}
		
		
	}

}
