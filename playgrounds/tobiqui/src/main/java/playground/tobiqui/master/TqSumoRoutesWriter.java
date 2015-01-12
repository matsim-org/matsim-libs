package playground.tobiqui.master;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;


public class TqSumoRoutesWriter extends MatsimXmlWriter{
	
	private Map<Id<Person>, Person> persons = new LinkedHashMap<Id<Person>, Person>(); 
	private String outputfile;
	private List<Tuple<String,String>> vType = new ArrayList<Tuple<String,String>>();
	private List<Tuple<String,String>> list = new ArrayList<Tuple<String,String>>();
	
	public TqSumoRoutesWriter(Map<Id<Person>, Person> persons, String outputfile){
		this.persons = persons;
		this.outputfile = outputfile;
	}
	
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
			
			Iterator<?> it = persons.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<Id<Person>, Person> pairs = (Entry<Id<Person>, Person>) it.next();
				Id id = pairs.getKey();
				Person p = (Person) pairs.getValue();
				PlanImpl pli = (PlanImpl) p.getSelectedPlan();
				String departure = String.valueOf(pli.getFirstActivity().getEndTime());
				
				list.clear();
				list.add(new Tuple<String, String>("id", id.toString()));
				list.add(new Tuple<String, String>("depart", departure));
				super.writeStartTag("person", list);
				list.clear();
				
				ActivityImpl firstAct = (ActivityImpl) pli.getFirstActivity();
				ActivityImpl act = (ActivityImpl) pli.getFirstActivity();
				ActivityImpl lastAct = (ActivityImpl) pli.getLastActivity();
				LegImpl leg = (LegImpl) pli.getNextLeg(act);
				ActivityImpl nextAct = (ActivityImpl) pli.getNextActivity(leg);
				
				while (act != lastAct){
					list.add(new Tuple<String, String>("from", act.getLinkId().toString()));
					list.add(new Tuple<String, String>("to", nextAct.getLinkId().toString()));
					if (leg.getMode().equals("car")){
						list.add(new Tuple<String, String>("lines", "car_" + id.toString() + "_" + nextAct.getType()));
						super.writeStartTag("ride", list, true);
					}else 
						if (leg.getMode().contains("walk")){
							super.writeStartTag("walk", list, true);
						}else 
							if (leg.getMode().equals("pt")){
								super.writeStartTag("walk", list, true); //muss noch angepasst werden
							}
					list.clear();
					
					if (nextAct != lastAct){
//						Integer depart = this.plans.persons.get(id).get(i+1).departure;
//						System.out.println(id.toString() + ": " +depart);
						list.add(new Tuple<String, String>("lane", nextAct.getLinkId().toString() + "_0"));
						if (nextAct.getEndTime() > 0)
							list.add(new Tuple<String, String>("until", String.valueOf(nextAct.getEndTime())));
						else
							list.add(new Tuple<String, String>("duration", String.valueOf(nextAct.getMaximumDuration())));
						list.add(new Tuple<String, String>("actType", nextAct.getType()));
						super.writeStartTag("stop", list, true);
						list.clear();
					}
					act = nextAct;
					if (act != lastAct){
						leg = (LegImpl) pli.getNextLeg(act);
						nextAct = (ActivityImpl) pli.getNextActivity(leg);
					}
				}
				super.writeEndTag("person");
				
				
				firstAct = (ActivityImpl) pli.getFirstActivity();
				act = (ActivityImpl) pli.getFirstActivity();
				lastAct = (ActivityImpl) pli.getLastActivity();
				leg = (LegImpl) pli.getNextLeg(act);
				nextAct = (ActivityImpl) pli.getNextActivity(leg);
				
				while (act != lastAct){
					if (leg.getMode().equals("car")){
						list.add(new Tuple<String, String>("id", "car_" + id.toString() + "_" + nextAct.getType()));
						list.add(new Tuple<String, String>("depart", "triggered"));
						list.add(new Tuple<String, String>("type", "car"));
						list.add(new Tuple<String, String>("departLane", "best"));
						super.writeStartTag("vehicle", list);
						list.clear();
					
						String route = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds().toString();
						if (act.getLinkId().equals(nextAct.getLinkId()))
							route = act.getLinkId().toString();
						else
							route = act.getLinkId().toString() + " " + route.substring(1, route.length()-1).replace(",", "") + 
									" " + nextAct.getLinkId().toString();					
						list.add(new Tuple<String, String>("edges", route));
						super.writeStartTag("route", list, true);
						list.clear();
			
//						list.add(new Tuple<String, String>("lane", act.getLinkId().toString() + "_0"));
//						list.add(new Tuple<String, String>("duration", "20"));
//						super.writeStartTag("stop", list, true);
//						list.clear();
						
						super.writeEndTag("vehicle");
					}
					act = nextAct;
					if (act != lastAct){
						leg = (LegImpl) pli.getNextLeg(act);
						nextAct = (ActivityImpl) pli.getNextActivity(leg);
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
}
