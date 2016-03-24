package playground.dziemke.analysis.srv;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkUtils;

import playground.dziemke.analysis.AnalysisFileWriter;
import playground.dziemke.analysis.AnalysisUtils;
import playground.dziemke.analysis.Trip;
import playground.dziemke.analysis.TripAnalyzerBasic;

/**
 * @author dziemke
 */
public class TripAnalyzerSrVNew {
	public static final Logger log = Logger.getLogger(TripAnalyzerSrVNew.class);
	
	/* Parameters */
	private static final boolean useWeights = true;			//wt
	private static final boolean onlyCar = false;			//car
	private static final boolean onlyCarAndCarPool = true;	//carp
	private static final boolean onlyHomeAndWork = false;	//hw
	private static final boolean distanceFilter = true;		//dist
	private static final boolean ageFilter = false;
	
	private static final Integer minAge = 80; // typically "x0"
	private static final Integer maxAge = 119; // typically "x9"; highest number ususally chosen is 119	
	

//	private static final boolean onlyCar = false; // "car"; should be used for runs with ChangeLegMode enabled
//	private static final boolean onlyInterior = false; // "int"
//	private static final boolean onlyBerlinBased = true; // "ber"; usually varied for analysis
//	private static final boolean distanceFilter = true; // "dist"; usually varied for analysis
	// private static final double double minDistance = 0;
	private static final double maxDistance_km = 100; // most frequently used value: 150
//
//	private static final boolean onlyWorkTrips = false; // "work"
//
//	private static final boolean ageFilter = false; // "age"
//	private static final Integer minAge = 80; // typically "x0"
//	private static final Integer maxAge = 119; // typically "x9"; higehst number ususally chosen is 119

	private static final int maxBinDuration_min = 120;
	private static final int binWidthDuration_min = 1;

	private static final int maxBinTime_h = 27;
	private static final int binWidthTime_h = 1;

	private static final int maxBinDistance_km = 60;
	private static final int binWidthDistance_km = 1;

	private static final int maxBinSpeed_km_h = 60;
	private static final int binWidthSpeed_km_h = 1;

	/* Input and output */
	private static final String inputFileTrips = "../../../shared-svn/projects/cemdapMatsimCadyts/analysis/srv/input/W2008_Berlin_Weekday.dat";
	private static final String inputFilePersons = "../../../shared-svn/projects/cemdapMatsimCadyts/analysis/srv/input/P2008_Berlin2.dat";
	
	private static final String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
//	private static finalString shapeFile = "/Users/dominik/Workspace/data/srv/input/RBS_OD_STG_1412/RBS_OD_STG_1412.shp";
			
	private static String outputDirectory = "../../../shared-svn/projects/cemdapMatsimCadyts/analysis/srv/output/wd_neu_2016_3";
	
	
//	private static final String networkFile = "../../../shared-svn/studies/countries/de/berlin/counts/iv_counts/network.xml";
//	private static final String eventsFile = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/ITERS/it." + usedIteration + 
//			"/" + runId + "." + usedIteration + ".events.xml.gz";
//	private static final String cemdapPersonsInputFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/cemdap_berlin/" + 
//			cemdapPersonsInputFileId + "/persons1.dat";
//	private static final String planningAreaShapeFile = "../../../shared-svn/projects/cemdapMatsimCadyts/scenario/shapefiles/Berlin_DHDN_GK4.shp";
//	private static String outputDirectory = "../../../runs-svn/cemdapMatsimCadyts/" + runId + "/analysis";
	
	private static String gnuplotScriptName = "plot_rel_path_run.gnu";


	/* Variables to store information */
	private static int numberOfConsideredTrips = 0;
	private static int numberOfInIncompleteTrips = 0;
    
	private static Map<Id<Trip>, Double> distanceRoutedMap = new TreeMap<>();
	private static Map<Id<Trip>, Double> distanceBeelineMap = new TreeMap<>();
    
	private static Map<String, Integer> otherInformationMap = new TreeMap<>();

	
	public static void main(String[] args) {
		int numberOfTripsWithNonNegativeTimesAndDurations = 0;
		int numberOfTripsWithCalculableSpeed = 0;
		
		adaptOutputDirectory();
	    
		/* Events infrastructure and reading the events file */
//	    EventsManager eventsManager = EventsUtils.createEventsManager();
//	    TripHandler tripHandler = new TripHandler();
//	    eventsManager.addHandler(tripHandler);
//	    MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
//	    eventsReader.readFile(eventsFile);
//	    log.info("Events file read!");
//	     
//	    /* Get network, which is needed to calculate distances */
	    Network network = NetworkUtils.createNetwork();
	    MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
	    networkReader.readFile(networkFile);
//	    
//	    Map<Integer, Geometry> zoneGeometries = ShapeReader.read(planningAreaShapeFile, "NR");
//		planningAreaGeometry = zoneGeometries.get(planningAreaId);	
		
		
		// parse trip file
		log.info("Parsing " + inputFileTrips + ".");		
		SrV2008TripParser tripParser = new SrV2008TripParser();
		tripParser.parse(inputFileTrips);
		log.info("Finished parsing trips.");

		// parse person file
		log.info("Parsing " + inputFilePersons + ".");		
		SrV2008PersonParser personParser = new SrV2008PersonParser();
		personParser.parse(inputFilePersons);
		log.info("Finished parsing persons.");
		
	    
		AnalysisFileWriter writer = new AnalysisFileWriter();

//		if (ageFilter == true) {
//	    	// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
//	    	CemdapPersonInputFileReader cemdapPersonInputFileReader = new CemdapPersonInputFileReader();
//		 	cemdapPersonInputFileReader.parse(cemdapPersonsInputFile);
//		 	cemdapPersonAttributes = cemdapPersonInputFileReader.getPersonAttributes();
//	    }
	    
//	    List<Trip> trips = createListOfValidTrip(tripHandler.getTrips(), network, personParser);
	    List<Trip> trips = createListOfValidTrip(tripParser.getTrips(), network, personParser);
	    
	    /* Do calculations and write-out*/
	    // reliant on variable "V_ANKUNFT": -9 = no data, -10 = implausible
		// and on variable "V_BEGINN": -9 = no data, -10 = implausible
	    Map <Integer, Double> tripDurationMap = TripAnalyzerBasic.createTripDurationMap(trips, binWidthDuration_min, maxBinDuration_min);
	    double averageTripDuration = TripAnalyzerBasic.calculateAverageTripDuration_min(trips);
//	    Map <Integer, Double> tripDurationMap = createTripDurationMap(trips, binWidthDuration_min, maxBinDuration_min); // local
//	    double averageTripDuration = calculateAverageTripDuration_min(trips); // local
	    writer.writeToFileIntegerKey(tripDurationMap, outputDirectory + "/tripDuration.txt", binWidthDuration_min, numberOfConsideredTrips, averageTripDuration);
	    writer.writeToFileIntegerKeyCumulative(tripDurationMap, outputDirectory + "/tripDurationCumulative.txt", binWidthDuration_min, numberOfConsideredTrips, averageTripDuration);
	    
	    numberOfTripsWithNonNegativeTimesAndDurations = TripAnalyzerBasic.countTripsWithNonNegativeTimesAndDurations(trips);
	    
	    Map <Integer, Double> departureTimeMap = TripAnalyzerBasic.createDepartureTimeMap(trips, binWidthTime_h, maxBinTime_h);
	    writer.writeToFileIntegerKey(departureTimeMap, outputDirectory + "/departureTime.txt", binWidthTime_h, numberOfConsideredTrips, -99);
	    	    
	    Map<String, Double> activityTypeMap = TripAnalyzerBasic.createActivityTypeMap(trips);
	    writer.writeToFileStringKey(activityTypeMap, outputDirectory + "/activityTypes.txt", numberOfConsideredTrips);
	    
//	    Map<Integer, Double> tripDistanceBeelineMap = TripAnalyzerBasic.createTripDistanceBeelineMap(trips, binWidthDistance_km, maxBinDistance_km, network);
//		double averageTripDistanceBeeline_km = TripAnalyzerBasic.calculateAverageTripDistanceBeeline_km(trips, network);
		Map<Integer, Double> tripDistanceBeelineMap = createTripDistanceBeelineMap(trips, binWidthDistance_km); // SrV-specific, distances given by survey
		double averageTripDistanceBeeline_km = calculateAverageTripDistanceBeeline_km(trips); // SrV-specific, distances given by survey
		writer.writeToFileIntegerKey(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeeline.txt", binWidthDistance_km, numberOfConsideredTrips, averageTripDistanceBeeline_km);
		writer.writeToFileIntegerKeyCumulative(tripDistanceBeelineMap, outputDirectory + "/tripDistanceBeelineCumulative.txt", binWidthDistance_km, numberOfConsideredTrips, averageTripDistanceBeeline_km);
	    
//	    Map<Integer, Double> tripDistanceRoutedMap = TripAnalyzerBasic.createTripDistanceRoutedMap(trips, binWidthDistance_km, maxBinDistance_km, network);
//	    double averageTripDistanceRouted_km = TripAnalyzerBasic.calculateAverageTripDistanceRouted_km(trips, network);
	    Map<Integer, Double> tripDistanceRoutedMap = createTripDistanceRoutedMap(trips, binWidthDistance_km); // SrV-specific, distances given by survey
	    double averageTripDistanceRouted_km = calculateAverageTripDistanceRouted_km(trips); // SrV-specific, distances given by survey
	    writer.writeToFileIntegerKey(tripDistanceRoutedMap, outputDirectory + "/tripDistanceRouted.txt", binWidthDistance_km, numberOfConsideredTrips, averageTripDistanceRouted_km);
	    
//	    numberOfTripsWithCalculableSpeed = TripAnalyzerBasic.countTripsWithCalculableSpeed(trips);
	    numberOfTripsWithCalculableSpeed = countTripsWithCalculableSpeed(trips); // SrV-specific, distances given by survey
	    
//	    Map<Integer, Double> averageTripSpeedBeelineMap = TripAnalyzerBasic.createAverageTripSpeedBeelineMap(trips, binWidthSpeed_km_h, network);
//		double averageOfAverageTripSpeedsBeeline_km_h = TripAnalyzerBasic.calculateAverageOfAverageTripSpeedsBeeline_km_h(trips, network);
		Map<Integer, Double> averageTripSpeedBeelineMap = createAverageTripSpeedBeelineMap(trips, binWidthSpeed_km_h, network); // SrV-specific, distances given by survey
		double averageOfAverageTripSpeedsBeeline_km_h = calculateAverageOfAverageTripSpeedsBeeline_km_h(trips, network); // SrV-specific, distances given by survey
		writer.writeToFileIntegerKey(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeeline.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeed, averageOfAverageTripSpeedsBeeline_km_h);
		writer.writeToFileIntegerKeyCumulative(averageTripSpeedBeelineMap, outputDirectory + "/averageTripSpeedBeelineCumulative.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeed, averageOfAverageTripSpeedsBeeline_km_h);
		
//		Map<Integer, Double> averageTripSpeedRoutedMap = TripAnalyzerBasic.createAverageTripSpeedRoutedMap(trips, binWidthSpeed_km_h, maxBinSpeed_km_h, network);
//		double averageOfAverageTripSpeedsRouted_km_h = TripAnalyzerBasic.calculateAverageOfAverageTripSpeedsRouted_km_h(trips, network);
		Map<Integer, Double> averageTripSpeedRoutedMap = createAverageTripSpeedRoutedMap(trips, binWidthSpeed_km_h, maxBinSpeed_km_h, network); // SrV-specific, distances given by survey
		double averageOfAverageTripSpeedsRouted_km_h = calculateAverageOfAverageTripSpeedsRouted_km_h(trips, network); // SrV-specific, distances given by survey
		writer.writeToFileIntegerKey(averageTripSpeedRoutedMap, outputDirectory + "/averageTripSpeedRouted.txt", binWidthSpeed_km_h, numberOfTripsWithCalculableSpeed, averageOfAverageTripSpeedsRouted_km_h);

		/* Other information */
//	    otherInformationMap.put("Number of trips that have no previous activity", tripHandler.getNoPreviousEndOfActivityCounter()); // TODO reactivate
		otherInformationMap.put("Number of trips that have no negative times or durations", numberOfConsideredTrips - numberOfTripsWithNonNegativeTimesAndDurations);		
	    otherInformationMap.put("Number of trips that have no calculable speed", numberOfConsideredTrips - numberOfTripsWithCalculableSpeed);
	    otherInformationMap.put("Number of incomplete trips (i.e. number of removed agents)", numberOfInIncompleteTrips);
	    otherInformationMap.put("Number of (complete) trips", numberOfConsideredTrips);
	    writer.writeToFileOther(otherInformationMap, outputDirectory + "/otherInformation.txt");
	    
//	    // write a routed distance vs. beeline distance comparison file
//	    doBeelineCaluclations(trips, binWidthDistance_km, maxBinDistance_km, network);
//	    writer.writeRoutedBeelineDistanceComparisonFile(distanceRoutedMap, distanceBeelineMap, outputDirectory + "/beeline.txt", numberOfConsideredTrips);
	    	    
	    log.info(numberOfInIncompleteTrips + " trips are incomplete.");
	}
	
	
	/* SrV-specific calculations using distances given by survey instead of own calculated distances */
	public static Map<Integer, Double> createTripDistanceBeelineMap(List<Trip> trips, int binWidthDistance_km) {
		Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
		for (Trip trip : trips) {
			if (trip.getDistanceBeelineFromSurvey_m() >= 0) {
				double tripDistanceBeeline_km = trip.getDistanceBeelineFromSurvey_m() / 1000.;
				double tripWeight = trip.getWeight();
				AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, tripWeight);
			}
		}
		return tripDistanceBeelineMap;
	}

	public static double calculateAverageTripDistanceBeeline_km(List<Trip> trips) {
		double sumOfAllDistancesBeeline_km = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			if (trip.getDistanceBeelineFromSurvey_m() >= 0) {
				sumOfAllDistancesBeeline_km += (trip.getDistanceBeelineFromSurvey_m() / 1000.);
				sumOfAllWeights += trip.getWeight();
			}
		}
		return sumOfAllDistancesBeeline_km / sumOfAllWeights;
	}
	
	public static Map<Integer, Double> createTripDistanceRoutedMap(List<Trip> trips, int binWidthDistance_km) {
    	Map<Integer, Double> tripDistanceRoutedMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		if (trip.getDistanceRoutedShortestFromSurvey_m() >= 0) {
    			double tripDistanceRouted_km = trip.getDistanceRoutedShortestFromSurvey_m() / 1000.;
    			double tripWeight = trip.getWeight();
    			AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceRoutedMap, tripDistanceRouted_km, binWidthDistance_km, tripWeight);
    		}
    	}
    	return tripDistanceRoutedMap;
    }
	
	public static double calculateAverageTripDistanceRouted_km(List<Trip> trips) {
		double sumOfAllDistancesRouted_km = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			if (trip.getDistanceRoutedShortestFromSurvey_m() >= 0) {
				sumOfAllDistancesRouted_km += (trip.getDistanceRoutedShortestFromSurvey_m() / 1000.);
				sumOfAllWeights += trip.getWeight();
			}
		}
		return sumOfAllDistancesRouted_km / sumOfAllWeights;
	}
	
	// speeds
	public static int countTripsWithCalculableSpeed(List<Trip> trips) {
		int counter = 0;
		for (Trip trip : trips) {
			double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
			double tripDistanceBeeline_km = trip.getDistanceBeelineFromSurvey_m() / 1000.;
			if (tripDuration_h >= 0. && tripDistanceBeeline_km >= 0.) {
	    		counter++;
			}
		}
		return counter;
	}
	
	public static Map<Integer, Double> createAverageTripSpeedBeelineMap(List<Trip> trips, int binWidthSpeed_km_h, Network network) {
		Map<Integer, Double> averageTripSpeedBeelineMap = new TreeMap<>();
		for (Trip trip : trips) {
			double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
			double tripDistanceBeeline_km = trip.getDistanceBeelineFromSurvey_m() / 1000.;
			if (tripDuration_h >= 0. && tripDistanceBeeline_km >= 0.) {
	    		double tripWeight = trip.getWeight();
	    		AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedBeelineMap, (tripDistanceBeeline_km / tripDuration_h), binWidthSpeed_km_h, tripWeight);
			}
		}
		return averageTripSpeedBeelineMap;
	}

	public static double calculateAverageOfAverageTripSpeedsBeeline_km_h(List<Trip> trips, Network network) {
		double sumOfAllAverageSpeedsBeeline_km_h = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
			double tripDistanceBeeline_km = (trip.getDistanceBeelineFromSurvey_m() / 1000.);
			if (tripDuration_h >= 0. && tripDistanceBeeline_km >= 0.) {
				sumOfAllAverageSpeedsBeeline_km_h += (tripDistanceBeeline_km / tripDuration_h);
				sumOfAllWeights += trip.getWeight();
			}
		}
		return sumOfAllAverageSpeedsBeeline_km_h / sumOfAllWeights;
	}

	public static Map<Integer, Double> createAverageTripSpeedRoutedMap(List<Trip> trips, int binWidthSpeed_km_h, int maxBinSpeed_km_h, Network network) {
    	Map<Integer, Double> averageTripSpeedRoutedMap = new TreeMap<>();
    	for (Trip trip : trips) {
    		double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
    		double tripDistanceRouted_km = trip.getDistanceRoutedShortestFromSurvey_m() / 1000.;
    		if (tripDuration_h >= 0. && tripDistanceRouted_km >= 0.) {
    			double tripWeight = trip.getWeight();
	    		AnalysisUtils.addToMapIntegerKeyCeiling(averageTripSpeedRoutedMap, (tripDistanceRouted_km / tripDuration_h), binWidthSpeed_km_h, //maxBinSpeed_km_h, 
	    				tripWeight);
    		}
    	}
    	return averageTripSpeedRoutedMap;
    }
	
	public static double calculateAverageOfAverageTripSpeedsRouted_km_h(List<Trip> trips, Network network) {
		double sumOfAllAverageSpeedsRouted_km_h = 0.;
		double sumOfAllWeights = 0.;
		for (Trip trip : trips) {
			double tripDuration_h = trip.getDurationByCalculation_s() / 3600.;
			double tripDistanceRouted_km = (trip.getDistanceRoutedShortestFromSurvey_m() / 1000.);
			if (tripDuration_h >= 0. && tripDistanceRouted_km >= 0.) {
				sumOfAllAverageSpeedsRouted_km_h += (tripDistanceRouted_km / tripDuration_h);
				sumOfAllWeights += trip.getWeight();
			}
    	}
		return sumOfAllAverageSpeedsRouted_km_h / sumOfAllWeights;
	}
	// end SrV-specific stuff
	
	
//	static void doBeelineCaluclations(List<Trip> trips, int binWidthDistance_km, int maxBinDistance_km, Network network) {
//		Map<Integer, Double> tripDistanceBeelineMap = new TreeMap<>();
//		for (Trip trip : trips) {
//			double tripDistanceRouted_km = trip.getDistanceRoutedShortestFromSurvey_m() / 1000.;
//			double tripDistanceBeeline_km = trip.getDistanceBeelineFromSurvey_m() / 1000.;
//			double tripWeight = trip.getWeight();
//			AnalysisUtils.addToMapIntegerKeyCeiling(tripDistanceBeelineMap, tripDistanceBeeline_km, binWidthDistance_km, //maxBinDistance_km, 
//					tripWeight);
//			
//			distanceBeelineMap.put(trip.getTripId(), tripDistanceBeeline_km); // TODO eventually remove this
//			distanceRoutedMap.put(trip.getTripId(), tripDistanceRouted_km); // TODO eventually remove this
//		}
//	}
	
	
	@SuppressWarnings("all")
	private static void adaptOutputDirectory() {
//		outputDirectory = outputDirectory + "_" + usedIteration;
//	    if (onlyCar == true) {
//			outputDirectory = outputDirectory + "_car";
//		}
//	    if (onlyInterior == true) {
//			outputDirectory = outputDirectory + "_int";
//	    }
//		if (onlyBerlinBased == true) {
//			outputDirectory = outputDirectory + "_ber";
//		}
//		if (distanceFilter == true) {
//			outputDirectory = outputDirectory + "_dist";
//		}
//		if (onlyWorkTrips == true) {
//			outputDirectory = outputDirectory + "_work";
//		}
//		if (ageFilter == true) {
//			outputDirectory = outputDirectory + "_age_" + minAge.toString();
//			outputDirectory = outputDirectory + "_" + maxAge.toString();
//		}
//		outputDirectory = outputDirectory + "_6"; // TODO in case used for double-check
//		
//		/* Create directory */
//		new File(outputDirectory).mkdir();
		
		
		
		if (useWeights == true) {
			outputDirectory = outputDirectory + "_wt";
		}
		
		if (onlyCar == true) {
			outputDirectory = outputDirectory + "_car";
		}
		
		if (onlyCarAndCarPool == true) {
			outputDirectory = outputDirectory + "_carp";
		}
		
		if (onlyCar == false && onlyCarAndCarPool == false) {
			outputDirectory = outputDirectory + "_all";
		}
				
		if (distanceFilter == true) {
			outputDirectory = outputDirectory + "_dist";
		}
		
		if (onlyHomeAndWork == true) {
			outputDirectory = outputDirectory + "_hw";
		}		
				
		if (ageFilter == true) {
			outputDirectory = outputDirectory + "_" + minAge.toString();
			outputDirectory = outputDirectory + "_" + maxAge.toString();
		}

//		outputDirectory = outputDirectory + "/";
		/* Create directory */
		new File(outputDirectory).mkdir();
		
	}
	
	
	@SuppressWarnings("all")
	private static List<Trip> createListOfValidTrip(Map<Id<Trip>, Trip> tripMap, Network network, SrV2008PersonParser personParser) {
		List<Trip> trips = new LinkedList<>();
		
		for (Trip trip : tripMap.values()) {

//			// get coordinates of links
//			Id<Link> departureLinkId = trip.getDepartureLinkId();
//			Id<Link> arrivalLinkId = trip.getArrivalLinkId();
//			//
//			Link departureLink = network.getLinks().get(departureLinkId);
//			Link arrivalLink = network.getLinks().get(arrivalLinkId);
//
//			// TODO use coords of toNode instead of center coord of link
//			double arrivalCoordX = arrivalLink.getCoord().getX();
//			double arrivalCoordY = arrivalLink.getCoord().getY();
//			double departureCoordX = departureLink.getCoord().getX();
//			double departureCoordY = departureLink.getCoord().getY();
//
//			// create points
//			Point arrivalLocation = MGC.xy2Point(arrivalCoordX, arrivalCoordY);
//			Point departureLocation = MGC.xy2Point(departureCoordX, departureCoordY);
//
//			// choose if trip will be considered
//			if (onlyBerlinBased == true) {
//				if (!planningAreaGeometry.contains(arrivalLocation) && !planningAreaGeometry.contains(departureLocation)) {
//					continue;
//				}
//			}
//			if (onlyInterior == true) {
//				if (!planningAreaGeometry.contains(arrivalLocation) || !planningAreaGeometry.contains(departureLocation)) {
//					continue;
//				}
//			}
////			if (!trip.getMode().equals("car") && !trip.getMode().equals("pt")) {
////				throw new RuntimeException("In current implementation leg mode must either be car or pt");
////			}
//			if (onlyCar == true) {
//				if (!trip.getMode().equals("car")) {
//					continue;
//				}
//			}
//			if (distanceFilter == true && (trip.getDistanceBeelineByCalculation_m(network) / 1000.) >= maxDistance_km) {
//				continue;
//			}
////			if (distanceFilter == true && (trip.getBeelineDistance(network) / 1000.) <= minDistance) {
////    			continue;
////    		}
//			if (onlyWorkTrips == true) {
//				if (trip.getActivityEndActType().equals("work")) {
//					continue;
//				}
//			}
//
//			// TODO The plan was to calculate activity-chain frequencies here. Needs to be done somewhere else
//			// write person activity attributes
////   	 	if (trip.getActivityEndActType().equals("work")) {
////   	 		personActivityAttributes.putAttribute(trip.getDriverId(), "hasWorkActivity", true);
////    		}
//
//			/* Person-specific attributes */
//			if (ageFilter == true) {
//				// TODO needs to be adapted for other analyses that are based on person-specific attributes as well
//				// so far age is the only one
//				String personId = trip.getPersonId().toString();
//				int age = (int) cemdapPersonAttributes.getAttribute(personId, "age");
//
//				if (age < minAge) {
//					continue;
//				}
//				if (age > maxAge) {
//					continue;
//				}
//			}
//
//			/* The only case where incomplete trips occur is when agents are removed according to "removeStuckVehicles = true"
//			 * Since a removed agent can at most have one incomplete trip (the one when he is removed), the number of
//			 * incomplete trips should be equal to the number of removed agents */
//			if(!trip.getTripComplete()) {
//				System.err.println("Trip is not complete!");
//				numberOfInIncompleteTrips++;
//				continue;
//			}	    	
			
			
//			boolean considerTrip = false;

	    	// mode of transport and activity type
	    	// reliant on variable "V_HHPKW_F": 0/1
	    	int useHouseholdCar = trip.getUseHouseholdCar();
	    	// reliant on variable "V_ANDPKW_F": 0/1
	    	int useOtherCar = trip.getUseOtherCar();
	    	// reliant on variable "V_HHPKW_MF": 0/1
	    	int useHouseholdCarPool = trip.getUseHouseholdCarPool();
	    	// reliant on variable "V_ANDPKW_MF": 0/1
	    	int useOtherCarPool = trip.getUseOtherCarPool();

	    	String activityEndActType = trip.getActivityEndActType();
	    	String activityStartActType = trip.getActivityStartActType();

	    	if (onlyHomeAndWork == true) {
	    		if (!(activityEndActType.equals("home") && activityStartActType.equals("work")) && !(activityEndActType.equals("work") && activityStartActType.equals("home"))) {
	    			continue;
	    		}
	    	}

	    	if (onlyCar == true) {
	    		if (!(useHouseholdCar == 1) && !(useOtherCar == 1)) {		 
	    			continue;
	    		}
	    	}
	    	if (onlyCarAndCarPool == true) {
	    		if (!(useHouseholdCar == 1) && !(useOtherCar == 1) && !(useHouseholdCarPool == 1) && !(useOtherCarPool == 1)) {		 
	    			continue;
	    		}
	    	} 
	
	    	// distance
	    	double tripDistanceBeeline_km = trip.getDistanceBeelineFromSurvey_m() / 1000.;
	    	if (distanceFilter == true && tripDistanceBeeline_km >= maxDistance_km) {
	    		continue;
	    	}
//	    	if (distanceFilter == true && tripDistanceBeeline_km <= minDistance_km) {
//	    		continue;
//	    	}

	    	// age
	    	String personId = trip.getPersonId().toString();
	    	
	    	if (ageFilter == true) {
	    		int age = (int) personParser.getPersonAttributes().getAttribute(personId, "age");
	    		if (age < minAge) {
	    			continue;
	    		}
	    		if (age > maxAge) {
	    			continue;
	    		}
	    	}
	    	
	    	// make weighting correct
	    	double weight;
    		if (useWeights == true) {
    			weight = trip.getWeight();
    		} else {
    			weight = 1.;
    		}
			trip.setWeight(weight);

			/* Only trips that fullfill all checked criteria are added; otherwise that loop would have been "continued" already */
			trips.add(trip);
			numberOfConsideredTrips++;
		}
		
		return trips;
	}
}