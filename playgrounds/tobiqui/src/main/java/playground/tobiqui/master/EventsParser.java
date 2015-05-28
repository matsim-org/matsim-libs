package playground.tobiqui.master;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.xml.sax.Attributes;

public class EventsParser extends MatsimXmlParser{
	public EventsParser(){
		super();
	}
	VehicleData vehicleData;
	List<VehicleData> vehicles = new ArrayList<>();
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		System.out.println("name: " + name + "; atts: " + atts.getValue("id") + "; context: " + context);
		if ("vehicle".equals(name)){
			Id<Vehicle> id = Id.create(atts.getValue("id"), Vehicle.class);
			VehicleType type = new VehicleTypeImpl(Id.create(atts.getValue("type"), VehicleType.class));
			Double departure = Double.valueOf(atts.getValue("depart"));
			Double arrival = Double.POSITIVE_INFINITY;
			if (atts.getValue("arrival") != null)
				arrival = Double.valueOf(atts.getValue("arrival"));
			vehicleData = new VehicleData(id, type, departure, arrival);
		}
		if ("route".equals(name)){
			String[] edges = atts.getValue("edges").split(" ");
			String[] exitTimes = atts.getValue("exitTimes").split(" ");
			LinkedHashMap<Double, Id<Link>> edgeExitTimes = new LinkedHashMap<>();
			int length = 0;
			if (edges.length == exitTimes.length)
				length = edges.length;
			else
				if (edges.length > exitTimes.length){
					length = exitTimes.length;
					System.out.println("more edges than exitTimes");
				}else{
					length = edges.length;
					System.out.println("more exitTimes than edges");
				}
			for (int it = 0; it < length; it++)
					edgeExitTimes.put(Double.valueOf(exitTimes[it]), Id.create(edges[it], Link.class));
			vehicleData.setEdgeExitTimes(edgeExitTimes);
			vehicles.add(vehicleData);
		}
			// TODO Auto-generated method stub
		
	}

	public void endTag(String name, String content, Stack<String> context) {
		// TODO Auto-generated method stub		
	}
	static class VehicleData implements Vehicle{
		private Id<Vehicle> id;
		private VehicleType type;
		private Double departure;
		private Double arrival;
		private LinkedHashMap<Double, Id<Link>> edgeExitTimes = new LinkedHashMap<>();
		
		public VehicleData(Id<Vehicle> id, VehicleType type, Double departure,
				Double arrival) {
			this.id = id;
			this.type = type;
			this.departure = departure;
			this.arrival = arrival;
		}
		
		public void setEdgeExitTimes(LinkedHashMap<Double, Id<Link>> edgeExitTimes) {
			this.edgeExitTimes = edgeExitTimes;
		}

		@Override
		public Id<Vehicle> getId() {
			return id;
		}
		@Override
		public VehicleType getType() {
			return type;
		}
		public Double getDeparture() {
			return departure;
		}
		public Double getArrival() {
			return arrival;
		}
		public LinkedHashMap<Double, Id<Link>> getEdgeExitTimes() {
			return edgeExitTimes;
		}
	}
}
