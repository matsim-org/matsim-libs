package playground.sergioo.RoutesAlternatives;

import java.util.List;

import playground.sergioo.AddressLocator.Location;

public class PublicTransitRoute extends Route {
	
	//Methods
	/**
	 * @param distance
	 * @param totalTime
	 */
	public PublicTransitRoute(Location origin, Location destination, double totalTime) {
		super(origin, destination, totalTime);
	}
	/**
	 * 
	 * @param points
	 * @param modes
	 * @param originStationIds
	 * @param vehicleRouteIds
	 * @throws Exception if the number of points minus one is not the number of modes
	 */
	public void addLegs(List<String> points, List<Mode> modes, List<String> originStationIds, List<String> destinationStationIds, List<String> vehicleRouteIds) throws Exception {
		if(points.size()-1==modes.size() && modes.size()==originStationIds.size() && modes.size()==destinationStationIds.size() && modes.size()==vehicleRouteIds.size())
			for(int i=0; i<modes.size(); i++)
				if(modes.get(i).equals(Mode.SUBWAY)||modes.get(i).equals(Mode.BUS))
					legs.add(new PublicTransitLeg(points.get(i), points.get(i+1), modes.get(i), originStationIds.get(i), destinationStationIds.get(i), vehicleRouteIds.get(i)));
				else
					legs.add(new Leg(points.get(i), points.get(i+1), modes.get(i)));
		else
			throw new Exception("The number of points minus one must be the number of modes");
	}
	
}
