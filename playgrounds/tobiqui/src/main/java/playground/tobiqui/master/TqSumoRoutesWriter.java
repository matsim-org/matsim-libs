package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;


public class TqSumoRoutesWriter extends MatsimXmlWriter{
	
	private List<Person> persons;
	private Map<Id<VehicleType>, VehicleType> vehicleTypes = new LinkedHashMap<Id<VehicleType>, VehicleType>();
	private Map<Id<Vehicle>, Vehicle> vehicles = new LinkedHashMap<Id<Vehicle>, Vehicle>();
	private Map<Id<Vehicle>, Integer> vehicles2BSorted = new LinkedHashMap<Id<Vehicle>, Integer>();
	private Set<Id<Vehicle>> vehiclesSorted = new LinkedHashSet<Id<Vehicle>>();
	private TransitSchedule transitSchedule;
	private String outputfile;
	private List<Tuple<String,String>> vType = new ArrayList<Tuple<String,String>>();
	private List<Tuple<String,String>> list = new ArrayList<Tuple<String,String>>();
	
	public TqSumoRoutesWriter(List<Person> persons, Map<Id<VehicleType>, VehicleType> vehicleTypes,
								Map<Id<Vehicle>, Vehicle> vehicles, TransitSchedule transitSchedule, String outputfile){
		this.persons = persons;
		this.vehicleTypes = vehicleTypes;
		this.vehicles = vehicles;
		this.transitSchedule = transitSchedule;
		this.outputfile = outputfile;
	}
	
	public void writeFile(){
		
		try {
			
			FileOutputStream os = new FileOutputStream(outputfile);
			super.openOutputStream(os);
			super.writeStartTag("routes", null);
			
			writeVTypes();	
			System.out.println("writing vTypes completed");
			writeBusStops();
			System.out.println("writing busStops completed");
			writePersons();
			System.out.println("writing Persons completed");
			writeVehicles();
			System.out.println("writing Vehicles completed");
			
//			Iterator<?> ii = persons.entrySet().iterator();
//			while (ii.hasNext()) {
//				Map.Entry<Id<Person>, Person> pairs = (Entry<Id<Person>, Person>) ii.next();
//				Id<Person> id = pairs.getKey();
//				Person p = (Person) pairs.getValue();
//				PlanImpl pli = (PlanImpl) p.getSelectedPlan();
//				
//				
//				ActivityImpl firstAct = (ActivityImpl) pli.getFirstActivity();
//				ActivityImpl act = (ActivityImpl) pli.getFirstActivity();
//				ActivityImpl lastAct = (ActivityImpl) pli.getLastActivity();
//				LegImpl leg = (LegImpl) pli.getNextLeg(act);
//				ActivityImpl nextAct = (ActivityImpl) pli.getNextActivity(leg);
//				
//				while (act != lastAct){
//					if (leg.getMode().equals("car")){
//						list.add(new Tuple<String, String>("id", "car_" + id.toString() + "_" + nextAct.getType()));
//						list.add(new Tuple<String, String>("depart", Double.toString(act.getEndTime())));
//						list.add(new Tuple<String, String>("type", "car"));
//						list.add(new Tuple<String, String>("departLane", "best"));
//						super.writeStartTag("vehicle", list);
//						list.clear();
//					
//						String route = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds().toString();
//						if (act.getLinkId().equals(nextAct.getLinkId()))
//							route = act.getLinkId().toString();
//						else
//							route = act.getLinkId().toString() + " " + route.substring(1, route.length()-1).replace(",", "") + 
//									" " + nextAct.getLinkId().toString();					
//						list.add(new Tuple<String, String>("edges", route));
//						super.writeStartTag("route", list, true);
//						list.clear();
//			
////						list.add(new Tuple<String, String>("lane", act.getLinkId().toString() + "_0"));
////						list.add(new Tuple<String, String>("duration", "20"));
////						super.writeStartTag("stop", list, true);
////						list.clear();
//						
//						super.writeEndTag("vehicle");
//					}
//					act = nextAct;
//					if (act != lastAct){
//						leg = (LegImpl) pli.getNextLeg(act);
//						nextAct = (ActivityImpl) pli.getNextActivity(leg);
//					}
//				}
//			}
//			
//			Iterator<?> iii = vehicles.entrySet().iterator();
//			while (iii.hasNext()) {
//				Map.Entry<Id<Vehicle>, Vehicle> pairs = (Entry<Id<Vehicle>, Vehicle>) iii.next();
//				Id<Vehicle> id = pairs.getKey();
////				String type = pairs.getValue().getType().getId().toString();
////				list.add(new Tuple<String, String>("id", id.toString()));
////				list.add(new Tuple<String, String>("type", type));
////				super.writeStartTag("vehicle", list);
////				list.clear();
//				
//				Iterator<?> iiii = transitSchedule.getTransitLines().entrySet().iterator();
//				while (iiii.hasNext()) {
//					Map.Entry<Id<TransitLine>, TransitLine> pairs2 = (Entry<Id<TransitLine>, TransitLine>) iiii.next();
//					Id<TransitLine> id2 = pairs2.getKey();
//					
//					Iterator<?> iiiii = pairs2.getValue().getRoutes().entrySet().iterator();
//					while (iiiii.hasNext()) {
//						Map.Entry<Id<TransitRoute>, TransitRoute> pairs3 = (Entry<Id<TransitRoute>, TransitRoute>) iiiii.next();
//						Id<TransitRoute> id3 = pairs3.getKey();
//						
//						Iterator<?> iiiiii = pairs3.getValue().getDepartures().entrySet().iterator();
//						while (iiiiii.hasNext()) {
//							Map.Entry<Id<Departure>, Departure> pairs4 = (Entry<Id<Departure>, Departure>) iiiiii.next();
//							Id<Departure> id4 = pairs4.getKey();
//							
//							if (pairs4.getValue().getVehicleId().equals(id)){
//								list.add(new Tuple<String, String>("id", id.toString()));
//								list.add(new Tuple<String, String>("type", pairs.getValue().getType().getId().toString()));
//								list.add(new Tuple<String, String>("depart", Double.toString(pairs4.getValue().getDepartureTime())));
////								list.add(new Tuple<String, String>("departLane", "best"));
////								list.add(new Tuple<String, String>("route", id2.toString() + "-" + id3.toString()));
//								super.writeStartTag("vehicle", list);
//								list.clear();
//								
//								NetworkRoute tRoute = pairs3.getValue().getRoute();
//								String route = tRoute.getLinkIds().toString();
//								if (tRoute.getStartLinkId().equals(tRoute.getEndLinkId()))
//									route = tRoute.getStartLinkId().toString();
//								else
//									route = tRoute.getStartLinkId().toString() + " " + route.substring(1, route.length()-1).replace(",", "") + 
//											" " + tRoute.getEndLinkId().toString();			
//								list.add(new Tuple<String, String>("edges", route));
//								super.writeStartTag("route", list, true);
//								list.clear();
//								
//								Iterator<?> i7 = pairs3.getValue().getStops().iterator();
//								while (i7.hasNext()) {
//									TransitRouteStop pairs5 = (TransitRouteStop) i7.next();
//									String busStopFacility = pairs5.getStopFacility().getId().toString();
//									list.add(new Tuple<String, String>("busStop", busStopFacility));
//									list.add(new Tuple<String, String>("duration", "20"));
//									super.writeStartTag("stop", list, true);
//									list.clear();
//								}
//							}
//						}
//					}
//				}
//				super.writeEndTag("vehicle");
//			}
			super.writeEndTag("routes");
			super.writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void writeVTypes(){
		vType.add(new Tuple<String, String>("id", "car"));
		vType.add(new Tuple<String, String>("vClass", "passenger")); // defaultValues -> http://sumo.dlr.de/wiki/Vehicle_Type_Parameter_Defaults	
		super.writeStartTag("vType", vType, true);
		vType.clear();
		
		Iterator<?> i = vehicleTypes.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<Id<VehicleType>, VehicleType> pairs = (Entry<Id<VehicleType>, VehicleType>) i.next();
			Id<VehicleType> id = pairs.getKey();
			String type = pairs.getValue().getId().toString();
			String length = Double.toString(pairs.getValue().getLength());
			vType.add(new Tuple<String, String>("id", type));
			if (type.contains("Bus"))
				vType.add(new Tuple<String, String>("vClass", "bus")); //defaultValues -> http://sumo.dlr.de/wiki/Vehicle_Type_Parameter_Defaults
			vType.add(new Tuple<String, String>("length", length));
			super.writeStartTag("vType", vType, true);
			vType.clear();
		}
	}
	
	public void writeBusStops(){
		Iterator<?> j = transitSchedule.getFacilities().entrySet().iterator();
		while (j.hasNext()) {
			Map.Entry<Id<TransitStopFacility>, TransitStopFacility> pairsJ = (Entry<Id<TransitStopFacility>, TransitStopFacility>) j.next();
			Id<TransitStopFacility> id = pairsJ.getKey();
			String lane = pairsJ.getValue().getLinkId().toString();
			list.add(new Tuple<String, String>("id", id.toString()));
			list.add(new Tuple<String, String>("lane", lane + "_0"));
			super.writeStartTag("busStop", list, true);
			list.clear();
		}
	}
	
	public void writePersons(){
		Iterator<?> ii = persons.iterator();
		while (ii.hasNext()) {
			Map.Entry<Id<Person>, Person> pairs = (Entry<Id<Person>, Person>) ii.next();
			Id<Person> id = pairs.getKey();
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
						if (leg.getMode().equals("pt")){ //add pt
							String routeInfo = ((GenericRouteImpl) leg.getRoute()).getRouteDescription();
							String line = routeInfo.split("===")[2];
							String route = routeInfo.split("===")[3];
							Id<TransitLine> lineId = null;
							lineId = lineId.create(line, TransitLine.class);
							Id<TransitRoute> routeId = null;
							routeId = routeId.create(route, TransitRoute.class);
							
							String lines = "";
							Collection<Departure> departures = transitSchedule.getTransitLines().get(lineId).getRoutes().get(routeId).getDepartures().values(); 
							for (Departure dep : departures){
							    lines = lines.concat(dep.getVehicleId().toString() + " ");
							}
							if (lines.endsWith(" "))
								lines = lines.substring(0, lines.length() - 1);

							list.add(new Tuple<String, String>("lines", lines));
							super.writeStartTag("ride", list, true); //muss noch angepasst werden
						}
				list.clear();
				
				if (nextAct != lastAct){
//					Integer depart = this.plans.persons.get(id).get(i+1).departure;
//					System.out.println(id.toString() + ": " +depart);
					list.add(new Tuple<String, String>("lane", nextAct.getLinkId().toString() + "_0"));
					if (nextAct.getEndTime() > 0)
						list.add(new Tuple<String, String>("until", String.valueOf(nextAct.getEndTime())));
					else
						list.add(new Tuple<String, String>("duration", "20"));
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
		}
	}
	
	public void writeVehicles(){
		Iterator<?> i = getVehiclesSortedByDeparture().iterator();
//		Id<Vehicle>> pairsV = (Entry<Id<Vehicle>>) i.next();
//		Id<Vehicle> idV = (Id<Vehicle>) i.next();
		while (i.hasNext()){
			Iterator<Person> ii = persons.iterator();
			Id<Vehicle> idV = (Id<Vehicle>) i.next();
			while (ii.hasNext()) {
				Person p = ii.next();
				Id<Person> id = p.getId();
				PlanImpl pli = (PlanImpl) p.getSelectedPlan();
				Boolean next = false;
				
				ActivityImpl firstAct = (ActivityImpl) pli.getFirstActivity();
				ActivityImpl act = (ActivityImpl) pli.getFirstActivity();
				ActivityImpl lastAct = (ActivityImpl) pli.getLastActivity();
				LegImpl leg = (LegImpl) pli.getNextLeg(act);
				ActivityImpl nextAct = (ActivityImpl) pli.getNextActivity(leg);
				
				while (act != lastAct){
					String newId = "car_" + id.toString() + "_" + nextAct.getType();
					if ((leg.getMode().equals("car")) && (idV.toString().equals(newId))){
						list.add(new Tuple<String, String>("id", newId));
						list.add(new Tuple<String, String>("depart", Double.toString(act.getEndTime())));
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
			
	//					list.add(new Tuple<String, String>("lane", act.getLinkId().toString() + "_0"));
	//					list.add(new Tuple<String, String>("duration", "20"));
	//					super.writeStartTag("stop", list, true);
	//					list.clear();
						
						super.writeEndTag("vehicle");
						next = true;
						break;
					}
					act = nextAct;
					if (act != lastAct){
						leg = (LegImpl) pli.getNextLeg(act);
						nextAct = (ActivityImpl) pli.getNextActivity(leg);
					}
				}
				if (next)
					break;
			}
			
			if (idV.toString().startsWith("bus")){
				Boolean next = false;
				
				Iterator<?> iiii = transitSchedule.getTransitLines().entrySet().iterator();
				while (iiii.hasNext()) {
					Map.Entry<Id<TransitLine>, TransitLine> pairs2 = (Entry<Id<TransitLine>, TransitLine>) iiii.next();
					Id<TransitLine> id2 = pairs2.getKey();
					
					Iterator<?> iiiii = pairs2.getValue().getRoutes().entrySet().iterator();
					while (iiiii.hasNext()) {
						Map.Entry<Id<TransitRoute>, TransitRoute> pairs3 = (Entry<Id<TransitRoute>, TransitRoute>) iiiii.next();
						Id<TransitRoute> id3 = pairs3.getKey();
						
						Iterator<?> iiiiii = pairs3.getValue().getDepartures().entrySet().iterator();
						while (iiiiii.hasNext()) {
							Map.Entry<Id<Departure>, Departure> pairs4 = (Entry<Id<Departure>, Departure>) iiiiii.next();
							Id<Departure> id4 = pairs4.getKey();
							
							if (pairs4.getValue().getVehicleId().equals(idV)){
								list.add(new Tuple<String, String>("id", idV.toString()));
								list.add(new Tuple<String, String>("type", vehicles.get(idV).getType().getId().toString()));
								list.add(new Tuple<String, String>("depart", Double.toString(pairs4.getValue().getDepartureTime())));
	//							list.add(new Tuple<String, String>("departLane", "best"));
	//							list.add(new Tuple<String, String>("route", id2.toString() + "-" + id3.toString()));
								super.writeStartTag("vehicle", list);
								list.clear();
								
								NetworkRoute tRoute = pairs3.getValue().getRoute();
								String route = tRoute.getLinkIds().toString();
								if (tRoute.getStartLinkId().equals(tRoute.getEndLinkId()))
									route = tRoute.getStartLinkId().toString();
								else
									route = tRoute.getStartLinkId().toString() + " " + route.substring(1, route.length()-1).replace(",", "") + 
											" " + tRoute.getEndLinkId().toString();			
								list.add(new Tuple<String, String>("edges", route));
								super.writeStartTag("route", list, true);
								list.clear();
								
								Iterator<?> i7 = pairs3.getValue().getStops().iterator();
								while (i7.hasNext()) {
									TransitRouteStop pairs5 = (TransitRouteStop) i7.next();
									String busStopFacility = pairs5.getStopFacility().getId().toString();
									list.add(new Tuple<String, String>("busStop", busStopFacility));
									list.add(new Tuple<String, String>("duration", "20"));
									super.writeStartTag("stop", list, true);
									list.clear();
								}
								next = true;
								break;
							}
						}
						if (next)
							break;
					}
					if (next)
						break;
				}
				super.writeEndTag("vehicle");
			}
		}
	}
	
	public Set<Id<Vehicle>> getVehiclesSortedByDeparture(){
		Iterator<?> ii = persons.iterator();
		while (ii.hasNext()) {
			Map.Entry<Id<Person>, Person> pairs = (Entry<Id<Person>, Person>) ii.next();
			Id<Person> id = pairs.getKey();
			Person p = (Person) pairs.getValue();
			PlanImpl pli = (PlanImpl) p.getSelectedPlan();
			
			ActivityImpl act = (ActivityImpl) pli.getFirstActivity();
			ActivityImpl lastAct = (ActivityImpl) pli.getLastActivity();
			LegImpl leg = (LegImpl) pli.getNextLeg(act);
			ActivityImpl nextAct = (ActivityImpl) pli.getNextActivity(leg);
			
			while (act != lastAct){
				if (leg.getMode().equals("car")){
					
					Id<Vehicle> idV = null;
					idV = idV.create("car_" + id.toString() + "_" + nextAct.getType(), Vehicle.class);
					vehicles2BSorted.put(idV, (int) act.getEndTime());
				}
				
				act = nextAct;
				if (act != lastAct){
					leg = (LegImpl) pli.getNextLeg(act);
					nextAct = (ActivityImpl) pli.getNextActivity(leg);
				}
			}
		}
				
		Iterator<?> iiii = transitSchedule.getTransitLines().entrySet().iterator();
		while (iiii.hasNext()) {
			Map.Entry<Id<TransitLine>, TransitLine> pairs2 = (Entry<Id<TransitLine>, TransitLine>) iiii.next();
			Id<TransitLine> id2 = pairs2.getKey();
			
			Iterator<?> iiiii = pairs2.getValue().getRoutes().entrySet().iterator();
			while (iiiii.hasNext()) {
				Map.Entry<Id<TransitRoute>, TransitRoute> pairs3 = (Entry<Id<TransitRoute>, TransitRoute>) iiiii.next();
				Id<TransitRoute> id3 = pairs3.getKey();
				
				Iterator<?> iiiiii = pairs3.getValue().getDepartures().entrySet().iterator();
				while (iiiiii.hasNext()) {
					Map.Entry<Id<Departure>, Departure> pairs4 = (Entry<Id<Departure>, Departure>) iiiiii.next();
					Id<Departure> id4 = pairs4.getKey();
					Departure dep = pairs4.getValue();
					vehicles2BSorted.put(dep.getVehicleId(), (int) dep.getDepartureTime());
				}
			}
		}
		
		vehiclesSorted = sortHashMapByValues(vehicles2BSorted).keySet();
		return vehiclesSorted;
	}
	
	public Map<Id<Vehicle>,Integer> sortHashMapByValues(Map<Id<Vehicle>, Integer> map){
		System.out.println("start sorting vehicles by departure: " + System.currentTimeMillis());
		List<Id<Vehicle>> mapKeys = new ArrayList(map.keySet());
		List<Integer> mapValues = new ArrayList(map.values());
				
		Collections.sort(mapKeys);
		Collections.sort(mapValues);
					
		Map<Id<Vehicle>,Integer> sortedMap = new LinkedHashMap<Id<Vehicle>,Integer>();
		
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
					Id<Vehicle> id = null;
					id = id.create(key.toString(), Vehicle.class);
					sortedMap.put(id, (Integer) value);
					break;
				}
			}
		}
		System.out.println("sorting vehicles by departure completed: " + System.currentTimeMillis());
		return sortedMap;
	}
}
