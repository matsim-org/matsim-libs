package playground.vbmh.einzel_klassen_tests;

import java.util.HashMap;

import playground.vbmh.util.CSVWriter;
import playground.vbmh.util.ReadParkhistory;
import playground.vbmh.vmEV.EVControl;

public class GetEfficiencyCSV {

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		ReadParkhistory hist = new ReadParkhistory();
		HashMap<String, Boolean> personList =new HashMap<String, Boolean>();
		String lauf;

		EVControl evControl = new EVControl();
		evControl.startUp("auswertung/evs.xml", null);
		if(1==0){
			hist.readXML("alles_null_di/parkhistory/parkhistory_200.xml");
			System.out.println("alles_null gelesen");
			lauf="alles_null";
		}
		if(1==0){
			hist.readXML("normal_di/parkhistory/parkhistory_200.xml");
			System.out.println("normal gelesen");
			lauf="normal";
		}
		if(1==0){
			hist.readXML("erste_min_5_di/parkhistory/parkhistory_200.xml");
			System.out.println("erste_min_5 gelesen");
			lauf="erste_min_5";
		}
		if(1==0){
			hist.readXML("null_ev_exc_di/parkhistory/parkhistory_200.xml");
			System.out.println("ev_exc gelesen");
			lauf="ev_exc";
		}
		if(1==1){
			hist.readXML("has_to_only_di/parkhistory/parkhistory_200.xml");
			System.out.println("has to only gelesen");
			lauf="has_to_only_mo";
		}
		
		
			CSVWriter writer = new CSVWriter("evOptimierung_"+lauf);
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
	
	static boolean checkIfHome(String person, HashMap<String, Boolean> personList){
		
		if(personList.get(person)!=null){
			return true;
		}else{
			personList.put(person,true);
			return false;
		}
		
	}

}
