package playground.florian.GTFSConverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

class OsmMatcher {

	static String gtfsTransitSchedule = "./transitSchedule_new.xml";
	static String osmTransitSchedule = "./transitSchedule_subway.xml";
	
	
	public static void main(String[] args) {
		Config c = ConfigUtils.createConfig();
		c.scenario().setUseTransit(true);
		c.scenario().setUseVehicles(true);
		ScenarioImpl gtfsScenario = (ScenarioImpl)(ScenarioUtils.createScenario(c));
		new TransitScheduleReader(gtfsScenario).readFile(gtfsTransitSchedule);
		ScenarioImpl osmScenario = (ScenarioImpl)(ScenarioUtils.createScenario(c));
		new TransitScheduleReader(osmScenario).readFile(osmTransitSchedule);
		OsmMatcher om = new OsmMatcher(gtfsScenario.getTransitSchedule(), osmScenario.getTransitSchedule());
		om.matchSchedules();
	}
	
	//######################################################################################################################################################
	
	private TransitSchedule gtfsTs;
	private TransitSchedule osmTs;
	
	private Map<Id,Id> gtfsTransitLineToRouteAssignments = new HashMap<Id,Id>();
	private Map<Id,Id> osmTransitLineToRouteAssignments = new HashMap<Id,Id>();
	
	CoordinateTransformation osmTrafo = new IdentityTransformation();
	CoordinateTransformation gtfsTrafo = new IdentityTransformation();
	
	public OsmMatcher(TransitSchedule gtfsTs, TransitSchedule osmTs){
		this.gtfsTs = gtfsTs;
		this.osmTs = osmTs;
	}
	
	
	public void matchSchedules(){
		Map<Id, Id> osmStopToGtfsStopAssignments = this.matchStops();
		this.writeStationResult(osmStopToGtfsStopAssignments);
		Map<Id,Id> osmLineToGtfsLineAssignments = this.matchTrips(osmStopToGtfsStopAssignments);
		this.writeLineResult(osmLineToGtfsLineAssignments);
	}

	private Map<Id, Id> matchStops() {
		Map<Id,Id> result = new HashMap<Id,Id>();
		List<TransitStopFacility> osmStops = new ArrayList<TransitStopFacility>();
		osmStops.addAll(osmTs.getFacilities().values());
		int count = 0;
		for(TransitStopFacility stop: gtfsTs.getFacilities().values()){
			double dist = Double.MAX_VALUE;
			Coord gtfsCoord = stop.getCoord();
			Id gtfsId = stop.getId();
			Id osmId = new IdImpl("NONE FOUND");
			for(TransitStopFacility osmStop: osmStops){
				Coord osmCoord = osmTrafo.transform(osmStop.getCoord());
				double d = CoordUtils.calcDistance(gtfsCoord, osmCoord);
				if((d < dist) && (d < 0.002)){
					osmId = osmStop.getId();
					dist = d;
				}
			}
			result.put(gtfsId, osmId);
			if(++count % 1000 == 0){
				System.out.println(count + " stations processed");
			}
		}
		return result;
	}
	
	private Map<Id,Id> matchTrips(Map<Id,Id> osmStopToGtfsStopAssingments){
		Map<Id,Id> result = new HashMap<Id,Id>();
		for(TransitLine tl: gtfsTs.getTransitLines().values()){
			for(TransitRoute tr: tl.getRoutes().values()){
				List<TransitRouteStop> routeStops = tr.getStops();
				TransitRouteStop firstStop = routeStops.get(0);
				result.put(tr.getId(),new IdImpl("NONE FOUND"));
				this.gtfsTransitLineToRouteAssignments.put(tr.getId(), tl.getId());
				for(TransitLine otl: osmTs.getTransitLines().values()){
					for(TransitRoute otr: otl.getRoutes().values()){
						this.osmTransitLineToRouteAssignments.put(otr.getId(), otl.getId());
						for(TransitRouteStop otrs: otr.getStops()){
							if(otrs.getStopFacility().getId().equals(osmStopToGtfsStopAssingments.get(firstStop.getStopFacility().getId()))){
								List<TransitRouteStop> osmStops = new ArrayList<TransitRouteStop>();
								osmStops.addAll(otr.getStops());
								if(this.isInLine(routeStops,osmStops, osmStopToGtfsStopAssingments, otrs)){
									result.put(tr.getId(), otr.getId());
								}else{
									Collections.reverse(osmStops);
									if(this.isInLine(routeStops, osmStops, osmStopToGtfsStopAssingments, otrs)){
										result.put(tr.getId(), otr.getId());
									}
								}									
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	private boolean isInLine(List<TransitRouteStop> routeStops, List<TransitRouteStop> stops, Map<Id, Id> osmStopToGtfsStopAssingments, TransitRouteStop firstRouteStop) {
		int startIndex = stops.indexOf(firstRouteStop);
		boolean result = true;
		int i=0;
		for(TransitRouteStop trs: routeStops){
			if(startIndex+i >= stops.size()){
				result = false;
				break;
			}
			if(!(stops.get(startIndex+i).getStopFacility().getId().equals(osmStopToGtfsStopAssingments.get(trs.getStopFacility().getId())))){
				result = false;
				break;
			}
			i++;
		}
		return result;
	}

	private void writeStationResult(Map<Id,Id> osmStopToGtfsStopAssignments){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./osmStopToGtfsStopAssignments.txt")));
			for(Id gtfsId: osmStopToGtfsStopAssignments.keySet()){
				if(osmStopToGtfsStopAssignments.get(gtfsId).toString().equals("NONE FOUND")){
					bw.write(gtfsTs.getFacilities().get(gtfsId).getName() + " (" + gtfsTs.getFacilities().get(gtfsId).getCoord() + ") " + "\t\t\t --> \t\t\t" + "NONE FOUND \n");
				}else{
					bw.write(gtfsTs.getFacilities().get(gtfsId).getName() + " (" + gtfsTs.getFacilities().get(gtfsId).getCoord() + ") " + " --> " + osmStopToGtfsStopAssignments.get(gtfsId) + " (" + osmTs.getFacilities().get(osmStopToGtfsStopAssignments.get(gtfsId)).getCoord() + ") --> distance = " + CoordUtils.calcDistance(gtfsTs.getFacilities().get(gtfsId).getCoord(), osmTrafo.transform(osmTs.getFacilities().get(osmStopToGtfsStopAssignments.get(gtfsId)).getCoord())) + "\n");
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void writeLineResult(Map<Id,Id>osmLinetoGtfsLineAssingments){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./osmLineToGtfsLineAssignments.txt")));
			for(Id gtfsId: osmLinetoGtfsLineAssingments.keySet()){
				Id osmId = osmLinetoGtfsLineAssingments.get(gtfsId);
				if(osmLinetoGtfsLineAssingments.get(gtfsId).toString().equals("NONE FOUND")){
					bw.write(gtfsTs.getTransitLines().get(gtfsTransitLineToRouteAssignments.get(gtfsId)).getRoutes().get(gtfsId).getId() + "\t\t\t --> \t\t\t" + "NONE FOUND \n");
				}else{
					bw.write(gtfsTs.getTransitLines().get(gtfsTransitLineToRouteAssignments.get(gtfsId)).getRoutes().get(gtfsId).getId() + "\t\t\t --> \t\t\t" + osmTs.getTransitLines().get(osmTransitLineToRouteAssignments.get(osmId)).getRoutes().get(osmId).getId() + "\n");
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
}
