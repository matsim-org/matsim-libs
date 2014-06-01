package playground.wrashid.bsc.vbmh.einzel_klassen_tests;

import java.util.HashMap;
import java.util.LinkedList;

import playground.wrashid.bsc.vbmh.util.CSVWriter;
import playground.wrashid.bsc.vbmh.util.ReadParkhistory;

public class ScoresHasToCharge {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ReadParkhistory alles_null =new ReadParkhistory();
		alles_null.readXML("alles_null_sa/parkhistory/parkhistory_200.xml");
		System.out.println("alles_null gelesen");
		ReadParkhistory normal =new ReadParkhistory();
		normal.readXML("normal_sa/parkhistory/parkhistory_200.xml");
		System.out.println("normal gelesen");
		ReadParkhistory erste_min_5 =new ReadParkhistory();
		erste_min_5.readXML("erste_min_5_sa/parkhistory/parkhistory_200.xml");
		System.out.println("erste_min_5 gelesen");
		ReadParkhistory ev_exc =new ReadParkhistory();
		ev_exc.readXML("null_ev_exc_sa/parkhistory/parkhistory_200.xml");
		System.out.println("ev_exc gelesen");
		
		LinkedList<Double> alles_null_scores = getHasToScore(alles_null);
		LinkedList<Double> normal_scores = getHasToScore(normal);
		LinkedList<Double> erste_min_5_scores = getHasToScore(erste_min_5);
		LinkedList<Double> ev_exc_scores = getHasToScore(ev_exc);
		
		CSVWriter writer = new CSVWriter("ScoresHasToCharge.csv");
		writer.writeLine("alles_null, normal, erste_min_5, ev_exc");
		int size=0;
		if(alles_null_scores.size()>size){
			size=alles_null_scores.size();
			if(normal_scores.size()>size){
				size=normal_scores.size();
				if(erste_min_5_scores.size()>size){
					size=erste_min_5_scores.size();
					if(ev_exc_scores.size()>size){
						size=ev_exc_scores.size();
					}
				}
			}
		}
		for(int i=0; i<size; i++){
			
			String a;
			if (!(alles_null_scores.isEmpty())) {
				a = alles_null_scores.getFirst().toString();
				alles_null_scores.removeFirst();
			}else a="NaN";
			String b;
			if (!(normal_scores.isEmpty())) {
				b = normal_scores.getFirst().toString();
				normal_scores.removeFirst();
			}else b="NaN";
			String c;
			if (!(erste_min_5_scores.isEmpty())) {
				c = erste_min_5_scores.getFirst().toString();
				erste_min_5_scores.removeFirst();
			}else c = "NaN";
			String d;
			if (!(ev_exc_scores.isEmpty())) {
				d = ev_exc_scores.getFirst().toString();
				ev_exc_scores.removeFirst();
			}else d = "NaN";
			writer.writeLine(a+", "+b+", "+c+", "+d);	
		}
		
		writer.close();
		System.out.println("Fertig");
	}
	
	
	
	
	
	
	
	
	
	
	public static LinkedList<Double> getHasToScore(ReadParkhistory scenario){
		LinkedList<Double> scenario_scores=new LinkedList<Double>();
		
		LinkedList<HashMap<String, String>> scenario_public = scenario.getAllEventByAttribute("parkingType", "public");
		for(HashMap<String, String> values:scenario_public){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					scenario_scores.add(Double.parseDouble(spot_score));
				}
			}
		}	
		LinkedList<HashMap<String, String>> scenario_private = scenario.getAllEventByAttribute("parkingType", "private");
		HashMap<String, Double> scenario_private_scores =new HashMap<String, Double>();
		for(HashMap<String, String> values:scenario_private){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					String person = values.get("person");
					if(!(scenario_private_scores.containsKey(person))){
						scenario_private_scores.put(person, Double.parseDouble(spot_score));
					}
				}
			}
		}
		for(Double value:scenario_private_scores.values()){
			scenario_scores.add(value);
		}
		return scenario_scores;
	}

}
