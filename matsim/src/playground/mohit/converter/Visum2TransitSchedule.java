package playground.mohit.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleBuilder;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.Kilometer2MeterTransformation;


public class Visum2TransitSchedule {
	
	private final VisumNetwork visum;
	private final TransitSchedule schedule;
	private final CoordinateTransformation coordinateTransformation = new Kilometer2MeterTransformation();
	
	public Visum2TransitSchedule(final VisumNetwork visum, final TransitSchedule schedule) {
		this.visum = visum;
		this.schedule = schedule;
	}

	
	public void convert() {
		// Giving all Transport Modes used inside the Visum network

		// the ones for Berlin
//		this.visum.transportModes.put("B", TransportMode.bus);
//		this.visum.transportModes.put("F", TransportMode.walk);
//		this.visum.transportModes.put("K", TransportMode.bus);
//		this.visum.transportModes.put("L", TransportMode.other);
//		this.visum.transportModes.put("P", TransportMode.car);
//		this.visum.transportModes.put("R", TransportMode.bike);
//		this.visum.transportModes.put("S", TransportMode.train);
//		this.visum.transportModes.put("T", TransportMode.tram);
//		this.visum.transportModes.put("U", TransportMode.train);
//		this.visum.transportModes.put("V", TransportMode.other);
//		this.visum.transportModes.put("W", TransportMode.bus);
//		this.visum.transportModes.put("Z", TransportMode.train);
		
		// the ones for Zurich
		this.visum.transportModes.put("B", TransportMode.bus); // BUS
		this.visum.transportModes.put("F", TransportMode.walk); // BUSS
		this.visum.transportModes.put("I", TransportMode.car); // IV-PW
		this.visum.transportModes.put("R", TransportMode.train); // REGIONALVERKEHR
		this.visum.transportModes.put("S", TransportMode.other); // SCHIFF
		this.visum.transportModes.put("T", TransportMode.tram); // TRAM
		this.visum.transportModes.put("Y", TransportMode.train); // BERGBAHN
		this.visum.transportModes.put("Z", TransportMode.train); // FERNVERKEHR
	
		TransitScheduleBuilder builder = this.schedule.getBuilder();
		
		// 1st step: convert stop points
		final Map<Id, TransitStopFacility> stopFacilities = new TreeMap<Id, TransitStopFacility>();
		
		for (VisumNetwork.StopPoint stopPoint : this.visum.stopPoints.values()){
			Coord coord = this.coordinateTransformation.transform(visum.stops.get(visum.stopAreas.get(stopPoint.stopAreaId).StopId).coord);
			TransitStopFacility stop = builder.createTransitStopFacility(stopPoint.id, coord);
			stopFacilities.put(stopPoint.id, stop);
			this.schedule.addStopFacility(stop);
		}
		// 2nd step: convert lines
		for (VisumNetwork.TransitLine line : this.visum.lines.values()){
			TransitLine tLine = builder.createTransitLine(line.id);
			
			for (VisumNetwork.TimeProfile timeProfile : this.visum.timeProfiles.values()){
				// convert line routes
			   if (timeProfile.lineName.compareTo(line.id) == 0 ){
				  List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
				  //  convert route profile
				  for (VisumNetwork.TimeProfileItem tpi : this.visum.timeProfileItems.values()){
					 if (tpi.lineName.compareTo(line.id.toString()) == 0 && tpi.lineRouteName.compareTo(timeProfile.lineRouteName.toString()) == 0 && tpi.timeProfileName.compareTo(timeProfile.index.toString())==0 && tpi.DCode.compareTo(timeProfile.DCode.toString())==0){
				       TransitRouteStop s = builder.createTransitRouteStop(stopFacilities.get(visum.lineRouteItems.get(line.id.toString() +"/"+ timeProfile.lineRouteName.toString()+"/"+ tpi.lRIIndex.toString()+"/"+tpi.DCode).stopPointNo),Time.parseTime(tpi.arr),Time.parseTime(tpi.dep));
				       stops.add(s);
				     }
				  }
				  TransportMode mode = this.visum.transportModes.get(line.tCode);
				  if (mode == null) {
				  	System.err.println("Could not find TransportMode for " + line.tCode + ", more info: " + line.id);
				  }
				  TransitRoute tRoute = builder.createTransitRoute(new IdImpl(timeProfile.lineName.toString()+"."+timeProfile.lineRouteName.toString()+"."+ timeProfile.index.toString()+"."+timeProfile.DCode.toString()),null,stops,mode);
				  //  convert departures
				  for (VisumNetwork.Departure d : this.visum.departures.values()){
					if (d.lineName.compareTo(line.id.toString())== 0 && d.lineRouteName.compareTo(timeProfile.lineRouteName.toString())== 0 && d.TRI.compareTo(timeProfile.index.toString())==0){
					  Departure departure = builder.createDeparture(new IdImpl(d.index), Time.parseTime(d.dep));
					  tRoute.addDeparture(departure);
					}
			      }
				  tLine.addRoute(tRoute);

		      }	
			}
			this.schedule.addTransitLine(tLine);
			
		}
	}
}


