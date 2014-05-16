package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

import java.util.HashMap;
import java.util.LinkedList;

import playground.wrashid.bsc.vbmh.util.CSVReader;
import playground.wrashid.bsc.vbmh.util.ReadParkhistory;
import playground.wrashid.bsc.vbmh.util.RemoveDuplicate;

public class Diagnose {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ReadParkhistory hist = new ReadParkhistory();
		hist.readXML("parkhistory_200.xml");
		LinkedList<HashMap<String, String>> list = hist.getAllEventByAttribute("parking", "9000006");
		ReadParkhistory a = hist.getSubHist(list);
		LinkedList<HashMap<String, String>> list1 = a.getAllEventByAttribute("spotType", "ev");
		a=hist.getSubHist(list1);
		LinkedList<HashMap<String, String>> list2 = a.getAllEventByAttribute("eventtype", "EV_left");
		
		for(HashMap<String, String> events:list2){
			
		}
		
		System.out.println(list2.size());
		
		
		
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
