package playground.sergioo.hits2012Scheduling.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
//import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.probability.ContinuousRealDistribution;
import playground.sergioo.accessibility2013.MultiDestinationDijkstra;
import playground.sergioo.hits2012.HitsReader;
import playground.sergioo.hits2012.Household;
import playground.sergioo.hits2012.Person;
import playground.sergioo.hits2012.Stage;
import playground.sergioo.hits2012.Trip;
import playground.sergioo.hits2012.Trip.PlaceType;
import playground.sergioo.hits2012.Trip.Purpose;
import playground.sergioo.hits2012.stages.MotorDriverStage;
import playground.sergioo.hits2012Scheduling.IncomeEstimation;
import playground.sergioo.passivePlanning2012.core.population.PlacesConnoisseur;
import playground.sergioo.passivePlanning2012.core.population.agenda.Agenda;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.CurrentTime;
import playground.sergioo.passivePlanning2012.population.parallelPassivePlanning.PassivePlannerManager.MobsimStatus;
import playground.sergioo.scheduling2013.SchedulingNetwork;
import playground.sergioo.scheduling2013.SchedulingNetwork.SchedulingLink;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterVariableImpl;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterWSImplFactory;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeCalculator;
import playground.sergioo.weeklySimulation.util.misc.Time;

public class TestSchedulingHITS {

	private static final double FRAC_SAMPLE = 0.8;
	private static CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
	private static Set<String> IMPORTANT_TYPES = new HashSet<>(Arrays.asList(new String[]{PlaceType.SHOP.text, PlaceType.EAT.text,
			PlaceType.CIVIC.text, PlaceType.HOME_OTHER.text, PlaceType.PARK.text, PlaceType.REC.text}));
	private static Set<String> FLEX_ATIVITIES = new HashSet<>(Arrays.asList(new String[]{Trip.Purpose.EAT.text,
			Trip.Purpose.ERRANDS.text, Trip.Purpose.REC.text, Trip.Purpose.SHOP.text, Trip.Purpose.SOCIAL.text}));
	private static int NUM_BUILDINGS = 15;

	private static class LocationInfo {
		private String location;
		private Coord coord;
		private Node node;
		private Set<String> types = new HashSet<>();
		private Set<String> purposes = new HashSet<>();
		public LocationInfo(String location, NetworkImpl net) {
			super();
			this.location = location;
			this.coord = coordinateTransformation.transform(Household.LOCATIONS.get(location).getCoord());
			this.node = net.getNearestNode(coord);
		}
	}
	private static class TypePlaceInfo {
		private String type;
		private Map<String, double[]> purposes = new HashMap<>();
		public TypePlaceInfo(String type) {
			this.type = type;
		}
	}
	private static class PlaceToLocation {
		private LocationInfo home;
		private LocationInfo location;
		private Set<String> types = new HashSet<>();
		private double euclideanDistance;
		private double travelTime;
		private double networkDistance;
		public PlaceToLocation(LocationInfo home, LocationInfo location) {
			super();
			this.home = home;
			this.location = location;
			this.euclideanDistance = CoordUtils.calcDistance(home.coord, location.coord);
		}
	}
	private static class LocationScore {
		private LocationInfo location;
		private double score;
		public LocationScore(LocationInfo location, double score) {
			super();
			this.location = location;
			this.score = score;
		}
	}
	private static class DummyDistribution extends ContinuousRealDistribution {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private double lowValue = Double.NaN;
		private double highValue = Double.NaN;
		@Override
		public double inverseCumulativeProbability(double p) {
			if(p==0.15) {
				if(Double.isNaN(lowValue))
					lowValue = super.inverseCumulativeProbability(p);
				return lowValue;
			}
			else if(p==0.85) {
				if(Double.isNaN(highValue))
					highValue = super.inverseCumulativeProbability(p);
				return highValue;
			}
			else
				return super.inverseCumulativeProbability(p);
		}
	}
	public static void main2(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0]);
		Set<String> actChains = new HashSet<>();
		Set<String> actChainsNoFlex = new HashSet<>();
		Set<String> actChainsNoFlexSimple = new HashSet<>();
		for(Household household:households.values())
			for(Person person:household.getPersons().values()) {
				String chain = "", chainNoFlex = "", chainNoFlexSimple = "";
				String prevPurpose = "", prevPurposeNoFlex = "", prevPurposeNoFlexSimple = "";
				if(person.isStartHome()) {
					prevPurpose = Purpose.HOME.text;
					prevPurposeNoFlex = Purpose.HOME.text;
					prevPurposeNoFlexSimple = "h";
					chain += prevPurpose.substring(0, 1);
					chainNoFlex += prevPurposeNoFlex.substring(0, 1);
					chainNoFlexSimple += prevPurposeNoFlexSimple;
				}
				for(Trip trip:person.getTrips().values()) {
					if(true) {
						prevPurpose = trip.getPurpose();
						String let = trip.getPurpose().substring(0, 1);
						if(trip.getPurpose().equals(Purpose.WORK_FLEX.text))
							let = "f";
						if(trip.getPurpose().equals(Purpose.EAT.text))
							let = "t";
						if(trip.getPurpose().equals(Purpose.SOCIAL.text))
							let = "c";
						if(trip.getPurpose().equals(Purpose.ERRANDS.text))
							let = "z";
						if(trip.getPurpose().equals(Purpose.RELIGION.text))
							let = "l";
						chain+=let;
					}
					if(trip.getPurpose().equals(Purpose.HOME.text)) {
						if(!prevPurposeNoFlex.equals(trip.getPurpose())) {
							prevPurposeNoFlex = trip.getPurpose();
							chainNoFlex += trip.getPurpose().substring(0, 1);
						}
						if(!prevPurposeNoFlexSimple.equals("h")) {
							prevPurposeNoFlexSimple = "h";
							chainNoFlexSimple += trip.getPurpose().substring(0, 1);
						}
					}
					else if(trip.getPurpose().equals(Purpose.WORK.text)||trip.getPurpose().equals(Purpose.WORK_FLEX.text)||trip.getPurpose().equals(Purpose.EDU.text)||trip.getPurpose().equals(Purpose.DRIVE.text)) {
						if(!prevPurposeNoFlex.equals(trip.getPurpose())) {
							prevPurposeNoFlex = trip.getPurpose();
							String let = trip.getPurpose().substring(0, 1);
							if(trip.getPurpose().equals(Purpose.WORK_FLEX.text))
								let = "f";
							chainNoFlex += let;
						}
						if(!prevPurposeNoFlexSimple.equals("w")) {
							prevPurposeNoFlexSimple = "w";
							chainNoFlexSimple += "w";
						}
					}
					
				}
				actChains.add(chain);
				actChainsNoFlex.add(chainNoFlex);
				actChainsNoFlexSimple.add(chainNoFlexSimple);
			}
		System.out.println(actChains.size());
		System.out.println(actChainsNoFlex.size());
		System.out.println(actChainsNoFlexSimple.size());
	}
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0]);
		IncomeEstimation.init();
		IncomeEstimation.setIncome(households);
		for(Household household:households.values())
			household.setRoles();
		int numPeople = 0;
		for(Household household:households.values())
			numPeople += household.getPersons().size();
		double numSample = numPeople*FRAC_SAMPLE;
		double numTest = numPeople - numSample;
		Map<String, Person> peopleSample = new HashMap<>();
		Map<String, Person> peopleTest = new HashMap<>();
		for(Household household:households.values()) {
			if(Math.random()<numSample/(numSample+numTest) && !household.getId().equals("400011AO")) {
				peopleSample.putAll(household.getPersons());
				numSample-=household.getPersons().size();
			}
			else {
				peopleTest.putAll(household.getPersons());
				numTest-=household.getPersons().size();
			}
		}
		/*Map<String, Map<String, Double>> ttMap = new HashMap<String, Map<String, Double>>();
		BufferedReader reader = new BufferedReader(new FileReader("./data/pairs.txt"));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(" ");
			Map<String, Double> tts = ttMap.get(parts[0]);
			if(tts==null) {
				tts = new HashMap<String, Double>();
				ttMap.put(parts[0], tts);
			}
			tts.put(parts[1], new Double(parts[2]));
			line = reader.readLine();
		}
		reader.close();
		System.out.println("TTs done");*/
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.loadConfig("./input/config-01.xml"));
		new MatsimNetworkReader(scenario).readFile("C:/Users/sergioo/workspace2/playgrounds/sergioo/input/network/network100.xml.gz");
		new TransitScheduleReader(scenario).readFile("C:/Users/sergioo/workspace2/playgrounds/sergioo/input/transit/transitSchedule.xml");
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		NetworkImpl net = (NetworkImpl) NetworkUtils.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		Set<String> noHomes = new HashSet<>();
		Map<String, LocationInfo> homes = new HashMap<>();
		Map<String, Map<String, LocationInfo>> locations = new HashMap<>();
		for(String type:IMPORTANT_TYPES)
			locations.put(type, new HashMap<String, LocationInfo>());
		Map<LocationInfo, Map<LocationInfo, PlaceToLocation>> homesToLocations = new HashMap<>();
		Map<String, LocationInfo> allLocations = new HashMap<>();
		for(Person person:peopleSample.values()) {
			String home = null;
			if(person.isStartHome())
				home = person.getTrips().get(person.getTrips().firstKey()).getStartPostalCode();
			else
				for(Trip trip:person.getTrips().values())
					if(trip.getPurpose().equals(Purpose.HOME.text))
						home = trip.getEndPostalCode();
			if(home!=null) {
				LocationInfo homeI = homes.get(home);
				if(homeI==null) {
					homeI = new LocationInfo(home, net);
					homes.put(home, homeI);
				}
				for(Trip trip:person.getTrips().values()) {
					if(IMPORTANT_TYPES.contains(trip.getPlaceType())) {
						String loc = trip.getEndPostalCode();
						Map<String, LocationInfo> locType = locations.get(trip.getPlaceType());
						LocationInfo locI = locType.get(loc);
						if(locI==null) {
							locI = new LocationInfo(loc, net);
							locType.put(loc, locI);
							allLocations.put(loc, locI);
						}
						locI.types.add(trip.getPlaceType());
						locI.purposes.add(trip.getPurpose());
						Map<LocationInfo, PlaceToLocation> locsHome = homesToLocations.get(homeI);
						if(locsHome==null) {
							locsHome = new HashMap<>();
							homesToLocations.put(homeI, locsHome);
						}
						PlaceToLocation homeToLocation = locsHome.get(locI);
						if(homeToLocation==null) {
							homeToLocation = new PlaceToLocation(homeI, locI);
							locsHome.put(locI, homeToLocation);
						}
						homeToLocation.types.add(trip.getPlaceType());
					}
				}
			}
			else
				noHomes.add(person.getId());
		}
		for(String noHome:noHomes)
			peopleSample.remove(noHome);
		Set<String> noHomesT = new HashSet<>();
		Map<String, Set<String>> personPlacesT = new HashMap<>();
		Map<String, LocationInfo> placesT = new HashMap<>();
		for(Person person:peopleTest.values()) {
			Set<String> places = new HashSet<>();
			boolean doesImportantType = false;
			if(person.isStartHome())
				places.add(person.getTrips().get(person.getTrips().firstKey()).getStartPostalCode());
			for(Trip trip:person.getTrips().values())
				if(trip.getPurpose().equals(Purpose.HOME.text)||trip.getPurpose().equals(Purpose.WORK.text)||trip.getPurpose().equals(Purpose.WORK_FLEX.text)||trip.getPurpose().equals(Purpose.EDU.text))
					places.add(trip.getEndPostalCode());
			for(Trip trip:person.getTrips().values())
				if(FLEX_ATIVITIES.contains(trip.getPurpose()))
					doesImportantType = true;
			if(places.size()>0 && doesImportantType) {
				personPlacesT.put(person.getId(), places);
				for(String place:places) {
					LocationInfo placeI = placesT.get(place);
					if(placeI==null) {
						placeI = new LocationInfo(place, net);
						placesT.put(place, placeI);
					}
				}
				for(Trip trip:person.getTrips().values()) {
					if(IMPORTANT_TYPES.contains(trip.getPlaceType())) {
						String loc = trip.getEndPostalCode();
						Map<String, LocationInfo> locType = locations.get(trip.getPlaceType());
						LocationInfo locI = locType.get(loc);
						if(locI==null) {
							locI = new LocationInfo(loc, net);
							locType.put(loc, locI);
							allLocations.put(loc, locI);
						}
						locI.types.add(trip.getPlaceType());
						locI.purposes.add(trip.getPurpose());
					}
				}
			}
			else if(places==null)
				noHomesT.add(person.getId());
		}
		for(String noHome:noHomesT)
			peopleTest.remove(noHome);
		Map<String, TypePlaceInfo> typesPlaceInfo = new HashMap<>();
		for(String type:IMPORTANT_TYPES)
			typesPlaceInfo.put(type, new TypePlaceInfo(type));
		Map<String, AbstractRealDistribution> totalDurations = new HashMap<>();
		for(String type:FLEX_ATIVITIES)
			totalDurations.put(type, new DummyDistribution());
		Map<String, AbstractRealDistribution> durations = new HashMap<>();
		for(String type:FLEX_ATIVITIES)
			durations.put(type, new DummyDistribution());
		for(Person person:peopleSample.values()) {
			Trip lastTrip = person.getTrips().get(person.getTrips().lastKey());
			int prevTime = 0;
			if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) == person.isStartHome())
				prevTime = getSeconds(lastTrip.getEndTime())-24*3600;
			String prevAct = "";
			if(person.isStartHome())
				prevAct=Purpose.HOME.text;
			Map<String, Double> sDurations = new HashMap<>();
			for(String type:FLEX_ATIVITIES)
				sDurations.put(type, 0.0);
			for(Trip trip:person.getTrips().values()) {
				String purpose = trip.getPurpose();
				if(FLEX_ATIVITIES.contains(purpose) && IMPORTANT_TYPES.contains(trip.getPlaceType())) {
					TypePlaceInfo typePlaceInfo = typesPlaceInfo.get(trip.getPlaceType());
					double[] times = typePlaceInfo.purposes.get(purpose);
					if(times == null) {
						times = new double[2];
						times[0] = Double.MAX_VALUE;
						times[1] = -Double.MAX_VALUE;
						typePlaceInfo.purposes.put(purpose, times);
					}
					int seconds = getSeconds(trip.getEndTime());
					if(seconds<times[0])
						times[0] = seconds;
					if(seconds>times[1])
						times[1] = seconds;
				}
				if(FLEX_ATIVITIES.contains(prevAct)) {
					double duration = (double) (getSeconds(trip.getStartTime())-prevTime);
					if(duration>0) {
						sDurations.put(prevAct, sDurations.get(prevAct)+1);
						((ContinuousRealDistribution)durations.get(prevAct)).addValue(duration);
					}
				}
				prevAct = purpose;
				prevTime = getSeconds(trip.getEndTime());
			}
			for(String type:FLEX_ATIVITIES)
				if(sDurations.get(type)>0)
					((ContinuousRealDistribution)totalDurations.get(type)).addValue(sDurations.get(type));
		}
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		WaitTimeCalculator waitTimeCalculator = new WaitTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig());
		StopStopTimeCalculator stopStopTimeCalculator = new StopStopTimeCalculator(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		/*EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(travelTimeCalculator);
		eventsManager.addHandler(waitTimeCalculator);
		eventsManager.addHandler(stopStopTimeCalculator);
		new EventsReaderXMLv1(eventsManager).parse("C:/Users/sergioo/workspace2/playgrounds/sergioo/input/events/150.events.xml.gz");*/
		TravelDisutility disutilityFunction = (new TravelTimeAndDistanceBasedTravelDisutilityFactory()).createTravelDisutility(travelTimeCalculator.getLinkTravelTimes(), scenario.getConfig().planCalcScore());
		TransitRouterWSImplFactory factory = new TransitRouterWSImplFactory(scenario, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		TransitRouter transitRouter = factory.get();
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(scenario.getNetwork(), disutilityFunction, travelTimeCalculator.getLinkTravelTimes(), preProcessDijkstra);
		Map<LocationInfo, Map<LocationInfo, PlaceToLocation>> placesToAllLocations = new HashMap<>();
		long time = System.currentTimeMillis();
		for(Entry<LocationInfo, Map<LocationInfo, PlaceToLocation>> homeToLocations:homesToLocations.entrySet()) {
			Set<Node> nodes = new HashSet<>();
			for(LocationInfo loc:homeToLocations.getValue().keySet())
				nodes.add(loc.node);
			Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(homeToLocations.getKey().node, nodes, 8*3600, null, null);
			for(Entry<LocationInfo, PlaceToLocation> loc:homeToLocations.getValue().entrySet()) {
				Path path = paths.get(loc.getKey().node.getId());
				double networkDistance = 0;
				for(Link link:path.links)
					networkDistance += link.getLength();
				loc.getValue().networkDistance = networkDistance;
				loc.getValue().travelTime = path.travelTime;
			}
		}
		System.out.println("After Sample: "+(System.currentTimeMillis()-time)/60000.0);
		time = System.currentTimeMillis();
		for(LocationInfo home:placesT.values()) {
			Set<Node> nodes = new HashSet<>();
			for(LocationInfo loc:allLocations.values())
				nodes.add(loc.node);
			Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(home.node, nodes, 8*3600, null, null);
			Map<LocationInfo, PlaceToLocation> allLocsHome = new HashMap<>();
			for(LocationInfo loc:allLocations.values()) {
				PlaceToLocation homeToLocation = new PlaceToLocation(home, loc);
				Path path = paths.get(loc.node.getId());
				double networkDistance = 0;
				for(Link link:path.links)
					networkDistance += link.getLength();
				homeToLocation.networkDistance = networkDistance;
				homeToLocation.travelTime = path.travelTime;
				allLocsHome.put(loc, homeToLocation);
			}
			placesToAllLocations.put(home, allLocsHome);
		}
		System.out.println("After Test: "+(System.currentTimeMillis()-time)/60000.0);
		Map<String, Collection<Double>> eDisDistributions = new HashMap<>();
		for(String type:IMPORTANT_TYPES)
			eDisDistributions.put(type, new ArrayList<Double>());
		Map<String, Collection<Double>> nDisDistributions = new HashMap<>();
		for(String type:IMPORTANT_TYPES)
			nDisDistributions.put(type, new ArrayList<Double>());
		Map<String, Collection<Double>> ttDistributions = new HashMap<>();
		for(String type:IMPORTANT_TYPES)
			ttDistributions.put(type, new ArrayList<Double>());
		for(Map<LocationInfo, PlaceToLocation> homeToLocations:homesToLocations.values())
			for(PlaceToLocation homeToLocation:homeToLocations.values())
				for(String type:homeToLocation.types) {
					eDisDistributions.get(type).add(homeToLocation.euclideanDistance);
					nDisDistributions.get(type).add(homeToLocation.networkDistance);
					ttDistributions.get(type).add(homeToLocation.travelTime);
				}
		Map<String, RealDistribution> distributions = new HashMap<>(); 
		for(String type:IMPORTANT_TYPES)
			distributions.put(type, new ContinuousRealDistribution(ttDistributions.get(type)));
		int k=0;
		//PrintWriter writer = new PrintWriter("./data/hits/tts.txt");
		long maxCTime = Long.MIN_VALUE;
		for(Entry<String, Set<String>> personPlaces:personPlacesT.entrySet()) {
			long cTime = -System.currentTimeMillis();
			System.out.println(k+++"/"+personPlacesT.size());
			Person person = peopleTest.get(personPlaces.getKey());
			Set<String> placesA = personPlaces.getValue();
			Set<Map<LocationInfo, PlaceToLocation>> maps = new HashSet<>();
			for(String placeA:placesA) {
				LocationInfo place = placesT.get(placeA);
				maps.add(placesToAllLocations.get(place));
			}
			Set<LocationInfo> knownPlaces = getKnownPlacesRandomType(person, maps, locations, distributions);
			ActivityFacilities  facilities = FacilitiesUtils.createActivityFacilities();
			PlacesConnoisseur placeConnoisseur = new PlacesConnoisseur(); 
			for(LocationInfo knownPlace:knownPlaces) {
				Id<ActivityFacility> id = Id.create(knownPlace.location, ActivityFacility.class);
				if(facilities.getFacilities().get(id)==null)
					((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, knownPlace.coord);
				for(String purpose:knownPlace.purposes) {
					double min = Double.MAX_VALUE;
					double max = Double.MIN_VALUE;
					for(String type:knownPlace.types) {
						double[] times = typesPlaceInfo.get(type).purposes.get(purpose);
						if(times!=null) {
							if(times[0]<min)
								min = times[0];
							if(times[1]>max)
								max = times[1];
						}
					}
					if(min<Double.MAX_VALUE && max>Double.MIN_VALUE && min<max)
						placeConnoisseur.addKnownPlace(id, min, max, purpose);
				}
			}
			/*Id<ActivityFacility> id = Id.create(homeA, ActivityFacility.class);
			((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, home.coord);
			placeConnoisseur.addKnownPlace(id, 0, 48*3600, Trip.Purpose.HOME.text);
			knownPlaces.add(home);*/
			List<Tuple<String, Tuple<Double, Double>>> previousActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
			List<Tuple<String, Tuple<Double, Double>>> followingActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
			Trip lastTrip = person.getTrips().get(person.getTrips().lastKey());
			int prevTime = 0;
			if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) == person.isStartHome())
				prevTime = getSeconds(lastTrip.getEndTime())-24*3600;
			String prevAct = "";
			String prevType = "";
			if(person.isStartHome())
				prevAct=Purpose.HOME.text;
			boolean previous = true, following = false;
			String origin = "", destination = "";
			modes.clear();
			modes.add("pt");
			for(Trip trip:person.getTrips().values()) {
				for(Stage stage:trip.getStages().values())
					if(stage instanceof MotorDriverStage)
						modes.add("car");
				if(previous && !prevAct.isEmpty()) {
					previousActivities.add(new Tuple<>(prevAct,new Tuple<>((double)getSeconds(trip.getStartTime()), (double)getSeconds(trip.getStartTime())-prevTime)));
					if(FLEX_ATIVITIES.contains(trip.getPurpose())) {
						previous = false;
						origin = trip.getStartPostalCode();
						Id<ActivityFacility> id = Id.create(origin, ActivityFacility.class);
						if(IMPORTANT_TYPES.contains(prevType))
							for(Entry<String, double[]> typePlaceInfo:typesPlaceInfo.get(prevType).purposes.entrySet())
								if(locations.get(prevType).get(origin)!=null)
									placeConnoisseur.addKnownPlace(id, typePlaceInfo.getValue()[0], typePlaceInfo.getValue()[1], typePlaceInfo.getKey());
						placeConnoisseur.addKnownPlace(id, 0, 48*3600, prevAct);
						LocationInfo origLoc = new LocationInfo(origin, net);
						knownPlaces.add(origLoc);
						if(facilities.getFacilities().get(id)==null)
							((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, origLoc.coord);
					}
				}
				else if(following) {
					if(!prevAct.isEmpty())
						followingActivities.add(new Tuple<>(prevAct,new Tuple<>((double)getSeconds(trip.getStartTime()), (double)getSeconds(trip.getStartTime())-prevTime)));
				}
				else if(!previous && !FLEX_ATIVITIES.contains(trip.getPurpose())) {
					following = true;
					destination = trip.getEndPostalCode();
					Id<ActivityFacility> id = Id.create(destination, ActivityFacility.class);
					if(IMPORTANT_TYPES.contains(trip.getPlaceType()))
						for(Entry<String, double[]> typePlaceInfo:typesPlaceInfo.get(trip.getPlaceType()).purposes.entrySet())
							if(locations.get(trip.getPlaceType()).get(destination)!=null)
								placeConnoisseur.addKnownPlace(id, typePlaceInfo.getValue()[0], typePlaceInfo.getValue()[1], typePlaceInfo.getKey());
					placeConnoisseur.addKnownPlace(id, 0, 48*3600, trip.getPurpose());
					LocationInfo destLoc = new LocationInfo(destination, net);
					destLoc.types.add(trip.getPlaceType());
					knownPlaces.add(destLoc);
					if(facilities.getFacilities().get(id)==null)
						((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, destLoc.coord);
				}
				prevAct = trip.getPurpose();
				prevType = trip.getPlaceType();
				int prevTimeP = getSeconds(trip.getEndTime());
				if(prevTimeP<prevTime)
					prevTimeP+=24*3600;
				prevTime = prevTimeP;
			}
			if(following) {
				if(!prevAct.isEmpty()) {
					boolean sameAct = lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) == person.isStartHome();
					double endTime = (sameAct?(double)getSeconds(person.getTrips().get(person.getTrips().firstKey()).getStartTime()):0)+24*3600;
					followingActivities.add(new Tuple<>(prevAct ,new Tuple<>(endTime, endTime-prevTime)));
				}
			}
			for(LocationInfo locationInfo:knownPlaces) {
				Set<Node> nodes = new HashSet<>();
				for(LocationInfo locationInfoO:knownPlaces)
					if(locationInfo!=locationInfoO) {
						nodes.add(locationInfoO.node);
						Path path = ((TransitRouterVariableImpl)transitRouter).calcPathRoute(locationInfo.coord, locationInfoO.coord, 8*3600, null);
						//writer.println(locationInfo.location+" "+locationInfoO.location+" pt "+path.travelTime);
						for(Time.Period period:Time.Period.values())
							placeConnoisseur.addKnownTravelTime(Id.create(locationInfo.location, ActivityFacility.class), Id.create(locationInfoO.location, ActivityFacility.class), "pt", period.getMiddleTime(), path.travelTime);
					}
				Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(locationInfo.node, nodes, 8*3600, null, null);
				for(LocationInfo locationInfoO:knownPlaces)
					if(locationInfo!=locationInfoO) {
						double travelTime = paths.get(locationInfoO.node.getId()).travelTime;
						//writer.println(locationInfo.location+" "+locationInfoO.location+" car "+travelTime);
						for(Time.Period period:Time.Period.values())
							placeConnoisseur.addKnownTravelTime(Id.create(locationInfo.location, ActivityFacility.class), Id.create(locationInfoO.location, ActivityFacility.class), "car", period.getMiddleTime(), travelTime);
					}
			}
			if(previousActivities.size()>0 && followingActivities.size()>0) {
				Agenda agenda = getAgenda(person, totalDurations, durations);
				System.out.println(person.getId()+": "+origin+"-->"+destination+"("+(followingActivities.get(0).getSecond().getFirst()-followingActivities.get(0).getSecond().getSecond()-previousActivities.get(previousActivities.size()-1).getSecond().getFirst())+")");
				List<SchedulingLink> path = new SchedulingNetwork().createNetwork(new CurrentTime(), facilities, Id.create(origin, ActivityFacility.class), Id.create(destination, ActivityFacility.class), 15*60, modes, placeConnoisseur, agenda, previousActivities, followingActivities, new MobsimStatus());
				if(path==null) {
					System.out.println("ppppppppppppppppppppppppppppppppppppppppppppp");
					//path = new SchedulingNetwork().createNetwork(new CurrentTime(), facilities, Id.create(origin, ActivityFacility.class), Id.create(destination, ActivityFacility.class), 15*60, modes, placeConnoisseur, agenda, previousActivities, followingActivities, new MobsimStatus());
				}
				else {
					System.out.println(path);
					/*if((followingActivities.get(0).getSecond().getFirst()-followingActivities.get(0).getSecond().getSecond()-previousActivities.get(previousActivities.size()-1).getSecond().getFirst())>5000 && path.size()==1)
						path = new SchedulingNetwork().createNetwork(new CurrentTime(), facilities, Id.create(origin, ActivityFacility.class), Id.create(destination, ActivityFacility.class), 15*60, modes, placeConnoisseur, agenda, previousActivities, followingActivities, new MobsimStatus());*/
				}
			}
			cTime+=System.currentTimeMillis();
			if(maxCTime<cTime)
				maxCTime = cTime;
		}
		System.out.println(maxCTime);
		//writer.close();
	}
	private static Agenda getAgenda(Person person, Map<String, AbstractRealDistribution> totalDurations, Map<String, AbstractRealDistribution> durations) {
		Agenda agenda = new Agenda();
		for(String act:FLEX_ATIVITIES)
			if(Math.random()>0.5)
				agenda.addElement(act, totalDurations.get(act), durations.get(act));
		return agenda;
	}
	private static int getSeconds(Date time) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(time);
		return cal.get(Calendar.HOUR_OF_DAY)*3600+cal.get(Calendar.MINUTE)*60+cal.get(Calendar.SECOND);
	}
	private static Set<LocationInfo> getKnownPlacesRandom(Person person, Map<String, Map<String, LocationInfo>> locations, Map<String, Collection<Double>> distributions) {
		int[] nums = new int[IMPORTANT_TYPES.size()];
		for(int i=0; i<nums.length; i++)
			nums[i] = NUM_BUILDINGS/nums.length;
		Set<LocationInfo> locs = new HashSet<>();
		for(int i=0; i<NUM_BUILDINGS; i++) {
			Map<String, LocationInfo> map = locations.get(IMPORTANT_TYPES.toArray()[(int)(Math.random()*IMPORTANT_TYPES.size())]);
			Object[] keys = map.keySet().toArray();
			locs.add(map.get(keys[(int)(Math.random()*locations.size())]));
		}
		return locs;
	}
	private static Set<LocationInfo> getKnownPlacesRandomType(Person person, Set<Map<LocationInfo, PlaceToLocation>> maps, Map<String, Map<String, LocationInfo>> locations, Map<String, RealDistribution> distributions) {
		double[] rands = new double[IMPORTANT_TYPES.size()];
		double sum = 0;
		for(int i=0; i<rands.length; i++) {
			rands[i] = Math.random();
			sum += rands[i];
		}
		Map<String, Integer> nums = new HashMap<>();
		int i=0;
		for(String type:IMPORTANT_TYPES)
			nums.put(type, (int)Math.round(rands[i++]*NUM_BUILDINGS/sum));
		Map<String, Map<LocationInfo, Set<Double>>> scoresLoc = new HashMap<>();
		for(Map<LocationInfo, PlaceToLocation> map:maps)
			for(Entry<LocationInfo, PlaceToLocation> locationInfo:map.entrySet()) {
				for(String type:locationInfo.getKey().types) {
					Map<LocationInfo, Set<Double>> mapLocations = scoresLoc.get(type);
					if(mapLocations==null) {
						mapLocations = new HashMap<>();
						scoresLoc.put(type, mapLocations);
					}
					Set<Double> scores = mapLocations.get(locationInfo.getKey());
					if(scores==null) {
						scores = new HashSet<>();
						mapLocations.put(locationInfo.getKey(), scores);
					}
					scores.add(distributions.get(type).density(locationInfo.getValue().travelTime));
				}
			}
		Map<String, List<LocationScore>> scores = new HashMap<>();
		for(String type:IMPORTANT_TYPES)
			scores.put(type, new ArrayList<LocationScore>());
		for(Entry<String, Map<LocationInfo, Set<Double>>> scoresMap:scoresLoc.entrySet()) {
			List<LocationScore> list = scores.get(scoresMap.getKey());
			for(Entry<LocationInfo, Set<Double>> scoresSet:scoresMap.getValue().entrySet()) {
				double finalScore = 0;
				for(Double score:scoresSet.getValue())
					finalScore+=score;
				finalScore/=scoresSet.getValue().size();
				i=0;
				for(;i<list.size() && list.get(i).score>finalScore; i++);
				list.add(i, new LocationScore(scoresSet.getKey(), finalScore));
				if(list.size()>nums.get(scoresMap.getKey()))
					list.remove(list.size()-1);
			}
		}
		Set<LocationInfo> locs = new HashSet<>();
		for(String type:IMPORTANT_TYPES)
			for(LocationScore locationScore:scores.get(type))
				if(locationScore!=null)
					locs.add(locationScore.location);
		return locs;
	}
	
	public static void main1(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		reader.readLine();
		List<double[]> nums = new ArrayList<>();
		List<Boolean> choices = new ArrayList<>();
		String line =  reader.readLine();
		while(line!=null) {
			String[] parts = line.split("\t");
			double[] num = new double[parts.length-2];
			for(int i=0; i<num.length; i++)
				num[i] = Double.parseDouble(parts[i]);
			nums.add(num);
			choices.add(parts[parts.length-1].equals("1"));
			line =  reader.readLine();
		}
		reader.close();
		int numCorrect = 0;
		for(int i=(int) (nums.size()*0.8); i<nums.size(); i++)
			if(isShop(nums.get(i))==choices.get(i))
				numCorrect++;
		System.out.println(numCorrect/(nums.size()*0.2));
	}
	
	private static boolean isShop(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_SHOP_HIGH = values[4];
		double ACC_EAT_LOW = values[5];
		double ACC_EAT_HIGH = values[6];
		double ACC_SHOP_LOW = values[7];
		double HOME_TIME = values[8];
		double WORK_TIME = values[9];
		double GENDER = values[10];
		double CAR_AVAIL = values[11];
		double CHINESE = values[12];
		double INDIAN = values[13];
		double MALAY = values[14];
		double MAIN = values[15];
		double PARTNER = values[16];
		double YOUNGER = values[17];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_37 = (AGE < 37)?AGE_SC:0;
		double AGE_MORE_78 = (AGE >= 78)?AGE_SC:0;
		double I_A = (INCOME == 0)?1:0;
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_2500 = (MAIN_INCOME < 2500)?MI_SC:0;
		double MI_MORE_2500 = (MAIN_INCOME >= 2500)?MI_SC:0;
		double HS_SC = HOUSEHOLD_SIZE / 9;
		double ACC_A_SC = ACC_SHOP_HIGH / 5000;
		double ACC_B_SC = ACC_SHOP_LOW / 150;
		double ACC_C_SC = ACC_EAT_HIGH / 2000;
		double ACC_D_SC = ACC_EAT_LOW / 200;
		double HT_SC = HOME_TIME / 150000;
		double WT_SC = WORK_TIME / 150000;
		double WT_A = (WORK_TIME == 0)?1:0;
		double ACC_A = 2.32;
		double ACC_B = -10.7;
		double ACC_C = 7.84;
		double ACC_D = 6.16;
		double AGE_A = 0.801;
		double AGE_B = -0.468;
		double CA = 0.0328;
		double CHI = 0.339;
		double GEND = 0.945;
		double HS = 0.396;
		double HT = 2.9;
		double INC = -2.58;
		double INC_0 = 0.488;
		double IND = 0.325;
		double K = -8.22;
		double MAL = 0.35;
		double MI_A = -1.6;
		double MI_B = -0.149;
		double MN = -0.207;
		double PA = -0.168;
		double WT = -3.74;
		double WT_0 = 1.94;
		double YO = -0.853;
		double uYes = K * one
				+ AGE_A * AGE_LESS_37
				+ AGE_B * AGE_MORE_78
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI_A * MI_LESS_2500
				+ MI_B * MI_MORE_2500
				+ HS * HS_SC
				+ ACC_A * ACC_A_SC
				+ ACC_B * ACC_B_SC
				+ ACC_C * ACC_C_SC
				+ ACC_D * ACC_D_SC
				+ HT * HT_SC
				+ WT_0 * WT_A
				+ WT * WT_SC
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		double pYes = 1/(Math.exp(-uYes)+1);
		return Math.random()<pYes;
	}
	
	private static boolean isEat(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_SHOP_HIGH = values[4];
		double ACC_EAT_LOW = values[5];
		double ACC_EAT_HIGH = values[6];
		double ACC_SHOP_LOW = values[7];
		double ACC_HOME_OTHER = values[8];
		double HOME_TIME = values[9];
		double WORK_TIME = values[10];
		double GENDER = values[11];
		double CAR_AVAIL = values[12];
		double CHINESE = values[13];
		double INDIAN = values[14];
		double MALAY = values[15];
		double MAIN = values[16];
		double PARTNER = values[17];
		double YOUNGER = values[18];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_39 = (AGE < 39?1:0) * AGE_SC;
		double AGE_MORE_39 = (AGE >= 39?1:0) * AGE_SC;
		double I_A = (INCOME == 0)?1:0;
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_2500 = (MAIN_INCOME < 2500?1:0) * MI_SC;
		double MI_MORE_2500 = (MAIN_INCOME >= 2500?1:0) * MI_SC;
		double HS_SC = HOUSEHOLD_SIZE / 9;
		double ACC_A_SC = ACC_SHOP_HIGH / 100;
		double ACC_B_SC = ACC_SHOP_LOW / 10;
		double ACC_C_SC = ACC_EAT_HIGH / 50;
		double ACC_D_SC = ACC_EAT_LOW / 10;
		double ACC_D_SC_SQR = ACC_D_SC * ACC_D_SC;
		double ACC_E_SC = ACC_HOME_OTHER / 10;
		double HT_SC = (HOME_TIME < 70000?1:0) * HOME_TIME / 150000;
		double WT_SC = WORK_TIME / 150000;
		double WT_A = (WORK_TIME == 0)?1:0;
		double ACC_A = 1.58;
		double ACC_B = -1.46;
		double ACC_C = 4.15;
		double ACC_D = 5.05;
		double ACC_D_SQR = -3.93;
		double ACC_E = 0.0471;
		double AGE_A = 1.43;
		double AGE_B = 0.814;
		double CA = 0.285;
		double CHI = 0.156;
		double GEND = -0.206;
		double HS = 1.08;
		double HT = -1.9;
		double INC = -0.632;
		double INC_0 = 0.449;
		double IND = -0.0996;
		double K = -4.08;
		double MAL = -0.0698;
		double MI_A = -1.88;
		double MI_B = 0.141;
		double MN = 0.394;
		double PA = 0.106;
		double WT = -4.82;
		double WT_0 = -0.535;
		double YO = 0.144;
		double uYes = K * one
				+ AGE_A * AGE_LESS_39
				+ AGE_B * AGE_MORE_39
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI_A * MI_LESS_2500
				+ MI_B * MI_MORE_2500
				+ HS * HS_SC
				+ ACC_A * ACC_A_SC
				+ ACC_B * ACC_B_SC
				+ ACC_C * ACC_C_SC
				+ ACC_D * ACC_D_SC
				+ ACC_D_SQR * ACC_D_SC_SQR
				+ ACC_E * ACC_E_SC
				+ HT * HT_SC
				+ WT_0 * WT_A
				+ WT * WT_SC
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		double pYes = 1/(Math.exp(-uYes)+1);
		return Math.random()<pYes;
	}

}
