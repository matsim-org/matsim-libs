package playgroundMeng.ptAccessabilityAnalysis.stopInfoCellector;


import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.TransitStopFacilityImpl;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import playgroundMeng.ptAccessabilityAnalysis.prepare.TimeConvert;


public class TransitStopFacilityExtendImp extends TransitStopFacilityImpl implements TransitStopFacility  {
	
	Map<Integer, RouteStopInfo> routeStopInfoMap = new HashedMap();
	Map<String, Integer> modeInfo = new HashedMap();
	
	public TransitStopFacilityExtendImp(Id<TransitStopFacility> id, Coord coord, boolean isBlockingLane) {
		super(id, coord, isBlockingLane);
	}
	public void setRouteStopInfoMap(Map<Integer, RouteStopInfo> routeStopInfoMap) {
		this.routeStopInfoMap = routeStopInfoMap;
	}
	public void setRouteStopInfoMap(TransitRoute transitRoute) {
	}
	public Map<Integer, RouteStopInfo> getRouteStopInfoMap() {
		return routeStopInfoMap;
	}
	public void deleteRouteStopInfo(RouteStopInfo routeStopInfo) {
		List<Integer> removeKeyList = new LinkedList<Integer>();
		for(Integer key: this.routeStopInfoMap.keySet()) {
			if (this.routeStopInfoMap.get(key).equals(routeStopInfo)) {
				removeKeyList.add(key);
			}
		}
		for(Integer a : removeKeyList) {
			this.routeStopInfoMap.remove(a);
		}
	}
	public void addRouteStopInfo(RouteStopInfo routeStopInfo) {
		this.routeStopInfoMap.put(this.routeStopInfoMap.size(), routeStopInfo);
	}
	public void addRouteStopInfo(String mode, Map<Id<Vehicle>, Double> vehicleId2departure, TransitRouteStop transitRouteStop) {
		double departureOffset;
		
		if(transitRouteStop.isAwaitDepartureTime()) {
			departureOffset = transitRouteStop.getDepartureOffset();
		} else {
			departureOffset = 0;
		}
		double arrivalOffset = transitRouteStop.getArrivalOffset();
		
		for(Id<Vehicle> vId : vehicleId2departure.keySet()){
			RouteStopInfo routeStopInfo = new RouteStopInfo();
			routeStopInfo.setTransportMode(mode);
			routeStopInfo.setVehicleId(vId);
			if(vehicleId2departure.get(vId) >= 24*3600) {
				routeStopInfo.setDepatureTime(vehicleId2departure.get(vId) + departureOffset);
				routeStopInfo.setArrivalTime(vehicleId2departure.get(vId) + arrivalOffset);
			} else {
				routeStopInfo.setDepatureTime(TimeConvert.timeConvert(vehicleId2departure.get(vId) + departureOffset));
				routeStopInfo.setArrivalTime(TimeConvert.timeConvert(vehicleId2departure.get(vId) + arrivalOffset));
			}
			
			this.addRouteStopInfo(routeStopInfo);
		}	
	}
	public Map<String, Integer> getModeInfo() {
		
		for(RouteStopInfo routeStopInfo : this.routeStopInfoMap.values()) {
			if(modeInfo.containsKey(routeStopInfo.getTransportMode())){
				int oldNum = modeInfo.get(routeStopInfo.getTransportMode());
				modeInfo.put(routeStopInfo.getTransportMode(), oldNum++);
			} else {
				modeInfo.put(routeStopInfo.getTransportMode(), 1);
			}
		}
		return modeInfo;
	}
}
