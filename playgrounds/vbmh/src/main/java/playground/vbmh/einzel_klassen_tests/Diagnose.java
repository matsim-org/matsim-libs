package playground.vbmh.einzel_klassen_tests;

import java.util.HashMap;
import java.util.LinkedList;

import playground.vbmh.util.CSVReader;
import playground.vbmh.util.CSVWriter;
import playground.vbmh.util.ReadParkhistory;
import playground.vbmh.util.RemoveDuplicate;
import playground.vbmh.vmParking.ParkHistoryWriter;

public class Diagnose {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		
		//---------------------
		int count=0;
		ReadParkhistory hist = new ReadParkhistory();
//		hist.readXML("parkhistory_200.xml");
		if(1==0){
			hist.readXML("alles_null_sa/parkhistory/parkhistory_200.xml");
			System.out.println("alles_null gelesen");
		}
		if(1==0){
			hist.readXML("normal_sa/parkhistory/parkhistory_200.xml");
			System.out.println("normal gelesen");
		}
		if(1==0){
			hist.readXML("erste_min_5_sa/parkhistory/parkhistory_200.xml");
			System.out.println("erste_min_5 gelesen");
		}
		if(1==0){
			hist.readXML("null_ev_exc_sa/parkhistory/parkhistory_200.xml");
			System.out.println("ev_exc gelesen");
		}
		
		LinkedList<HashMap<String, String>> list = hist.getAllEventByAttribute("eventtype", "occupied");
		ReadParkhistory a = hist.getSubHist(list);
//		LinkedList<HashMap<String, String>> list1 = a.getAllEventByAttribute("spotType", "ev");
//		a=hist.getSubHist(list1);
//		LinkedList<HashMap<String, String>> list2 = a.getAllEventByAttribute("eventtype", "EV_left");
		
//		LinkedList<HashMap<String, String>> hasToCharge = hist.getAllEventByAttribute("eventtype", "Agent_looking_for_parking_has_to_charge");
//		ReadParkhistory hasTo = new ReadParkhistory();
//		
//		hasTo.events=hasToCharge;
//		
//		for(HashMap<String, String> events:list){
//			String id = events.get("person");
////			for(HashMap<String, String> person:hasToCharge){
//				if(hasTo.getAllEventByAttribute("person", id)!=null){
//					System.out.println(events.toString());
//					count++;
//				}
////				if(person.containsValue(id)){
////					count++;
////				}
//			}
//		}
		
		for(HashMap<String, String> event:list){
			if(event.get("Parkingid").contains("900000")){
				System.out.println(event.toString());
			}
		}
		
		
//		System.out.println(list.size());
		System.out.println(count);
		
		
		//////----------------------------------------
//		
		
		
		//--------------------------------------------------
		
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
