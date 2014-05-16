package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

import java.util.HashMap;
import java.util.LinkedList;

import playground.wrashid.bsc.vbmh.util.CSVReader;
import playground.wrashid.bsc.vbmh.util.CSVWriter;
import playground.wrashid.bsc.vbmh.util.ReadParkhistory;
import playground.wrashid.bsc.vbmh.util.RemoveDuplicate;

public class DiagnoseMoritz {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ReadParkhistory hist = new ReadParkhistory();
		hist.readXML("parkhistory_200.xml");
		LinkedList<HashMap<String, String>> list = hist.getAllEventByAttribute("parking", "9000006");
		ReadParkhistory a = hist.getSubHist(list);
//		LinkedList<HashMap<String, String>> list1 = a.getAllEventByAttribute("spotType", "ev");
//		a=hist.getSubHist(list1);
//		LinkedList<HashMap<String, String>> list2 = a.getAllEventByAttribute("eventtype", "EV_left");
////		
//		CSVWriter writer = new CSVWriter("occupancyPL6");
//		
//		Integer evs=0;
//		Integer nevs=0;
//		writer.writeLine("time\t evs\t nevs");
//		Double evsa = 0.0;
//		Double nevsa = 0.0;
//		double klein = 0;
//		for(HashMap<String, String> event:a.events){
//			if(event.get("spotType").equals("ev") && (event.get("eventtype").equals("EV_left") || event.get("eventtype").equals("NEV_left"))){
//				evs--;
//				
//			}
//			
//			if(event.get("spotType").equals("nev") && (event.get("eventtype").equals("EV_left") || event.get("eventtype").equals("NEV_left"))){
//				nevs--;
//			}
//			
//			if(event.get("spotType").equals("ev") && (event.get("eventtype").equals("EV_parked") || event.get("eventtype").equals("NEV_parked"))){
//				evs++;
//			}
//			
//			if(event.get("spotType").equals("nev") && (event.get("eventtype").equals("EV_parked") || event.get("eventtype").equals("NEV_parked"))){
//				nevs++;
//			}
//					
//			Double time = Double.parseDouble(event.get("time"));
//			
//			evsa=evs/200.0;
//			nevsa=nevs/800.0;
//			writer.writeLine(time.toString()+"\t "+evsa.toString()+"\t "+nevsa.toString());
//			System.out.println(klein);
//		}
//		
//		writer.close();
//		
		
		LinkedList<HashMap<String, String>> stranger = hist.getAllEventByAttribute("person", "28164_2");
		for(HashMap<String, String> ereignis : stranger){
			System.out.println(ereignis.toString());
			
		}
		
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
