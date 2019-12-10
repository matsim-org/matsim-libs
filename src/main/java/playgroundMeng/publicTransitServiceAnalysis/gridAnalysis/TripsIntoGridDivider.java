package playgroundMeng.publicTransitServiceAnalysis.gridAnalysis;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.GridImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.Trip;
import playgroundMeng.publicTransitServiceAnalysis.others.TimeConvert;
import playgroundMeng.publicTransitServiceAnalysis.run.PtAccessabilityConfig;

public class TripsIntoGridDivider {

	public static void divideTripsIntoGrid(GridImp gridImp, String gridKey, List<Trip> trips) {
		PtAccessabilityConfig ptAccessabilityConfig = PtAccessabilityConfig.getInstance();
		GeometryFactory gf = new GeometryFactory();
		for (Trip trip : trips) {
			if(ptAccessabilityConfig.getConsideredModes() != null) {
				for(String string : ptAccessabilityConfig.getConsideredModes()) {
					if(trip.getModes().contains(string)) {
						addTrip2Grid(gridImp, gridKey, ptAccessabilityConfig, gf, trip);
					}
				}
			} else {
				addTrip2Grid(gridImp, gridKey, ptAccessabilityConfig, gf, trip);
			}
			
		}

	}

	private static void addTrip2Grid(GridImp gridImp, String gridKey, PtAccessabilityConfig ptAccessabilityConfig, GeometryFactory gf,
			Trip trip) {
		if (!trip.isFoundDestinationZone() || !trip.isFoundOriginZone()) {

			boolean origin = gridImp.getGeometry()
					.contains(gf.createPoint(new Coordinate(trip.getActivityEndImp().getCoord().getX(),
							trip.getActivityEndImp().getCoord().getY())));
			if (origin) {
				int time = (int) (TimeConvert.timeConvert(trip.getActivityEndImp().getTime())
						/ ptAccessabilityConfig.getAnalysisTimeSlice())
						* ptAccessabilityConfig.getAnalysisTimeSlice();
				gridImp.getTime2OriginTrips().get(time).add(trip);
				trip.setFoundOriginZone(true);
				trip.setOriginZoneId(gridKey);
			}
			boolean destination = gridImp.getGeometry()
					.contains(gf.createPoint(new Coordinate(trip.getActivityStartImp().getCoord().getX(),
							trip.getActivityStartImp().getCoord().getY())));
			if (destination) {
				int time = (int) (TimeConvert.timeConvert(trip.getActivityStartImp().getTime())
						/ ptAccessabilityConfig.getAnalysisTimeSlice())
						* ptAccessabilityConfig.getAnalysisTimeSlice();
				gridImp.getTime2DestinationTrips().get(time).add(trip);
				trip.setFoundDestinationZone(true);
				trip.setDestinationZoneId(gridKey);
			}
		}
	}
}
