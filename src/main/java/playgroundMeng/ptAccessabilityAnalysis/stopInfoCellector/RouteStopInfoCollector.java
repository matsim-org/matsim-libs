package playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityConfig;



@Singleton
public class RouteStopInfoCollector {
	private static final Logger logger = Logger.getLogger(RouteStopInfoCollector.class);
	
	
	TransitSchedule transitSchedule;
	PtAccessabilityConfig ptAccessabilityConfig;
	private Map<Id<TransitStopFacility>, TransitStopFacilityExtendImp> transitStopFacilities = new HashedMap();
	
	@Inject
	public RouteStopInfoCollector(TransitSchedule transitSchedule, PtAccessabilityConfig ptAccessabilityConfig) {
		this.transitSchedule = transitSchedule;
		this.ptAccessabilityConfig = ptAccessabilityConfig;
		
		for(TransitStopFacility transitStopFacility : this.transitSchedule.getFacilities().values()) {
			TransitStopFacilityExtendImp transitStopFacilityExtendImp = new TransitStopFacilityExtendImp(transitStopFacility.getId(), 
					transitStopFacility.getCoord(), transitStopFacility.getIsBlockingLane());
			this.transitStopFacilities.put(transitStopFacility.getId(), transitStopFacilityExtendImp);
		}
		this.run();
	}
	
	private void run() {
		for(TransitLine transitLine: this.transitSchedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Map<Id<Vehicle>, Double>> vehicleId2departureList = this.collectVehicleId2departure(transitRoute);
				String mode = transitRoute.getTransportMode();
				
				for(TransitRouteStop transitRouteStop: transitRoute.getStops()) {
					for(Map<Id<Vehicle>, Double> vehicleId2departure : vehicleId2departureList) {
						this.transitStopFacilities.get(transitRouteStop.getStopFacility().getId()).addRouteStopInfo(mode,vehicleId2departure,transitRouteStop);
					}	
				}
			}
		}
		if(ptAccessabilityConfig.isWriteStopInfo()) {
			writeInfo(ptAccessabilityConfig.getOutputDirectory()+"StopFacilities.xy");
		}		
	}
	
	public Map<Id<TransitStopFacility>, TransitStopFacilityExtendImp> getTransitStopFacilities() {
		return transitStopFacilities;
	}
	
	private List<Map<Id<Vehicle>, Double>> collectVehicleId2departure(TransitRoute transitRoute) {
		List<Map<Id<Vehicle>, Double>> vehicleId2departureList = new LinkedList<Map<Id<Vehicle>,Double>>();
		for (Departure departure : transitRoute.getDepartures().values()) {
			Map<Id<Vehicle>, Double> vehicleId2departure = new HashedMap();
			vehicleId2departure.put(departure.getVehicleId(), departure.getDepartureTime());
			vehicleId2departureList.add(vehicleId2departure);
		}
			return vehicleId2departureList;
	}
	
    private void writeInfo(String string) {
    	File file = new File(string);
		try {
			logger.info("beginn to write StopFacilities' info");
			FileWriter fileWriter = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			bufferedWriter.write("x	y	mode	departureTime	arrivalTime");
			
			for(TransitStopFacilityExtendImp transitStopFacilityExtendImp: this.getTransitStopFacilities().values()) {
				for(RouteStopInfo RouteStopInfo: transitStopFacilityExtendImp.getRouteStopInfoMap().values()) {
					bufferedWriter.newLine();
					bufferedWriter.write(transitStopFacilityExtendImp.getCoord().getX()+"\t"+
							transitStopFacilityExtendImp.getCoord().getY()+"\t"+
							RouteStopInfo.getTransportMode()+"\t"+
							RouteStopInfo.getDepatureTime()+"\t"+
							RouteStopInfo.getArrivalTime());
				}
			}
			bufferedWriter.close();
			logger.info("finsh writting StopFacilities' info");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
}
