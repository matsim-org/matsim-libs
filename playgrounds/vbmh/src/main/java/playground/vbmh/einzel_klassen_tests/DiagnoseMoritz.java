package playground.vbmh.einzel_klassen_tests;

import java.util.HashMap;
import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.vbmh.util.CSVWriter;
import playground.vbmh.util.ReadParkhistory;
import playground.vbmh.vmEV.EVControl;
@SuppressWarnings("unused")

public class DiagnoseMoritz {

	public static void main(String[] args) {
		// TODO Auto-generated method stub


//	---------------------
	
		String lauf;
		ReadParkhistory hist = new ReadParkhistory();
		//hist.readXML("parkhistory_200.xml");
//		LinkedList<HashMap<String, String>> list = hist.getAllEventByAttribute("spotType","ev");
//		ReadParkhistory a = hist.getSubHist(list);
		HashMap<String, Boolean> personList =new HashMap<String, Boolean>();

		EVControl evControl = new EVControl();
		evControl.startUp("auswertung/evs.xml", null);
		if(1==0){
			hist.readXML("alles_null_sa/parkhistory/parkhistory_200.xml");
			System.out.println("alles_null gelesen");
		}
		if(1==0){
			hist.readXML("normal_sa/parkhistory/parkhistory_200.xml");
			System.out.println("normal gelesen");
		}
		if(1==0){
			hist.readXML("erste_min_5_di/parkhistory/parkhistory_200.xml");
			System.out.println("erste_min_5 gelesen");
		}
		if(1==1){
			hist.readXML("ev_exc_di/parkhistory/parkhistory_200.xml");
			System.out.println("ev_exc gelesen");
		}
		
		if(1==0){
			hist.readXML("has_to_only_mo/parkhistory/parkhistory_200.xml");
			System.out.println("has to only gelesen");
			lauf="has_to_only_mo";
		}

		if(1==0){
			hist.readXML("has_to_only_di/parkhistory/parkhistory_200.xml");
			System.out.println("has to only gelesen");
			lauf="has_to_only_a";
		}
		
		
		System.out.println("Los jetzt Person");
		fetchPerson("35287_1", hist);
		System.out.println("Fertig Person");
		
		if(1==1){
			int i =0;
			HashMap<String, Boolean> outOfBattery = new HashMap<String, Boolean>();
			for(HashMap<String, String> event : hist.getAllEventByAttribute("eventtype", "ev_out_of_battery")){
				outOfBattery.put(event.get("person"), true);
			}
			for(HashMap<String, String> event : hist.getAllEventByAttribute("eventtype", "EV_parked")){
				if(checkIfHome(event.get("person"), personList)){
					continue;
				}
				if(outOfBattery.containsKey(event.get("person"))){
					if(event.get("spotType").equals("ev")){
						i++;
						System.out.println(event.toString());
					}
				}
				
			}
			
			System.out.println(i);
		}
		
		
		
		
		if(1==2){
			int i = 0;
			int j = 0;
			for(HashMap<String, String> event : hist.getAllEventByAttribute("eventtype", "agent_not_parked_within_default_distance")){
				if(evControl.hasEV(Id.create(event.get("person"), Person.class))) {
					System.out.println(event.toString());
					i++;
				}else {
					j++;
					}
			}
			
			System.out.println("EVS "+i);
			System.out.println("NEVs "+j);
		}
		
		
		if(1==2){
		
			for(HashMap<String, String> event : hist.getAllEventByAttribute("eventtype", "EV_parked")){
				if(Double.parseDouble(event.get("stateOfChargePercent"))<0 &&Double.parseDouble(event.get("stateOfChargePercent"))>-5){
					System.out.println(event.toString());	
				}
			}
		}

		
		if(1==2){
			for(HashMap<String, String> event : hist.getAllEventByAttribute("eventtype", "EV_parked")){
				if(Double.parseDouble(event.get("spot_score"))<-15 && event.get("spotType").equals("ev") && event.get("parkingType").equals("private")){
					System.out.println(event.toString());	
				}
			}
		}
		
		
		
//		CSVWriter writera = new CSVWriter("havetoChargeArrival");
//		writera.writeLine("time");
//		for(HashMap<String, String> event : hist.getAllEventByAttribute("eventtype", "Agent_looking_for_parking_has_to_charge")){
//			if(checkIfHome(event.get("person"), personList)){
//				continue;
//			}
//			writera.writeLine(event.get("time"));
//		}
//		
//		writera.close();
//		
		
		if(1==2){
		CSVWriter writer = new CSVWriter("evOptimierung");
		writer.writeLine("time\t sinnvoll \t sinnlosev \t sinnlosNEV \t sinnlosSumme");
		int sinnvoll = 0;
		int sinnloseEV = 0;
		int sinnloseNEV=0;
		int insuffizient = 0;
		int sinnloseSumme=0;
		for(HashMap<String, String> event : hist.events){
			if(event.get("eventtype").equals("Agent_looking_for_parking_has_to_charge")){
				continue;
			}
			
			if(checkIfHome(event.get("person"), personList)){
				continue;
			}
			if(event.get("spotType")!=null && !event.get("spotType").equals("ev")){
				continue;
			}
			if(event.get("eventtype").equals("EV_parked")){
				if(Double.parseDouble(event.get("stateOfChargePercent"))<50){
					sinnvoll++;
				}else{
					sinnloseEV++;
				}
				if(Double.parseDouble(event.get("spot_score"))<-25){
					insuffizient++;
					//System.out.println(event.toString());
				}

				
			}

			if(event.get("eventtype").equals("NEV_parked")){
				sinnloseNEV++;
				
			}
			sinnloseSumme=sinnloseEV+sinnloseNEV;
			writer.writeLine(event.get("time")+"\t"+sinnvoll+"\t"+sinnloseEV+"\t"+sinnloseNEV+"\t"+sinnloseSumme);
			
		}
		

		
		writer.close();
		
		
		System.out.println(sinnvoll);
		System.out.println(sinnloseEV);
		System.out.println(sinnloseNEV);
		System.out.println(insuffizient);
		}
//-----------------------------------------------------------------------
		
		
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
//		
//		LinkedList<HashMap<String, String>> stranger = a.getAllEventByAttribute("eventtype", "occupied");
//		for(HashMap<String, String> ereignis : stranger){
//			System.out.println(ereignis.toString());
//			
//		}
//		
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
		
		System.out.println("finished");
	}
	
	static boolean checkIfHome(String person, HashMap<String, Boolean> personList){
		
		if(personList.get(person)!=null){
			return true;
		}else{
			personList.put(person,true);
			return false;
		}
		
	}
	
	static void fetchPerson(String person, ReadParkhistory hist){
		//ReadParkhistory hist = new ReadParkhistory();
		//hist.readXML("parkhistory_200.xml");
		LinkedList<HashMap<String, String>> list = hist.getAllEventByAttribute("person",person);
		for(HashMap<String, String> event : list){
			System.out.println(event.toString());
		}
	}

}
