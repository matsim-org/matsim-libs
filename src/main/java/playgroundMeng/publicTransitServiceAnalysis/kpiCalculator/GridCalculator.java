package playgroundMeng.publicTransitServiceAnalysis.kpiCalculator;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.GridImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.LinkExtendImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.TransitStopFacilityExtendImp;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.TransitStopFacilityExtendImp.RouteStopInfo;
import playgroundMeng.publicTransitServiceAnalysis.basicDataBank.Trip;
import playgroundMeng.publicTransitServiceAnalysis.others.PtAccessabilityConfig;

public class GridCalculator {

	public static void calculateTime2Score(GridImp gridImp) {
		addStopsInfo2Link(gridImp);
		PtAccessabilityConfig ptAccessabilityConfig = PtAccessabilityConfig.getInstance();
		for (int x : gridImp.getTime2Score().keySet()) {
			for (LinkExtendImp linkExtendImp : gridImp.getLinkExtendImps()) {
				linkExtendImp.setTime2Score(x, 0.);

				for (Map<Id<TransitStopFacility>, RouteStopInfo> key : linkExtendImp.getPtInfos().keySet()) {
					for (RouteStopInfo routeStopInfo : key.values()) {
						if (routeStopInfo.getDepatureTime() >= x && routeStopInfo
								.getDepatureTime() < (x + ptAccessabilityConfig.getAnalysisTimeSlice())) {
							try {
								linkExtendImp.setTime2Score(x,
										(linkExtendImp.getTime2Score().get(x) + scorecalculateFromMode(routeStopInfo)));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}

				double a = gridImp.getTime2Score().get(x) + linkExtendImp.getTime2Score().get(x);
				gridImp.getTime2Score().put(x, a);
			}
		}
	}

	public static void calculateTime2Ratio(GridImp gridImp) throws Exception {
		for (int a : gridImp.getTime2OriginTrips().keySet()) {
			int num = 0;
			double sumRatio = 0.;
			double sumRatioWW = 0.;
			int numNoPt = 0;

			if (!gridImp.getTime2OriginTrips().get(a).isEmpty()) {
				for (Trip trip : gridImp.getTime2OriginTrips().get(a)) {
					tripRatioCalculate(trip);
					if (trip.getRatio() > 0) {
						sumRatio = sumRatio + trip.getRatio();
						sumRatioWW = sumRatioWW + trip.getRatioWithOutWaitingTime();
						num++;
					} else if (trip.getRatio() < 0) {
						numNoPt++;
					}
				}
			}

			if (num != 0) {
				gridImp.getTime2RatioOfOrigin().put(a, sumRatio / num);
				gridImp.getTime2RatioWWOfOrigin().put(a, sumRatioWW / num);
			}
			gridImp.getTime2NumNoPtTripsOfOrigin().put(a, numNoPt);
			gridImp.getTime2NumTripsOfOrigin().put(a, num + numNoPt);
		}

		for (int a : gridImp.getTime2DestinationTrips().keySet()) {
			int num = 0;
			double sumRatio = 0.;
			double sumRatioWW = 0.;
			int numNoPt = 0;

			if (!gridImp.getTime2DestinationTrips().get(a).isEmpty()) {
				for (Trip trip : gridImp.getTime2DestinationTrips().get(a)) {
					tripRatioCalculate(trip);
					if (trip.getRatio() > 0) {
						sumRatio = sumRatio + trip.getRatio();
						sumRatioWW = sumRatioWW + trip.getRatioWithOutWaitingTime();
						num++;
					} else if (trip.getRatio() < 0) {
						numNoPt++;
					}
				}
			}

			if (num != 0) {
				gridImp.getTime2RatioOfDestination().put(a, sumRatio / num);
				gridImp.getTime2RatioWWOfDestination().put(a, sumRatioWW / num);
			}
			gridImp.getTime2NumNoPtTripsOfDestination().put(a, numNoPt);
			gridImp.getTime2NumTripsOfDestination().put(a, num + numNoPt);
		}

	}

	public static void calculateTime2Kpi(GridImp gridImp) {
		for (int a : gridImp.getTime2Score().keySet()) {
			if (gridImp.getTime2OriginTrips().get(a).size() == 0) {
				gridImp.getTime2OriginKpi().put(a, -1.);
			} else {
				double kpiO = gridImp.getTime2Score().get(a) / gridImp.getTime2OriginTrips().get(a).size();
				gridImp.getTime2OriginKpi().put(a, kpiO);
			}

			if (gridImp.getTime2DestinationTrips().get(a).size() == 0) {
				gridImp.getTime2DestinationKpi().put(a, -1.);
			} else {
				double kpiD = gridImp.getTime2Score().get(a) / gridImp.getTime2DestinationTrips().get(a).size();
				gridImp.getTime2DestinationKpi().put(a, kpiD);
			}
		}
	}

	private static void tripRatioCalculate(Trip trip) throws Exception {
		CarTravelTimeCalculator.getInstance().caculate(trip);
		PtTravelTimeCaculator.getInstance().caculate(trip);

		if (trip.getTravelDistance() <= 200) {
			trip.setRatio(0.);
		} else if (!trip.getPtTraveInfo().isUsePt()) {
			trip.setRatio(-1.);
		} else {
			trip.setRatio(trip.getPtTraveInfo().getTravelTime() / trip.getCarTravelInfo().getTravelTime());
			trip.setRatioWithOutWaitingTime(
					trip.getPtTraveInfo().getTraveLTimeWithOutWaitingTime() / trip.getCarTravelInfo().getTravelTime());
		}
	}

	private static void addStopsInfo2Link(GridImp gridImp) {
		for (LinkExtendImp linkExtendImp : gridImp.getLinkExtendImps()) {
			if (!gridImp.getTransitStopFacilityExtendImps().isEmpty()) {
				for (TransitStopFacilityExtendImp transitStopFacilityExtendImp : gridImp
						.getTransitStopFacilityExtendImps()) {
					try {
						linkExtendImp.addStopsInfo(transitStopFacilityExtendImp);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private static double scorecalculateFromMode(RouteStopInfo routeStopInfo) throws Exception {
		if (!PtAccessabilityConfig.getInstance().getModeScore().containsKey(routeStopInfo.getTransportMode())) {
			throw new Exception(routeStopInfo.getTransportMode() + " is not defined in config");
		} else {
			return PtAccessabilityConfig.getInstance().getModeScore().get(routeStopInfo.getTransportMode());
		}
	}

}
