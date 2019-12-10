package playgroundMeng.publicTransitServiceAnalysis.basicDataBank;

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

import playgroundMeng.publicTransitServiceAnalysis.others.TimeConvert;


public class TransitStopFacilityExtendImp extends TransitStopFacilityImpl implements TransitStopFacility {

	private Map<Integer, RouteStopInfo> routeStopInfoMap = new HashedMap();
	private Map<String, Integer> modeInfo = new HashedMap();
	private boolean findGrid = false;

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
		for (Integer key : this.routeStopInfoMap.keySet()) {
			if (this.routeStopInfoMap.get(key).equals(routeStopInfo)) {
				removeKeyList.add(key);
			}
		}
		for (Integer a : removeKeyList) {
			this.routeStopInfoMap.remove(a);
		}
	}

	public void addRouteStopInfo(RouteStopInfo routeStopInfo) {
		this.routeStopInfoMap.put(this.routeStopInfoMap.size(), routeStopInfo);
	}

	public void addRouteStopInfo(String mode, Map<Id<Vehicle>, Double> vehicleId2departure,
			TransitRouteStop transitRouteStop) {
		double departureOffset;

		if (transitRouteStop.isAwaitDepartureTime()) {
			departureOffset = transitRouteStop.getDepartureOffset();
		} else {
			departureOffset = 0;
		}
		double arrivalOffset = transitRouteStop.getArrivalOffset();

		for (Id<Vehicle> vId : vehicleId2departure.keySet()) {
			RouteStopInfo routeStopInfo = new RouteStopInfo();
			routeStopInfo.setTransportMode(mode);
			routeStopInfo.setVehicleId(vId);
			if (vehicleId2departure.get(vId) >= 24 * 3600) {
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

		for (RouteStopInfo routeStopInfo : this.routeStopInfoMap.values()) {
			if (modeInfo.containsKey(routeStopInfo.getTransportMode())) {
				int oldNum = modeInfo.get(routeStopInfo.getTransportMode());
				modeInfo.put(routeStopInfo.getTransportMode(), oldNum++);
			} else {
				modeInfo.put(routeStopInfo.getTransportMode(), 1);
			}
		}
		return modeInfo;
	}

	public boolean isFindGrid() {
		return findGrid;
	}

	public void setFindGrid(boolean findGrid) {
		this.findGrid = findGrid;
	}

	public class RouteStopInfo {
		private String transportMode;
		private Id<Vehicle> vehicleId;
		private double depatureTime;
		private double arrivalTime;

		public void setArrivalTime(double arrivalTime) {
			this.arrivalTime = arrivalTime;
		}

		public void setDepatureTime(double depatureTime) {
			this.depatureTime = depatureTime;
		}

		public void setTransportMode(String transportMode) {
			this.transportMode = transportMode;
		}

		public void setVehicleId(Id<Vehicle> vehicleId) {
			this.vehicleId = vehicleId;
		}

		public double getArrivalTime() {
			return arrivalTime;
		}

		public double getDepatureTime() {
			return depatureTime;
		}

		public String getTransportMode() {
			return transportMode;
		}

		public Id<Vehicle> getVehicleId() {
			return vehicleId;
		}

		@Override
		public String toString() {
			return "  ArrivalTime = " + this.getArrivalTime() + " DepatureTime = " + this.getDepatureTime()
					+ " VehicleId = " + this.getVehicleId() + " TransportMode = " + this.getTransportMode();
		}
	}
}
