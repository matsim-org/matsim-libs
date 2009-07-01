package playground.mohit;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.transitSchedule.TransitStopFacility;
 

import playground.marcel.pt.transitSchedule.TransitRouteStopImpl;
import playground.marcel.pt.transitSchedule.TransitScheduleImpl;

public class Visum2TransitSchedule {
	
	private final VisumNetwork visum;
	private final TransitScheduleImpl schedule;
	
	public Visum2TransitSchedule(VisumNetwork visum, TransitScheduleImpl schedule) {
		this.visum = visum;
		this.schedule = schedule;
	}
	public double toDouble(String s)
	{   Double d;
		d = Double.parseDouble(s.substring(0, 2))*3600 + Double.parseDouble(s.substring(3, 5))*60 + Double.parseDouble(s.substring(6, 8));
		return d;
	}
	
	public void convert() {
		// Giving all Transport Modes used inside the Visum network
		this.visum.transportModes.put("B", TransportMode.bus);
		this.visum.transportModes.put("F", TransportMode.walk);
		this.visum.transportModes.put("K", TransportMode.bus);
		this.visum.transportModes.put("L", TransportMode.other);
		this.visum.transportModes.put("P", TransportMode.car);
		this.visum.transportModes.put("R", TransportMode.bike);
		this.visum.transportModes.put("S", TransportMode.train);
		this.visum.transportModes.put("T", TransportMode.tram);
		this.visum.transportModes.put("U", TransportMode.train);
		this.visum.transportModes.put("V", TransportMode.other);
		this.visum.transportModes.put("W", TransportMode.bus);
		this.visum.transportModes.put("Z", TransportMode.train);
		
		// 1st step: convert stop points
		final Map<Id, TransitStopFacility> stopFacilities = new TreeMap<Id, TransitStopFacility>();
		
		for (VisumNetwork.StopPoint stopPoint : this.visum.stopPoints.values()){
			TransitStopFacility stop = new TransitStopFacility(stopPoint.id, visum.stops.get(visum.stopAreas.get(stopPoint.stopAreaId).StopId).coord);
			stopFacilities.put(stopPoint.id, stop);
			this.schedule.addStopFacility(stop);
		}
		// 2nd step: convert lines
		for (VisumNetwork.TransitLine line : this.visum.lines.values()){
			playground.marcel.pt.transitSchedule.TransitLineImpl tLine = new playground.marcel.pt.transitSchedule.TransitLineImpl(line.id);
			
			for (VisumNetwork.TimeProfile timeProfile : this.visum.timeProfiles.values()){
				// convert line routes
			   if (timeProfile.lineName.compareTo(line.id) == 0 ){
				  List<TransitRouteStopImpl> stops = new ArrayList<TransitRouteStopImpl>();
				  //  convert route profile
				  for (VisumNetwork.TimeProfileItem tpi : this.visum.timeProfileItems.values()){
					 if (tpi.lineName.compareTo(line.id.toString()) == 0 && tpi.lineRouteName.compareTo(timeProfile.lineRouteName.toString()) == 0 && tpi.timeProfileName.compareTo(timeProfile.index.toString())==0){
				       TransitRouteStopImpl s = new TransitRouteStopImpl(stopFacilities.get(visum.lineRouteItems.get(line.id.toString() +"/"+ timeProfile.lineRouteName.toString()+"/"+ tpi.lRIIndex.toString()+"/"+tpi.DCode).stopPointNo),this.toDouble(tpi.arr),this.toDouble(tpi.dep));
				       stops.add(s);
				     }
				  }
				  playground.marcel.pt.transitSchedule.TransitRouteImpl tRoute = new playground.marcel.pt.transitSchedule.TransitRouteImpl(new IdImpl(timeProfile.lineRouteName.toString()+"."+ timeProfile.index.toString()+timeProfile.DCode.toString()),null,stops,this.visum.transportModes.get(line.tCode));
				  //  convert departures
				  for (VisumNetwork.Departure d : this.visum.departures.values()){
					if (d.lineName.compareTo(line.id.toString())== 0 && d.lineRouteName.compareTo(timeProfile.lineRouteName.toString())== 0 && d.TRI.compareTo(timeProfile.index.toString())==0){
					  playground.marcel.pt.transitSchedule.DepartureImpl departure = new playground.marcel.pt.transitSchedule.DepartureImpl(new IdImpl(d.index), this.toDouble(d.dep));
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


