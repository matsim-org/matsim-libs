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
	static String osmTransitSchedule = "./transitSchedule_all.xml";
	
	
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
		Map<Id,Id> osmRouteToGtfsRouteAssignments = this.matchRoutes();
		this.writeRouteResult(osmRouteToGtfsRouteAssignments);
		Map<Id,Id> osmLineToGtfsLineAssignments = this.matchTrips(osmRouteToGtfsRouteAssignments);
		this.writeLineResult(osmLineToGtfsLineAssignments);
	}
	
	private Map<Id,Id> matchStops(){
		Map<Id,Id> result = new HashMap<Id,Id>();
		List<TransitStopFacility> osmStops = new ArrayList<TransitStopFacility>();
		osmStops.addAll(osmTs.getFacilities().values());
		for(TransitStopFacility stop: gtfsTs.getFacilities().values()){
			Id gtfsId = stop.getId();
			Id osmId = null;
			for(TransitStopFacility osmStop: osmStops){
				osmId = osmStop.getId();
				String[] idInfos = osmId.toString().split("_");
				String gtfsInfo = idInfos[idInfos.length-2].trim();
				String gtfsIdString = gtfsId.toString().trim();
				if(gtfsInfo.equalsIgnoreCase(gtfsIdString)){
					break;
				}else{
					osmId = new IdImpl("NONE FOUND");
				}
			}
			result.put(gtfsId, osmId);
		}
		return result;
	}

	private Map<Id,Id> matchRoutes(){
		Map<Id,Id> result = new HashMap<Id,Id>();
		for(TransitLine tl: gtfsTs.getTransitLines().values()){
			String gtfsRouteShortName = (tl.getId().toString().split("_"))[1].trim();
			Id fittingOsmId = new IdImpl("NONE FOUND");
			for(Id osmId: osmTs.getTransitLines().keySet()){
				String refInfo = (osmId.toString().split("_"))[2].trim();
				if(refInfo.equalsIgnoreCase(gtfsRouteShortName)){
					fittingOsmId = osmId;
					break;
				}
			}
			result.put(tl.getId(), fittingOsmId);
		}
		return result;
	}
	
	private Map<Id, Id> matchTrips(Map<Id, Id> osmRouteToGtfsRouteAssignments) {
		Map<Id,Id> result = new HashMap<Id,Id>();
		for(TransitLine tl: gtfsTs.getTransitLines().values()){
			TransitLine osmTl = osmTs.getTransitLines().get(osmRouteToGtfsRouteAssignments.get(tl.getId()));
			if(osmTl!= null){
				for(TransitRoute tr: tl.getRoutes().values()){
					Id osmId = new IdImpl("NONE FOUND");
					String headsignInfo = (tr.getId().toString().split("_"))[1].trim();
					for(TransitRoute osmTr: osmTl.getRoutes().values()){
						String directionInfo = (osmTr.getId().toString().split("_"))[1].trim();
						if(headsignInfo.equalsIgnoreCase(directionInfo)){
							osmId = osmTr.getId();
							break;
						}
					}
					if(osmId.toString().equals("NONE FOUND")){
						String[] parts = headsignInfo.split(" ");
						for(String word: parts){
							for(TransitRoute osmTr: osmTl.getRoutes().values()){
								String directionInfo = (osmTr.getId().toString().split("_"))[1].trim();
								if(directionInfo.toLowerCase().contains(word.toLowerCase())){
									osmId = osmTr.getId();
									break;
								}
							}
							if(!(osmId.toString().equals("NONE FOUND"))){
								break;
							}
						}
					}
					result.put(tr.getId(), osmId);
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
				if(osmStopToGtfsStopAssignments.get(gtfsId).toString().equalsIgnoreCase("NONE FOUND")){
					bw.write(gtfsTs.getFacilities().get(gtfsId).getName() + " (" + gtfsId + ") " + "\t\t\t --> \t\t\t" + "NONE FOUND \n");
				}else{
					bw.write(gtfsTs.getFacilities().get(gtfsId).getName() + " (" + gtfsId + ") " + " --> " + osmTs.getFacilities().get(osmStopToGtfsStopAssignments.get(gtfsId)).getName() + " (" + osmStopToGtfsStopAssignments.get(gtfsId) + ")\n");
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void writeRouteResult(Map<Id, Id> osmRouteToGtfsRouteAssignments) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./osmRouteToGtfsRouteAssignments.txt")));
			for(Id gtfsId: osmRouteToGtfsRouteAssignments.keySet()){
				if(osmRouteToGtfsRouteAssignments.get(gtfsId).toString().equalsIgnoreCase("NONE FOUND")){
					bw.write(gtfsId + "\t\t\t --> \t\t\t" + "NONE FOUND \n");
				}else{
					bw.write(gtfsId + " --> " + osmRouteToGtfsRouteAssignments.get(gtfsId) + "\n");
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
				if(osmLinetoGtfsLineAssingments.get(gtfsId).toString().equalsIgnoreCase("NONE FOUND")){
					bw.write(gtfsId + "\t\t\t --> \t\t\t" + "NONE FOUND \n");
				}else{
					bw.write(gtfsId + " --> " + osmId + "\n");
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
}
