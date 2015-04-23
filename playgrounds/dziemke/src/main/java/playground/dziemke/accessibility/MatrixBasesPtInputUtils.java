package playground.dziemke.accessibility;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author dziemke
 */
public class MatrixBasesPtInputUtils {

	public static void main(String[] args) {
//		String transitScheduleFile = "../../matsim/examples/pt-tutorial/transitschedule.xml";
//		String outputFileStops = "output/stops.csv";
//		String outputFileTravelMatrix = "output/travelmatrix.csv";
//		
//		double departureTime = 8. * 60 * 60;
//		String separator = "\t";
//		
//		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
//
//		scenario.getConfig().scenario().setUseTransit(true);
//		
//		TransitScheduleReader reader = new TransitScheduleReader(scenario);
//		reader.readFile(transitScheduleFile);
//		
//		Map<Id<TransitStopFacility>, TransitStopFacility> transitStopFacilitiesMap = scenario.getTransitSchedule().getFacilities();
//		
//		createStopsFileBasedOnSchedule(scenario, transitStopFacilitiesMap, outputFileStops, separator);
//		createTravelMatrixFile(scenario, transitStopFacilitiesMap, departureTime, outputFileTravelMatrix, separator);
	}
	
	
	/**
	 * Creates a csv file containing the public transport stops as they are given in the transit schedule
	 * 
	 * @param scenario
	 * @param transitStopFacilitiesMap
	 * @param outputFileStops
	 * @param separator
	 */
	//TODO use this in the future in stead of the one based on ActivityFacilities
	public static void createStopsFileBasedOnSchedule(Scenario scenario,
			Map<Id<TransitStopFacility>,TransitStopFacility> transitStopFacilitiesMap,
			String outputFileStops, String separator) {
		
		final InputsCSVWriter stopsWriter = new InputsCSVWriter(outputFileStops, separator);
		
		stopsWriter.writeField("id");
		stopsWriter.writeField("x");
		stopsWriter.writeField("y");
		stopsWriter.writeNewLine();
			
		for (TransitStopFacility transitStopFacility : transitStopFacilitiesMap.values()) {
			stopsWriter.writeField(transitStopFacility.getId());
			stopsWriter.writeField(transitStopFacility.getCoord().getX());
			stopsWriter.writeField(transitStopFacility.getCoord().getY());
			stopsWriter.writeNewLine();
		}
		stopsWriter.close();
	}
	

	/**
	 * same as above, but based on measure points
	 * 
	 * @param scenario
	 * @param measuringPoints
	 * @param outputFileStops
	 * @param separator
	 */
	public static void createStopsFileBasedOnMeasuringPoints(Scenario scenario,
			ActivityFacilities measuringPoints, String outputFileRoot, String separator) {
		
		final InputsCSVWriter stopsWriter = new InputsCSVWriter(
				outputFileRoot + "stops.csv", separator);
		
		stopsWriter.writeField("id");
		stopsWriter.writeField("x");
		stopsWriter.writeField("y");
		stopsWriter.writeNewLine();
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			stopsWriter.writeField(measuringPoint.getId());
			stopsWriter.writeField(measuringPoint.getCoord().getX());
			stopsWriter.writeField(measuringPoint.getCoord().getY());
			stopsWriter.writeNewLine();
		}
		stopsWriter.close();
	}


//	/**
//	 * 
//	 * @param scenario
//	 * @param transitStopFacilitiesMap
//	 * @param departureTime
//	 * @param outputFileTravelMatrix
//	 */
//	public static void createTravelMatrixFile(Scenario scenario, Map<Id<TransitStopFacility>,
//			TransitStopFacility> transitStopFacilitiesMap, double departureTime, String outputFileTravelMatrix,
//			String separator) {
//		
//		TransitSchedule transitSchedule = scenario.getTransitSchedule();
//		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(), 
//				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
//				scenario.getConfig().vspExperimental());
//		
//		// TransitRouterImpl(final TransitRouterConfig config, final TransitSchedule schedule)
//		TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);
//		
//		final InputsCSVWriter travelMatrixWriter = new InputsCSVWriter(outputFileTravelMatrix, separator);
//		
//		for (TransitStopFacility transitStopFacilityFrom : transitStopFacilitiesMap.values()) {
//			for (TransitStopFacility transitStopFacilityTo : transitStopFacilitiesMap.values()) {
//				Coord fromCoord = transitStopFacilityFrom.getCoord();
//				Coord toCoord = transitStopFacilityTo.getCoord();
////				System.out.println("from " + fromCoord.toString() + " to " + toCoord.toString());
//				
//				List<Leg> legList = transitRouter.calcRoute(fromCoord, toCoord, departureTime, null);
//				
//				double travelTime = 0.;
//				
//				if (legList != null) {
//					for(int j=0; j < legList.size(); j++) {
//						Leg leg = legList.get(j);
//						travelTime = travelTime + leg.getTravelTime();
//						
////						System.out.println("leg travel time = " + leg.getTravelTime());
//					}
////					System.out.println("overall travel time = " + travelTime);
//				} else {
//					travelTime = Integer.MAX_VALUE;
//					System.out.println("Infinity!");
//				}
//				
//				double fromCoordX = fromCoord.getX();
//				double fromCoordY = fromCoord.getY();
//				double toCoordX = toCoord.getX();
//				double toCoordY = toCoord.getY();
//				
//				travelMatrixWriter.writeField(fromCoordX);
//				travelMatrixWriter.writeField(fromCoordY);
//				travelMatrixWriter.writeField(toCoordX);
//				travelMatrixWriter.writeField(toCoordY);
//				travelMatrixWriter.writeField(travelTime);
//				travelMatrixWriter.writeNewLine();
//			}
//		}
//		travelMatrixWriter.close();
//	}
	
	
//	/**
//	 * same as above. however, not based on transit stops, but based on measuring points which are of the type ActivityFacility
//	 * 
//	 * @param scenario
//	 * @param measuringPoints
//	 * @param departureTime
//	 * @param outputFileTravelMatrix
//	 * @param separator
//	 */
//	public static void createTravelMatrixFile(Scenario scenario, ActivityFacilities measuringPoints, 
//			double departureTime, String outputFileTravelMatrix,
//			String separator) {
//		
//		TransitSchedule transitSchedule = scenario.getTransitSchedule();
//		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(scenario.getConfig().planCalcScore(), 
//				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
//				scenario.getConfig().vspExperimental());
//		
//		// TransitRouterImpl(final TransitRouterConfig config, final TransitSchedule schedule)
//		TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);
//		
//		final InputsCSVWriter travelMatrixWriter = new InputsCSVWriter(outputFileTravelMatrix, separator);
//		
////		for (TransitStopFacility transitStopFacilityFrom : transitStopFacilitiesMap.values()) {
////			for (TransitStopFacility transitStopFacilityTo : transitStopFacilitiesMap.values()) {
//		for (ActivityFacility measuringPointFrom : measuringPoints.getFacilities().values()) {
//			for (ActivityFacility measuringPointTo : measuringPoints.getFacilities().values()) {
////				Coord fromCoord = transitStopFacilityFrom.getCoord();
////				Coord toCoord = transitStopFacilityTo.getCoord();
//				Coord fromCoord = measuringPointFrom.getCoord();
//				Coord toCoord = measuringPointTo.getCoord();
////				System.out.println("from " + fromCoord.toString() + " to " + toCoord.toString());
//				
//				List<Leg> legList = transitRouter.calcRoute(fromCoord, toCoord, departureTime, null);
//				
//				double travelTime = 0.;
//				
//				if (legList != null) {
//					for(int j=0; j < legList.size(); j++) {
//						Leg leg = legList.get(j);
//						travelTime = travelTime + leg.getTravelTime();
//						
////						System.out.println("leg travel time = " + leg.getTravelTime());
//					}
////					System.out.println("overall travel time = " + travelTime);
//				} else {
//					travelTime = Integer.MAX_VALUE;
//					System.out.println("Infinity!");
//				}
//				
//				double fromCoordX = fromCoord.getX();
//				double fromCoordY = fromCoord.getY();
//				double toCoordX = toCoord.getX();
//				double toCoordY = toCoord.getY();
//				
//				travelMatrixWriter.writeField(fromCoordX);
//				travelMatrixWriter.writeField(fromCoordY);
//				travelMatrixWriter.writeField(toCoordX);
//				travelMatrixWriter.writeField(toCoordY);
//				travelMatrixWriter.writeField(travelTime);
//				travelMatrixWriter.writeNewLine();
//			}
//		}
//		travelMatrixWriter.close();
//	}
	
	
	
	/**
	 * Travel distance and travel times files are optional in matrix-based pt accessibility.
	 * If they are not provided, a routing on the empty network using a freeModeSpeed is performed.
	 * this should actually, yield (almost) the same results as the (car) accessibility computation on an empty network
	 * The following calculates travel times based on the use the transit schedule; multi-threaded
	 * 
	 * @param scenario
	 * @param measuringPoints
	 * @param departureTime
	 * @param outputFileRoot
	 * @param separator
	 */
	public static void createTravelMatrixFileBasedOnMeasuringPoints(Scenario scenario, ActivityFacilities measuringPointsFrom, 
			ActivityFacilities measuringPointsTo, double departureTime, String outputFileRoot, String separator, int threadName) {
		
		new ThreadedMatrixCreator(scenario, measuringPointsFrom, measuringPointsTo, departureTime, outputFileRoot, separator, threadName);		
	}
	
	
//	/**
//	 * same as above, but based on actual stops instead of measuring points
//	 * 
//	 * @param scenario
//	 * @param measuringPoints
//	 * @param departureTime
//	 * @param outputFileTravelMatrix
//	 * @param separator
//	 */
//	public static void createTravelMatrixFileBasedOnSchedule(Scenario scenario, ActivityFacilities transitStopFacilitiesMap, 
//			ActivityFacilities transitStopFacilitiesMap, double departureTime, String outputFileTravelMatrix, String separator, String threadName) {
//		
//		new ThreadedCreator(scenario, measuringPointsFrom, measuringPointsTo, departureTime, outputFileTravelMatrix, separator, threadName);		
//	}
}


/**
 * 
 * @author dziemke
 *
 */
class ThreadedMatrixCreator implements Runnable {
	Thread thread;
	Integer threadNumber;
	Scenario scenario;
	ActivityFacilities measuringPointsFrom;
	ActivityFacilities measuringPointsTo;
	double departureTime;
	String outputFileRoot;
	String separator;
	
	
	ThreadedMatrixCreator( Scenario scenario, ActivityFacilities measuringPointsFrom, 
			ActivityFacilities measuringPointsTo, double departureTime, String outputFileRoot, String separator, int threadNumber){
		this.scenario = scenario;
		this.measuringPointsFrom = measuringPointsFrom;
		this.measuringPointsTo = measuringPointsTo;
		this.departureTime = departureTime;
		this.outputFileRoot = outputFileRoot;
		this.separator = separator;
		this.threadNumber = threadNumber;
		
		thread = new Thread (this, this.threadNumber.toString());
		thread.start ();
	}


	public void run() {
		TransitSchedule transitSchedule = this.scenario.getTransitSchedule();
		TransitRouterConfig transitRouterConfig = new TransitRouterConfig(this.scenario.getConfig().planCalcScore(), 
				this.scenario.getConfig().plansCalcRoute(), this.scenario.getConfig().transitRouter(),
				this.scenario.getConfig().vspExperimental());
		TransitRouter transitRouter = new TransitRouterImpl(transitRouterConfig, transitSchedule);
	    
	    final InputsCSVWriter travelTimeMatrixWriter = new InputsCSVWriter(
	    		this.outputFileRoot + "travelTimeMatrix_" + this.threadNumber + ".csv", this.separator);
	    final InputsCSVWriter travelDistanceMatrixWriter = new InputsCSVWriter(
	    		this.outputFileRoot + "travelDistanceMatrix_" + this.threadNumber + ".csv", this.separator);
	    
//	    int routeGiven = 0;
//	    int noRouteGiven = 0;

		for (ActivityFacility measuringPointFrom : this.measuringPointsFrom.getFacilities().values()) {
			for (ActivityFacility measuringPointTo : this.measuringPointsTo.getFacilities().values()) {
				Coord fromCoord = measuringPointFrom.getCoord();
				Coord toCoord = measuringPointTo.getCoord();
				
				List<Leg> legList = transitRouter.calcRoute(fromCoord, toCoord, this.departureTime, null);
				
				double travelTime = 0.;
				double travelDistance = 0.;
				
				if (legList != null) {
					for(int j=0; j < legList.size(); j++) {
						Leg leg = legList.get(j);
						travelTime = travelTime + leg.getTravelTime();
						Route route = leg.getRoute();
						if (route != null) {
							travelDistance = travelDistance + route.getDistance();
//							routeGiven++;
						} else {
							travelDistance = 1.3 * Math.sqrt( Math.pow(fromCoord.getX() - toCoord.getX(), 2)
									+ Math.pow(fromCoord.getY() - toCoord.getY(), 2) );
//							System.out.println("No route given!");
//							noRouteGiven++;
//							System.out.println("Share of no route given = " + noRouteGiven/routeGiven);
						}
					}
				} else {
					travelTime = Integer.MAX_VALUE;
					travelDistance = Integer.MAX_VALUE;
					System.out.println("Infinity!");
				}

				travelTimeMatrixWriter.writeField(measuringPointFrom.getId());
				travelTimeMatrixWriter.writeField(measuringPointTo.getId());
				travelTimeMatrixWriter.writeField(travelTime);
				travelTimeMatrixWriter.writeNewLine();
				
				travelDistanceMatrixWriter.writeField(measuringPointFrom.getId());
				travelDistanceMatrixWriter.writeField(measuringPointTo.getId());
				travelDistanceMatrixWriter.writeField(travelDistance);
				travelDistanceMatrixWriter.writeNewLine();
			}
		}
		travelTimeMatrixWriter.close();
		travelDistanceMatrixWriter.close();
	}
}