package playground.mohit;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.transitSchedule.TransitStopFacility;
 

import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;
import playground.marcel.pt.transitSchedule.TransitSchedule.*;
import playground.mohit.VisumNetwork.Stop;

public class Visum2TransitSchedule {
	
	private final VisumNetwork visum;
	private final TransitSchedule schedule;
	
	public Visum2TransitSchedule(VisumNetwork visum, TransitSchedule schedule) {
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
		// 1st step: convert stop points
		final Map<Id, TransitStopFacility> stopFacilities = new TreeMap<Id, TransitStopFacility>();
		
		for (VisumNetwork.StopPoint stopPoint : this.visum.stopPoints.values()){
			TransitStopFacility stop = new TransitStopFacility(stopPoint.id, visum.stops.get(stopPoint.stopId).coord);
			stopFacilities.put(stopPoint.id, stop);
			this.schedule.addStopFacility(stop);
		}
		// 2nd step: convert lines
		for (VisumNetwork.TransitLine line : this.visum.lines.values()){
			playground.marcel.pt.transitSchedule.TransitLine tLine = new playground.marcel.pt.transitSchedule.TransitLine(line.id);
			
			for (VisumNetwork.TimeProfile timeProfile : this.visum.timeProfiles.values()){
				// convert line routes
			   if (timeProfile.lineName.compareTo(line.id) == 0 ){
				  List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
				  //  convert route profile
				  for (VisumNetwork.TimeProfileItem tpi : this.visum.timeProfileItems.values()){
					 if (tpi.lineName.compareTo(line.id.toString()) == 0 && tpi.lineRouteName.compareTo(timeProfile.lineRouteName.toString()) == 0 && tpi.timeProfileName.compareTo(timeProfile.index.toString())==0){
				       TransitRouteStop s = new TransitRouteStop(stopFacilities.get(visum.lineRouteItems.get(line.id + timeProfile.lineRouteName.toString()+ tpi.lRIIndex.toString()).stopPointNo),this.toDouble(tpi.arr),this.toDouble(tpi.dep));
				       stops.add(s);
				     }
				  }
				  playground.marcel.pt.transitSchedule.TransitRoute tRoute = new playground.marcel.pt.transitSchedule.TransitRoute(new IdImpl(timeProfile.lineRouteName.toString()+"."+ timeProfile.index.toString()),null,stops,this.visum.transportModes.get(line.tCode));
				  //  convert departures
				  for (VisumNetwork.Departure d : this.visum.departures.values()){
					if (d.lineName.compareTo(line.id.toString())== 0 && d.lineRouteName.compareTo(timeProfile.lineRouteName.toString())== 0 && d.TRI.compareTo(timeProfile.index.toString())==0){
					  playground.marcel.pt.transitSchedule.Departure departure = new playground.marcel.pt.transitSchedule.Departure(new IdImpl(d.index), this.toDouble(d.dep));
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


