package playground.vbmh.einzel_klassen_tests;

import java.util.HashMap;
import java.util.LinkedList;

import playground.vbmh.util.CSVWriter;
import playground.vbmh.util.ReadParkhistory;

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
		
		
		//------------alles_null
		LinkedList<Double> alles_null_scores=new LinkedList<Double>();
		
		LinkedList<HashMap<String, String>> alles_null_public = alles_null.getAllEventByAttribute("parkingType", "public");
		for(HashMap<String, String> values:alles_null_public){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					alles_null_scores.add(Double.parseDouble(spot_score));
				}
			}
		}	
		LinkedList<HashMap<String, String>> alles_null_private = alles_null.getAllEventByAttribute("parkingType", "private");
		HashMap<String, Double> alles_null_private_scores =new HashMap<String, Double>();
		for(HashMap<String, String> values:alles_null_private){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					String person = values.get("person");
					if(!(alles_null_private_scores.containsKey(person))){
						alles_null_private_scores.put(person, Double.parseDouble(spot_score));
					}
				}
			}
		}
		for(Double value:alles_null_private_scores.values()){
			alles_null_scores.add(value);
		}
		
		
		//----------------normal
		LinkedList<Double> normal_scores=new LinkedList<Double>();
		
		LinkedList<HashMap<String, String>> normal_public = normal.getAllEventByAttribute("parkingType", "public");
		for(HashMap<String, String> values:normal_public){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					normal_scores.add(Double.parseDouble(spot_score));
				}
			}
		}	
		LinkedList<HashMap<String, String>> normal_private = normal.getAllEventByAttribute("parkingType", "private");
		HashMap<String, Double> normal_private_scores =new HashMap<String, Double>();
		for(HashMap<String, String> values:normal_private){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					String person = values.get("person");
					if(!(normal_private_scores.containsKey(person))){
						normal_private_scores.put(person, Double.parseDouble(spot_score));
					}
				}
			}
		}
		for(Double value:normal_private_scores.values()){
			normal_scores.add(value);
		}
		
		
		//------------erste_min_5
		LinkedList<Double> erste_min_5_scores=new LinkedList<Double>();
		
		LinkedList<HashMap<String, String>> erste_min_5_public = erste_min_5.getAllEventByAttribute("parkingType", "public");
		for(HashMap<String, String> values:erste_min_5_public){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					erste_min_5_scores.add(Double.parseDouble(spot_score));
				}
			}
		}	
		LinkedList<HashMap<String, String>> erste_min_5_private = erste_min_5.getAllEventByAttribute("parkingType", "private");
		HashMap<String, Double> erste_min_5_private_scores =new HashMap<String, Double>();
		for(HashMap<String, String> values:erste_min_5_private){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					String person = values.get("person");
					if(!(erste_min_5_private_scores.containsKey(person))){
						erste_min_5_private_scores.put(person, Double.parseDouble(spot_score));
					}
				}
			}
		}
		for(Double value:erste_min_5_private_scores.values()){
			erste_min_5_scores.add(value);
		}
		
		
		//-------ev_exc
		LinkedList<Double> ev_exc_scores=new LinkedList<Double>();
		
		LinkedList<HashMap<String, String>> ev_exc_public = ev_exc.getAllEventByAttribute("parkingType", "public");
		for(HashMap<String, String> values:ev_exc_public){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					ev_exc_scores.add(Double.parseDouble(spot_score));
				}
			}
		}	
		LinkedList<HashMap<String, String>> ev_exc_private = ev_exc.getAllEventByAttribute("parkingType", "private");
		HashMap<String, Double> ev_exc_private_scores =new HashMap<String, Double>();
		for(HashMap<String, String> values:ev_exc_private){
			if(values.get("stateOfChargePercent")!=null&&Float.parseFloat(values.get("stateOfChargePercent"))<50.0){
				String spot_score = values.get("spot_score");
				if(spot_score!=null){
					String person = values.get("person");
					if(!(ev_exc_private_scores.containsKey(person))){
						ev_exc_private_scores.put(person, Double.parseDouble(spot_score));
					}
				}
			}
		}
		for(Double value:ev_exc_private_scores.values()){
			ev_exc_scores.add(value);
		}
		
		
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

}
