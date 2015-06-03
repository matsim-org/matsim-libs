package playground.tobiqui.master;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;


public class TqSumoRoutesWriter extends MatsimXmlWriter{

	private List<Person> persons;
	private Collection<VehicleType> vehicleTypes = new ArrayList<>();
	private Map<Id<Vehicle>, Vehicle> vehicles = new HashMap<>();
	private Map<Id<Vehicle>, VehicleInformation> vehicles2BSorted = new HashMap<>();
	private TransitSchedule transitSchedule;
	private String outputRoute;
	private String outputAdditional;
	private List<Tuple<String,String>> vType = new ArrayList<>();
	private List<Tuple<String,String>> list = new ArrayList<>();

	public TqSumoRoutesWriter(List<Person> persons, Collection<VehicleType> vehicleTypes,
			Map<Id<Vehicle>, Vehicle> vehicles, TransitSchedule transitSchedule, String outputRoute, String outputAdditional){
		this.persons = persons;
		this.vehicleTypes = vehicleTypes;
		this.vehicles = vehicles;
		this.transitSchedule = transitSchedule;
		this.outputRoute = outputRoute;
		this.outputAdditional = outputAdditional;
	}

	public void writeFiles(){

		try {
			//write routesFile
			FileOutputStream os = new FileOutputStream(outputRoute);
			super.openOutputStream(os);
			super.writeStartTag("routes", null);
			//			writeVTypes();
			//			System.out.println("writing vTypes completed");
			//			writeVehicles();
			//			System.out.println("writing Vehicles completed");
			writePersons();
			System.out.println("writing Persons completed");
			super.writeEndTag("routes");
			super.writer.close();

			//write additionalFile
			FileOutputStream add = new FileOutputStream(outputAdditional);
			this.writer = new BufferedWriter(new OutputStreamWriter(add));
			super.writeStartTag("additional", null);
			writeVTypes();
			System.out.println("writing vTypes completed");
			writeBusStops();
			System.out.println("writing busStops completed");
			writeVehicles();
			System.out.println("writing Vehicles completed");
			super.writeEndTag("additional");
			super.writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void writeVTypes(){
		vType.add(new Tuple<String, String>("id", "car"));
		vType.add(new Tuple<String, String>("vClass", "passenger")); // defaultValues -> http://sumo.dlr.de/wiki/Vehicle_Type_Parameter_Defaults		
		super.writeStartTag("vType", vType, true);
		vType.clear();

		for (VehicleType vehType : vehicleTypes){
			String type = vehType.getId().toString();
			String length = Double.toString(vehType.getLength());
			vType.add(new Tuple<String, String>("id", type));
			if (type.contains("Bus")){
				vType.add(new Tuple<String, String>("vClass", "bus")); //defaultValues -> http://sumo.dlr.de/wiki/Vehicle_Type_Parameter_Defaults
			}
			super.writeStartTag("vType", vType, true);
			vType.clear();
		}
		vType.add(new Tuple<String, String>("id", "pedestrian"));
		vType.add(new Tuple<String, String>("vClass", "pedestrian")); // defaultValues -> http://sumo.dlr.de/wiki/Vehicle_Type_Parameter_Defaults		
		super.writeStartTag("vType", vType, true);
		vType.clear();
	}

	void writeBusStops(){
		for (TransitStopFacility transStop : transitSchedule.getFacilities().values()){
			Id<TransitStopFacility> id = transStop.getId();
			String lane = transStop.getLinkId().toString();
			list.add(new Tuple<String, String>("id", id.toString()));
			list.add(new Tuple<String, String>("lane", lane + "_0"));
			//			list.add(new Tuple<String, String>("startPos", "20"));
			//			list.add(new Tuple<String, String>("endPos", "40"));
			super.writeStartTag("busStop", list, true);
			list.clear();
		}
	}

	void writePersons(){
		for (Person p : persons) {
			Id<Person> id = p.getId();
			String departure = null;
			PlanImpl pli = (PlanImpl) p.getSelectedPlan();
			Activity act = pli.getFirstActivity();
			Activity lastAct = pli.getLastActivity();
			Leg leg = pli.getNextLeg(act);
			Activity nextAct = pli.getNextActivity(leg);
			if (leg.getMode().equals("car"))
				departure = String.valueOf(leg.getDepartureTime());
			else
				departure = String.valueOf(pli.getFirstActivity().getEndTime());

			list.clear();
			list.add(new Tuple<String, String>("id", id.toString()));
			list.add(new Tuple<String, String>("depart", departure));
			super.writeStartTag("person", list);
			list.clear();

			while (act != lastAct){
				list.add(new Tuple<String, String>("from", act.getLinkId().toString()));
				list.add(new Tuple<String, String>("to", nextAct.getLinkId().toString()));
				if (leg.getMode().equals("car")){
					list.add(new Tuple<String, String>("lines", "car_" + id.toString()/* + "_" + nextAct.getType()*/));
					super.writeStartTag("ride", list, true);
				}else if (leg.getMode().contains("walk")){
					list.add(new Tuple<String, String>("duration", String.valueOf((leg.getTravelTime()))));
					super.writeStartTag("walk", list, true);
				}else if (leg.getMode().equals("pt")){ //add pt
					String routeInfo = ((GenericRouteImpl) leg.getRoute()).getRouteDescription();
					String line = routeInfo.split("===")[2];
					String route = routeInfo.split("===")[3];
					Id<TransitLine> lineId = Id.create(line, TransitLine.class);
					Id<TransitRoute> routeId = Id.create(route, TransitRoute.class);

					String lines = "";
					Collection<Departure> departures = transitSchedule.getTransitLines().get(lineId).getRoutes().get(routeId).getDepartures().values();
					for (Departure dep : departures){
						lines = lines.concat(dep.getVehicleId().toString() + " ");
					}
					if (lines.endsWith(" "))
						lines = lines.substring(0, lines.length() - 1);

					list.add(new Tuple<String, String>("lines", lines));
					list.add(new Tuple<String, String>("arrivalPos", "-10"));
					super.writeStartTag("ride", list, true); //muss noch angepasst werden
				}
				list.clear();

				if (nextAct != lastAct){
					list.add(new Tuple<String, String>("lane", nextAct.getLinkId().toString() + "_0"));
					if (nextAct.getEndTime() > 0)
						if (pli.getNextLeg(nextAct).getMode().toString().equals("car"))
							list.add(new Tuple<String, String>("until", String.valueOf(pli.getNextLeg(nextAct).getDepartureTime())));
						else
							list.add(new Tuple<String, String>("until", String.valueOf(nextAct.getEndTime())));
					else
						list.add(new Tuple<String, String>("duration", "0"));
					list.add(new Tuple<String, String>("actType", nextAct.getType()));
					list.add(new Tuple<String, String>("startPos", "0"));
					list.add(new Tuple<String, String>("endPos", "10"));
					super.writeStartTag("stop", list, true);
					list.clear();
				}
				act = nextAct;
				if (act != lastAct){
					leg = pli.getNextLeg(act);
					nextAct = pli.getNextActivity(leg);
				}
			}
			super.writeEndTag("person");
		}
	}

	void writeVehicles(){

		for (Id<Vehicle> vehicleId : getVehiclesSortedByDeparture()){
			VehicleInformation vehInfo = vehicles2BSorted.get(vehicleId);
			if (vehicles2BSorted.get(vehicleId).getType().getId().toString().equals("car")){
				list.add(new Tuple<String, String>("id", vehicleId.toString()));
				list.add(new Tuple<String, String>("depart", Double.toString(vehInfo.getDeparture())));
				list.add(new Tuple<String, String>("type", vehInfo.getType().getId().toString()));
				list.add(new Tuple<String, String>("departLane", "best"));
				//				list.add(new Tuple<String, String>("departPos", "free"));


				super.writeStartTag("vehicle", list);
				list.clear();

				list.add(new Tuple<String, String>("edges", vehInfo.getRoute()));
				super.writeStartTag("route", list, true);
				list.clear();
				int it = 0;
				for (String stop : vehInfo.getBusStopFacilities()){
					//					if (it != 0){
					list.add(new Tuple<String, String>("lane", stop.concat("_0")));
					list.add(new Tuple<String, String>("startPos", "0"));
					list.add(new Tuple<String, String>("endPos", "10"));
					super.writeStartTag("stop", list, true);
					list.clear();
					//					}
					it++;
				}

				super.writeEndTag("vehicle");
			}else if (vehicles2BSorted.get(vehicleId).getType().getId().toString().contains("Bus")){
				list.add(new Tuple<String, String>("id", vehicleId.toString()));
				list.add(new Tuple<String, String>("type", vehInfo.getType().getId().toString()));
				list.add(new Tuple<String, String>("depart", Double.toString(vehInfo.getDeparture())));
				super.writeStartTag("vehicle", list);
				list.clear();

				list.add(new Tuple<String, String>("edges", vehInfo.getRoute()));
				super.writeStartTag("route", list, true);
				list.clear();

				for (String busStopFacility : vehInfo.getBusStopFacilities()){
					list.add(new Tuple<String, String>("busStop", busStopFacility));
					list.add(new Tuple<String, String>("duration", "20"));
					super.writeStartTag("stop", list, true);
					list.clear();
				}

				super.writeEndTag("vehicle");
			}
		}
	}

	List<Id<Vehicle>> getVehiclesSortedByDeparture(){
		Id<VehicleType> vehTypeIdCar = Id.create("car", VehicleType.class);
		VehicleType vehTypeCar = new VehicleTypeImpl(vehTypeIdCar);

		int count = 0;
		for (Person p : persons) {
			Id<Person> id = p.getId();
			PlanImpl pli = (PlanImpl) p.getSelectedPlan();

			Activity act = pli.getFirstActivity();
			Activity lastAct = pli.getLastActivity();
			Leg leg = pli.getNextLeg(act);
			Activity nextAct = pli.getNextActivity(leg);

			List<String> carStops = new ArrayList<>();
			String allDayCarRoutesOfPerson = "";
			Id<Vehicle> idV = Id.create("car_" + id.toString(), Vehicle.class);
			Boolean containsCar = false;
			Boolean firstCarLeg = true;
			Double carDeparture = null;
			while (act != lastAct){
				if (leg.getMode().equals("car")){
					containsCar = true;
					if (firstCarLeg)
						carDeparture = leg.getDepartureTime(); /*act.getEndTime();*/
					firstCarLeg = false;
					//					Id<Vehicle> idV = Id.create("car_" + id.toString()/* + "_" + nextAct.getType()*/, Vehicle.class);

					String route = ((LinkNetworkRouteImpl) leg.getRoute()).getLinkIds().toString();
					if (act.getLinkId().equals(nextAct.getLinkId())){
						route = act.getLinkId().toString();
					}else{
						route = act.getLinkId().toString() + " " + route.substring(1, route.length()-1).replace(",", "") +
								" " + nextAct.getLinkId().toString();
					}
					if (allDayCarRoutesOfPerson.equals("")){
						allDayCarRoutesOfPerson = route;
						//						System.out.println(allDayCarRoutesOfPerson);
					}else{
						if (route.contains(" ")) //else route contains only out of one link
							allDayCarRoutesOfPerson = allDayCarRoutesOfPerson + route.substring(route.indexOf(" "), route.length());
						else 
							count++;
					}
					carStops.add(act.getLinkId().toString());
					//					VehicleInformation vehInfo = new VehicleInformation(idV, vehTypeCar, act.getEndTime(), allDayCarRoutesOfPerson, carStops);

					//					vehicles2BSorted.put(idV, vehInfo);
				}

				act = nextAct;
				if (act != lastAct){
					leg = pli.getNextLeg(act);
					nextAct = pli.getNextActivity(leg);
				}
			}
			if (containsCar){
				VehicleInformation vehInfo = new VehicleInformation(idV, vehTypeCar, carDeparture, allDayCarRoutesOfPerson, carStops);
				vehicles2BSorted.put(idV, vehInfo);
			}
		}
		System.out.println(count);

		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {

			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<String> busStopFacilities = new ArrayList<>();
				for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
					String busStopFacility = transitRouteStop.getStopFacility().getId().toString();
					busStopFacilities.add(busStopFacility);
				}
				for (Departure departure : transitRoute.getDepartures().values()) {

					Id<Vehicle> vehId = departure.getVehicleId();
					VehicleType vehType = vehicles.get(vehId).getType();
					NetworkRoute tRoute = transitRoute.getRoute();
					String route = tRoute.getLinkIds().toString();

					if (tRoute.getStartLinkId().equals(tRoute.getEndLinkId()))
						route = tRoute.getStartLinkId().toString();
					else
						route = tRoute.getStartLinkId().toString() + " " + route.substring(1, route.length()-1).replace(",", "") +
						" " + tRoute.getEndLinkId().toString();
					VehicleInformation vehInfo = new VehicleInformation(vehId, vehType, departure.getDepartureTime(), route, busStopFacilities);

					vehicles2BSorted.put(vehId, vehInfo);
				}
			}
		}
		List<Id<Vehicle>> vehiclesSorted = new ArrayList<Id<Vehicle>>(vehicles2BSorted.keySet()); //id's sorted by end_times of first activity of selectedPlans
		Collections.sort(vehiclesSorted, new Comparator<Id<Vehicle>>() {
			@Override
			public int compare(Id<Vehicle> o1, Id<Vehicle> o2) {
				return Double.compare(firstDeparture(o1), firstDeparture(o2));
			}

			private double firstDeparture(Id<Vehicle> o1) {
				return vehicles2BSorted.get(o1).getDeparture();
			}
		});

		return vehiclesSorted;
	}

	class VehicleInformation implements Vehicle{
		private double departure;
		private String route;
		private Id<Vehicle> id;
		private VehicleType vehicleType;
		private List<String> busStopFacilities = new ArrayList<>();

		public VehicleInformation(Id<Vehicle> id, VehicleType vehicleType, Double departure, String route, List<String> busStopFacilities){
			this.id = id;
			this.vehicleType = vehicleType;
			this.departure = departure;
			this.route = route;
			this.busStopFacilities = busStopFacilities;
		}
		@Override
		public Id<Vehicle> getId() {
			return id;
		}
		@Override
		public VehicleType getType() {
			return vehicleType;
		}
		public double getDeparture() {
			return departure;
		}
		public String getRoute() {
			return route;
		}
		public List<String> getBusStopFacilities(){
			return busStopFacilities;
		}
	}
}
