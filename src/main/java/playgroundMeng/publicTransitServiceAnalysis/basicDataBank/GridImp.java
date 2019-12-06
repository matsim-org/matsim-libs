package playgroundMeng.publicTransitServiceAnalysis.basicDataBank;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playgroundMeng.publicTransitServiceAnalysis.others.CoordinateConversion;
import playgroundMeng.publicTransitServiceAnalysis.others.TimeConvert;

public class GridImp {

	private static final Logger logger = Logger.getLogger(GridImp.class);
	private final Geometry geometry;
	int timeSlice;
	CoordinateTransformation transformation = TransformationFactory
			.getCoordinateTransformation("EPSG:25832", "EPSG:4326"); 
	private double[] coordinate = new double[2];

	// Links and Stops which in this area;
	private List<LinkExtendImp> linkExtendImps = new LinkedList<LinkExtendImp>();
	private LinkedList<TransitStopFacilityExtendImp> transitStopFacilityExtendImps = new LinkedList<TransitStopFacilityExtendImp>();

	private Map<Integer, LinkedList<Trip>> time2OriginTrips = new HashedMap();
	private Map<Integer, LinkedList<Trip>> time2DestinationTrips = new HashedMap();

	// for kpi
	private Map<Integer, Double> time2Score = new HashedMap();
	private Map<Integer, Double> time2OriginKpi = new HashedMap();
	private Map<Integer, Double> time2DestinationKpi = new HashedMap();

	// for ratio

	private Map<Integer, Double> time2RatioOfOrigin = new HashedMap();
	private Map<Integer, Double> time2RatioOfDestination = new HashedMap();
	
	private Map<Integer, Double> time2RatioWWOfOrigin = new HashedMap();
	private Map<Integer, Double> time2RatioWWOfDestination = new HashedMap();
	
	
	private Map<Integer, Integer> time2NumTripsOfOrigin = new HashedMap();
	private Map<Integer, Integer> time2NumTripsOfDestination = new HashedMap();
	private Map<Integer, Integer> time2NumNoPtTripsOfOrigin = new HashedMap();
	private Map<Integer, Integer> time2NumNoPtTripsOfDestination = new HashedMap();

	public GridImp(Geometry geometry, int timeSlice) {
		this.timeSlice = timeSlice;
		this.geometry = geometry;
		this.setCoordinate();
		for (int a = 0; a < 24 * 3600; a += timeSlice) {

			time2OriginTrips.put(a, new LinkedList<Trip>());
			time2DestinationTrips.put(a, new LinkedList<Trip>());

			time2Score.put(a, 0.);
			time2OriginKpi.put(a, 0.);
			time2DestinationKpi.put(a, 0.);

			time2RatioOfOrigin.put(a, 0.);
			time2RatioOfDestination.put(a, 0.);
			
			time2RatioWWOfOrigin.put(a, 0.);
			time2RatioWWOfDestination.put(a, 0.);
			
			time2NumTripsOfOrigin.put(a, 0);
			time2NumTripsOfDestination.put(a, 0);
			time2NumNoPtTripsOfOrigin.put(a, 0);
			time2NumNoPtTripsOfDestination.put(a, 0);
			;

		}
	}

	public void setCoordinate() {
		
		double x = geometry.getCentroid().getX();
		double y = geometry.getCentroid().getY();
		
		Coord companyCoord = new Coord(x, y);
		companyCoord = transformation.transform(companyCoord);
		this.coordinate[0] = companyCoord.getY();
		this.coordinate[1] = companyCoord.getX();		
//		CoordinateConversion coordinateConversion = new CoordinateConversion();
//		double x = geometry.getCentroid().getX();
//		double y = geometry.getCentroid().getY();
//		String utm = "32 U " + String.valueOf(x) + " " + String.valueOf(y);
//		this.coordinate = coordinateConversion.utm2LatLon(utm);
//		
	}

	public void findTripsInThePolygon(List<Trip> trips) {

		GeometryFactory gf = new GeometryFactory();
		for (Trip trip : trips) {
			if (!trip.isFoundDestinationZone() || !trip.isFoundOriginZone()) {

				boolean origin = geometry
						.contains(gf.createPoint(new Coordinate(trip.getActivityEndImp().getCoord().getX(),
								trip.getActivityEndImp().getCoord().getY())));
				if (origin) {
					int time = (int) (TimeConvert.timeConvert(trip.getActivityEndImp().getTime()) / timeSlice)
							* timeSlice;
					this.time2OriginTrips.get(time).add(trip);
					trip.setFoundOriginZone(true);
				}
				boolean destination = geometry
						.contains(gf.createPoint(new Coordinate(trip.getActivityStartImp().getCoord().getX(),
								trip.getActivityStartImp().getCoord().getY())));
				if (destination) {
					int time = (int) (TimeConvert.timeConvert(trip.getActivityStartImp().getTime()) / timeSlice)
							* timeSlice;
					this.time2DestinationTrips.get(time).add(trip);
					trip.setFoundDestinationZone(true);
				}
			}
		}

	}

	public void caculate() {
		for (int a : this.time2OriginTrips.keySet()) {
			int num = 0;
			double sumRatio = 0.;
			int numNoPt = 0;

			if (!time2OriginTrips.get(a).isEmpty()) {
				for (Trip trip : this.getTime2OriginTrips().get(a)) {
					if (trip.getRatio() > 0) {
						sumRatio = sumRatio + trip.getRatio();
						num++;
					} else if (trip.getRatio() < 0) {
						numNoPt++;
					}
				}
			}

			if (num != 0) {
				this.time2RatioOfOrigin.put(a, sumRatio / num);
			}
			this.time2NumNoPtTripsOfOrigin.put(a, numNoPt);
			this.time2NumTripsOfOrigin.put(a, num);
		}

		for (int a : this.time2DestinationTrips.keySet()) {
			int num = 0;
			double sumRatio = 0.;
			int numNoPt = 0;

			if (!time2DestinationTrips.get(a).isEmpty()) {
				for (Trip trip : this.getTime2DestinationTrips().get(a)) {
					if (trip.getRatio() > 0) {
						sumRatio = sumRatio + trip.getRatio();
						num++;
					} else if (trip.getRatio() < 0) {
						numNoPt++;
					}
				}
			}

			if (num != 0) {
				this.time2RatioOfDestination.put(a, sumRatio / num);
			}
			this.time2NumNoPtTripsOfDestination.put(a, numNoPt);
			this.time2NumTripsOfDestination.put(a, num);
		}

	}
	
	public void setTime2RatioWWOfDestination(Map<Integer, Double> time2RatioWWOfDestination) {
		this.time2RatioWWOfDestination = time2RatioWWOfDestination;
	}
	
	public void setTime2RatioWWOfOrigin(Map<Integer, Double> time2RatioWWOfOrigin) {
		this.time2RatioWWOfOrigin = time2RatioWWOfOrigin;
	}
	
	public Map<Integer, Double> getTime2RatioWWOfDestination() {
		return time2RatioWWOfDestination;
	}
	
	public Map<Integer, Double> getTime2RatioWWOfOrigin() {
		return time2RatioWWOfOrigin;
	}

	public double[] getCoordinate() {
		return coordinate;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public int getTimeSlice() {
		return timeSlice;
	}

	public void setTimeSlice(int timeSlice) {
		this.timeSlice = timeSlice;
	}

	public Map<Integer, LinkedList<Trip>> getTime2OriginTrips() {
		return time2OriginTrips;
	}

	public void setTime2OriginTrips(Map<Integer, LinkedList<Trip>> time2OriginTrips) {
		this.time2OriginTrips = time2OriginTrips;
	}

	public Map<Integer, LinkedList<Trip>> getTime2DestinationTrips() {
		return time2DestinationTrips;
	}

	public void setTime2DestinationTrips(Map<Integer, LinkedList<Trip>> time2DestinationTrips) {
		this.time2DestinationTrips = time2DestinationTrips;
	}

	public Map<Integer, Double> getTime2RatioOfOrigin() {
		return time2RatioOfOrigin;
	}

	public void setTime2RatioOfOrigin(Map<Integer, Double> time2RatioOfOrigin) {
		this.time2RatioOfOrigin = time2RatioOfOrigin;
	}

	public Map<Integer, Double> getTime2RatioOfDestination() {
		return time2RatioOfDestination;
	}

	public void setTime2RatioOfDestination(Map<Integer, Double> time2RatioOfDestination) {
		this.time2RatioOfDestination = time2RatioOfDestination;
	}

	public Map<Integer, Integer> getTime2NumTripsOfOrigin() {
		return time2NumTripsOfOrigin;
	}

	public void setTime2NumTripsOfOrigin(Map<Integer, Integer> time2NumTripsOfOrigin) {
		this.time2NumTripsOfOrigin = time2NumTripsOfOrigin;
	}

	public Map<Integer, Integer> getTime2NumTripsOfDestination() {
		return time2NumTripsOfDestination;
	}

	public void setTime2NumTripsOfDestination(Map<Integer, Integer> time2NumTripsOfDestination) {
		this.time2NumTripsOfDestination = time2NumTripsOfDestination;
	}

	public Map<Integer, Integer> getTime2NumNoPtTripsOfOrigin() {
		return time2NumNoPtTripsOfOrigin;
	}

	public void setTime2NumNoPtTripsOfOrigin(Map<Integer, Integer> time2NumNoPtTripsOfOrigin) {
		this.time2NumNoPtTripsOfOrigin = time2NumNoPtTripsOfOrigin;
	}

	public Map<Integer, Integer> getTime2NumNoPtTripsOfDestination() {
		return time2NumNoPtTripsOfDestination;
	}

	public void setTime2NumNoPtTripsOfDestination(Map<Integer, Integer> time2NumNoPtTripsOfDestination) {
		this.time2NumNoPtTripsOfDestination = time2NumNoPtTripsOfDestination;
	}

	public List<LinkExtendImp> getLinkExtendImps() {
		return linkExtendImps;
	}

	public void setLinkExtendImps(List<LinkExtendImp> linkExtendImps) {
		this.linkExtendImps = linkExtendImps;
	}

	public LinkedList<TransitStopFacilityExtendImp> getTransitStopFacilityExtendImps() {
		return transitStopFacilityExtendImps;
	}

	public void setTransitStopFacilityExtendImps(
			LinkedList<TransitStopFacilityExtendImp> transitStopFacilityExtendImps) {
		this.transitStopFacilityExtendImps = transitStopFacilityExtendImps;
	}

	public Map<Integer, Double> getTime2Score() {
		return time2Score;
	}

	public void setTime2Score(Map<Integer, Double> time2Score) {
		this.time2Score = time2Score;
	}

	public Map<Integer, Double> getTime2OriginKpi() {
		return time2OriginKpi;
	}

	public void setTime2OriginKpi(Map<Integer, Double> time2OriginKpi) {
		this.time2OriginKpi = time2OriginKpi;
	}

	public Map<Integer, Double> getTime2DestinationKpi() {
		return time2DestinationKpi;
	}

	public void setTime2DestinationKpi(Map<Integer, Double> time2DestinationKpi) {
		this.time2DestinationKpi = time2DestinationKpi;
	}

	public void setCoordinate(double[] coordinate) {
		this.coordinate = coordinate;
	}
	
}
