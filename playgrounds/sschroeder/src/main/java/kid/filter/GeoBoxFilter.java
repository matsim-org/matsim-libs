package kid.filter;

import java.util.Collection;

import kid.KiDUtils;
import kid.ScheduledTransportChain;
import kid.ScheduledVehicle;
import kid.ScheduledVehicleFilter;


import com.vividsolutions.jts.geom.Coordinate;

public class GeoBoxFilter implements ScheduledVehicleFilter {

	private double minX;
	
	private double maxX;
	
	private double minY;
	
	private double maxY;
	
	public GeoBoxFilter(double minX, double maxX, double minY, double maxY) {
		super();
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
	}

	public boolean judge(ScheduledVehicle vehicle) {
		if(KiDUtils.isGeoCodable(vehicle)){
			for(ScheduledTransportChain tChain : vehicle.getScheduledTransportChains()){
				Collection<Coordinate> coordinates = KiDUtils.getGeocodesFromActivities(tChain);
				if(!judgeCoordinates(coordinates)){
					return false;
				}
			}
			return true;
		}
		else{
			return false;
		}
	}

	private boolean judgeCoordinates(Collection<Coordinate> coordinates) {
		for(Coordinate coord : coordinates){
			Coordinate transformedCoord = KiDUtils.tranformGeo_WGS84_2_WGS8432N(coord);
			if(!judgeCoordinate(transformedCoord)){
				return false;
			}
		}
		return true;
	}

	private boolean judgeCoordinate(Coordinate coord) {
		if(coord.x >= minX && coord.x <= maxX){
			if(coord.y >= minY && coord.y <= maxY){
				return true;
			}
		}
		return false;
	}

}
