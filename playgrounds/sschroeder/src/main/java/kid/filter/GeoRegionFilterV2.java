package kid.filter;

import java.util.Collection;
import java.util.List;

import kid.GeotoolsTransformation;
import kid.KiDUtils;
import kid.ScheduledTransportChain;
import kid.ScheduledVehicle;
import kid.ScheduledVehicleFilter;

import org.opengis.feature.simple.SimpleFeature;


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GeoRegionFilterV2 implements ScheduledVehicleFilter{
	
	private List<SimpleFeature> regions;
	
	private GeotoolsTransformation transformation;

	public GeoRegionFilterV2(List<SimpleFeature> regions) {
		this.regions = regions;
		transformation = KiDUtils.createTransformation_WGS84ToWGS84UTM32N();
	}

	public boolean judge(ScheduledVehicle vehicle) {
		if(KiDUtils.isCodableInTimeAndSpace(vehicle)){
			for(ScheduledTransportChain tChain : vehicle.getScheduledTransportChains()){
				Collection<Coordinate> coordinates = KiDUtils.getGeocodesFromActivities(tChain);
				if(judgeCoordinates(coordinates)){
					return true;
				}
			}
			return false;
		}
		else{
			return false;
		}
	}

	private boolean judgeCoordinates(Collection<Coordinate> coordinates) {
		for(Coordinate coordinate : coordinates){
			Coordinate transformedCoordinate = transformation.transform(coordinate);
			Point point = new GeometryFactory().createPoint(transformedCoordinate );
			for(SimpleFeature region : regions){
				if(point.within((Geometry)region.getDefaultGeometry())){
					return true;
				}
			}
		}
		return false;
	}
}
