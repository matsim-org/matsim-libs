package playground.tobiqui.master;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;


public class TqSumoRoutesWriter extends MatsimXmlWriter{
	
	private TqMatsimPlansParser plans;
	private String outputfile;
	private List<Tuple<String,String>> vType = new ArrayList<Tuple<String,String>>();
	private List<Tuple<String,String>> list = new ArrayList<Tuple<String,String>>();
	
	public TqSumoRoutesWriter(TqMatsimPlansParser plans, String outputfile){
		this.plans = plans;
		this.outputfile = outputfile;
	}
	
/*	public void writeFile(){
		vType.add(new Tuple<String, String>("id", "car"));
		vType.add(new Tuple<String, String>("accel", "0.8"));
		vType.add(new Tuple<String, String>("length", "5"));
		vType.add(new Tuple<String, String>("maxSpeed", "100"));
		
		
		try {
			
			FileOutputStream os = new FileOutputStream(outputfile);
			super.openOutputStream(os);
			super.writeStartTag("routes", null);
			super.writeStartTag("vType", vType, true);
			
			Map<Id,Integer> sortedDepartures = sortHashMapByValues(plans.departures);
			
			Iterator<?> it = sortedDepartures.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pairs = (Map.Entry) it.next();
				list.clear();
				list.add(new Tuple<String, String>("id", pairs.getKey().toString()));
				list.add(new Tuple<String, String>("type", "car"));
				list.add(new Tuple<String, String>("depart", pairs.getValue().toString()));
				list.add(new Tuple<String, String>("color", "1,0,0"));
				super.writeStartTag("vehicle", list);
				
				list.clear();
				list.add(new Tuple<String, String>("edges", this.plans.routes.get(pairs.getKey())));
				super.writeStartTag("route", list, true);
				
				super.writeEndTag("vehicle");
			}
			super.writeEndTag("routes");
			super.writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}*/
	
	public void writeFile(){
		vType.add(new Tuple<String, String>("id", "car"));
		vType.add(new Tuple<String, String>("accel", "0.8"));
		vType.add(new Tuple<String, String>("length", "5"));
		vType.add(new Tuple<String, String>("maxSpeed", "100"));
		
		
		try {
			
			FileOutputStream os = new FileOutputStream(outputfile);
			super.openOutputStream(os);
			super.writeStartTag("routes", null);
			super.writeStartTag("vType", vType, true);
			
			Map<Id,Integer> sortedDepartures = sortHashMapByValues(plans.firstDepartures);
			
			Iterator<?> it = sortedDepartures.entrySet().iterator();
			while (it.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry pairs = (Map.Entry) it.next();
				Id id = new IdImpl(pairs.getKey().toString());
				list.clear();
				list.add(new Tuple<String, String>("id", id.toString()));
				list.add(new Tuple<String, String>("depart", pairs.getValue().toString()));
				super.writeStartTag("person", list);
				list.clear();
				
				int tripAmount = this.plans.persons.get(id).size();
				for (int i = 1; i <= tripAmount; i++){
					String activity = this.plans.persons.get(id).get(i).actType;
					list.add(new Tuple<String, String>("from", this.plans.persons.get(id).get(i).fromLink));
					list.add(new Tuple<String, String>("to", this.plans.persons.get(id).get(i).toLink));
					if (this.plans.persons.get(id).get(i).mode.equals("car")){
						list.add(new Tuple<String, String>("lines", id.toString() + "_" + activity));
						super.writeStartTag("ride", list, true);
					}else if (this.plans.persons.get(id).get(i).mode.equals("walk")){
						super.writeStartTag("walk", list, true);
					}
					list.clear();
					
					if ((tripAmount > 1) && (tripAmount != i)){
						activity = this.plans.persons.get(id).get(i+1).actType;
						Integer depart = this.plans.persons.get(id).get(i+1).departure;
						System.out.println(id.toString() + ": " +depart);
						list.add(new Tuple<String, String>("lane", this.plans.persons.get(id).get(i).toLink + "_0"));
						list.add(new Tuple<String, String>("until", depart.toString()));
						list.add(new Tuple<String, String>("actType", activity));
						super.writeStartTag("stop", list, true);
						list.clear();
					}
				}
				super.writeEndTag("person");
				
				for (int i = 1; i <= tripAmount; i++){
					if (this.plans.persons.get(id).get(i).mode.equals("car")){
						String activity = this.plans.persons.get(id).get(i).actType;
						list.add(new Tuple<String, String>("id", id.toString() + "_" + activity));
						list.add(new Tuple<String, String>("depart", "triggered"));
						super.writeStartTag("vehicle", list);
						list.clear();
					
						list.add(new Tuple<String, String>("edges", this.plans.persons.get(id).get(i).route));
						super.writeStartTag("route", list, true);
						list.clear();
					
//						if ((tripAmount > 1) && (tripAmount != i)){
//							list.add(new Tuple<String, String>("lane", this.plans.persons.get(id).get(i).toLink + "_0"));
//							list.add(new Tuple<String, String>("until", this.plans.persons.get(id).get(i+1).departure.toString()));
//							super.writeStartTag("stop", list, true);
//							list.clear();
//						}
						super.writeEndTag("vehicle");
					}
				}
//					super.writeEndTag("vehicle");
			}
			super.writeEndTag("routes");
			super.writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<Id,Integer> sortHashMapByValues(Map<Id, Integer> map){
		List<Id> mapKeys = new ArrayList(map.keySet());
		List<Integer> mapValues = new ArrayList(map.values());
		Collections.sort(mapKeys);
		Collections.sort(mapValues);
		
//		System.out.println(mapValues.toString());
		
		Map<Id,Integer> sortedMap = new LinkedHashMap<Id,Integer>();
		
		Iterator valueIt = mapValues.iterator();
		while (valueIt.hasNext()){
			Object value = valueIt.next();
			Iterator keyIt = mapKeys.iterator();
			
			while(keyIt.hasNext()){
				Object key = keyIt.next();
				String comp1 = map.get(key).toString();
				String comp2 = value.toString();
				
				if (comp1.equals(comp2)){
					map.remove(key);
					mapKeys.remove(key);
					sortedMap.put(new IdImpl(key.toString()), (Integer) value);
					break;
				}
			}
//			System.out.println(sortedMap.values().toString());

		}
		return sortedMap;
	}
	
}
