package playground.sergioo.hits2012Scheduling.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
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
import java.util.Random;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import others.sergioo.util.probability.ContinuousRealDistribution;
import playground.sergioo.accessibility2013.MultiDestinationDijkstra;
import playground.sergioo.hits2012.HitsReader;
import playground.sergioo.hits2012.Household;
import playground.sergioo.hits2012.Location;
import playground.sergioo.hits2012.Location.DetailedType;
import playground.sergioo.hits2012.Person.Role;
import playground.sergioo.hits2012.Person;
import playground.sergioo.hits2012.Trip;
import playground.sergioo.hits2012.Trip.PlaceType;
import playground.sergioo.hits2012.Trip.Purpose;
import playground.sergioo.hits2012Scheduling.IncomeEstimation;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterVariableImpl;
import playground.sergioo.singapore2012.transitRouterVariable.TransitRouterWSImplFactory;
import playground.sergioo.singapore2012.transitRouterVariable.stopStopTimes.StopStopTimeCalculator;
import playground.sergioo.singapore2012.transitRouterVariable.waitTimes.WaitTimeCalculator;

public class T2 {

	private static final double FRAC_SAMPLE = 0.8;
	private static CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
	private static Set<String> IMPORTANT_TYPES = new HashSet<>(Arrays.asList(new String[]{PlaceType.SHOP.text, PlaceType.EAT.text,
			PlaceType.CIVIC.text, PlaceType.HOME_OTHER.text, PlaceType.PARK.text, PlaceType.REC.text}));
	private static Set<PlaceType> IMPORTANT_PTYPES = new HashSet<>(Arrays.asList(new PlaceType[]{PlaceType.SHOP, PlaceType.EAT,
			PlaceType.CIVIC, PlaceType.HOME_OTHER, PlaceType.PARK, PlaceType.REC/*, PlaceType.FUN, PlaceType.FINANTIAL*/}));
	private static List<String> FLEX_ATIVITIES = new ArrayList<>(Arrays.asList(new String[]{Trip.Purpose.EAT.text,
			Trip.Purpose.ERRANDS.text, Trip.Purpose.REC.text, Trip.Purpose.SHOP.text, Trip.Purpose.SOCIAL.text}));
	private static int NUM_BUILDINGS = 15;
	private static Random randomG;

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
			this.euclideanDistance = CoordUtils.calcEuclideanDistance(home.coord, location.coord);
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
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException, ClassNotFoundException {
		randomG = new Random(1234567895);
		Map<String, Household> households = HitsReader.readHits(args[0]);
		IncomeEstimation.init();
		IncomeEstimation.setIncome(households);
		for(Household household:households.values())
			household.setRoles();
		Map<PlaceType, Map<Object, Integer>> dists = new HashMap<PlaceType, Map<Object, Integer>>();
		for(PlaceType type:IMPORTANT_PTYPES) {
			Map<Object, Integer> map = new TreeMap<Object, Integer>();
			for(Location location:Household.LOCATIONS.values()) {
				Object key = location.getTypes().get(type.text);
				if(key!=null) {
					Integer num = map.get(key);
					if(num==null)
						num = 0;
					map.put(key, num+1);
				}
			}
			dists.put(type, map);
		}
		for(Location location:Household.LOCATIONS.values())
			location.setDetailedTypes(dists);
		int numPeople = 0;
		for(Household household:households.values())
			numPeople += household.getPersons().size();
		double numSample = numPeople*FRAC_SAMPLE;
		double numTest = numPeople - numSample;
		Map<String, Person> peopleSample = new HashMap<>();
		Map<String, Tuple<Household, Person>> peopleTest = new HashMap<>();
		for(Household household:households.values()) {
			if(randomG.nextDouble()<numSample/(numSample+numTest) && !household.getId().equals("400011AO")) {
				peopleSample.putAll(household.getPersons());
				numSample-=household.getPersons().size();
			}
			else {
				for(Person person:household.getPersons().values())
					peopleTest.put(person.getId(), new Tuple<>(household, person));
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
		new MatsimNetworkReader(scenario.getNetwork()).readFile("C:/Users/sergioo/workspace2/playgrounds/sergioo/input/network/network100.xml.gz");
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
						LocationInfo locI = allLocations.get(loc);
						if(locI==null) {
							locI = new LocationInfo(loc, net);
							allLocations.put(loc, locI);
						}
						locType.put(loc, locI);
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
		for(Tuple<Household, Person> personT:peopleTest.values()) {
			Person person = personT.getSecond();
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
						LocationInfo locI = allLocations.get(loc);
						if(locI==null) {
							locI = new LocationInfo(loc, net);
							allLocations.put(loc, locI);
						}
						locType.put(loc, locI);
						locI.types.add(trip.getPlaceType());
						locI.purposes.add(trip.getPurpose());
					}
				}
			}
			else if(places.isEmpty())
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
		TravelDisutility disutilityFunction = (new OnlyTimeDependentTravelDisutilityFactory()).createTravelDisutility(travelTimeCalculator.getLinkTravelTimes());
		TransitRouterWSImplFactory factory = new TransitRouterWSImplFactory(scenario, waitTimeCalculator.getWaitTimes(), stopStopTimeCalculator.getStopStopTimes());
		TransitRouter transitRouter = factory.get();
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(scenario.getNetwork(), disutilityFunction, travelTimeCalculator.getLinkTravelTimes(), preProcessDijkstra);
		Map<LocationInfo, Map<LocationInfo, PlaceToLocation>> placesToAllLocations = new HashMap<>();
		long time = System.currentTimeMillis();
		ObjectInputStream fis = new ObjectInputStream(new FileInputStream("./data/hits/tt1.dat"));		
		Map<String, Map<String, Double[]>> timesDistances = (Map<String, Map<String, Double[]>>) fis.readObject();
		for(Entry<LocationInfo, Map<LocationInfo, PlaceToLocation>> homeToLocations:homesToLocations.entrySet()) {
			/*Set<Node> nodes = new HashSet<>();
			for(LocationInfo loc:homeToLocations.getValue().keySet())
				nodes.add(loc.node);
			Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(homeToLocations.getKey().node, nodes, 8*3600, null, null);*/
			Map<String, Double[]> timeDistance = timesDistances.get(homeToLocations.getKey().node.getId().toString());
			/*if(timeDistance==null) {
				timeDistance = new HashMap<>();
				timesDistances.put(homeToLocations.getKey().node.getId().toString(), timeDistance);
			}*/
			for(Entry<LocationInfo, PlaceToLocation> loc:homeToLocations.getValue().entrySet()) {
				/*Path path = paths.get(loc.getKey().node.getId());
				double networkDistance = 0;
				for(Link link:path.links)
					networkDistance += link.getLength();*/
				loc.getValue().networkDistance = timeDistance.get(loc.getKey().node.getId().toString())[0];
				loc.getValue().travelTime = timeDistance.get(loc.getKey().node.getId().toString())[1];
				//timeDistance.put(loc.getKey().node.getId().toString(), new Double[]{networkDistance, path.travelTime, path.travelCost});
			}
		}
		System.out.println("After Sample: "+(System.currentTimeMillis()-time)/60000.0);
		time = System.currentTimeMillis();
		Map<String, Map<String, Double[]>> timesDistances2 = (Map<String, Map<String, Double[]>>) fis.readObject();
		for(LocationInfo home:placesT.values()) {
			/*Set<Node> nodes = new HashSet<>();
			for(LocationInfo loc:allLocations.values())
				nodes.add(loc.node);
			Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(home.node, nodes, 8*3600, null, null);*/
			Map<LocationInfo, PlaceToLocation> allLocsHome = new HashMap<>();
			Map<String, Double[]> timeDistance = timesDistances2.get(home.node.getId().toString());
			/*if(timeDistance==null) {
				timeDistance = new HashMap<>();
				timesDistances2.put(home.node.getId().toString(), timeDistance);
			}*/
			for(LocationInfo loc:allLocations.values()) {
				PlaceToLocation homeToLocation = new PlaceToLocation(home, loc);
				/*Path path = paths.get(loc.node.getId());
				double networkDistance = 0;
				for(Link link:path.links)
					networkDistance += link.getLength();*/
				homeToLocation.networkDistance = timeDistance.get(loc.node.getId().toString())[0];
				homeToLocation.travelTime = timeDistance.get(loc.node.getId().toString())[1];
				allLocsHome.put(loc, homeToLocation);
				//timeDistance.put(loc.node.getId().toString(), new Double[]{networkDistance, path.travelTime, path.travelCost});
			}
			placesToAllLocations.put(home, allLocsHome);
		}
		fis.close();
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
		/*PrintWriter writer = new PrintWriter("./data/hits/ttDistr.txt");
		for(Entry<String, RealDistribution> distribution:distributions.entrySet())
			for(Double num:((ContinuousRealDistribution)distribution.getValue()).getValues())
				writer.println(distribution.getKey()+","+num);
		writer.close();
		writer = new PrintWriter("./data/hits/durDistr.txt");
		for(Entry<String, AbstractRealDistribution> distribution:durations.entrySet())
			for(Double num:((ContinuousRealDistribution)distribution.getValue()).getValues())
				writer.println(distribution.getKey()+","+num);
		writer.close();*/
		/**
		###########################################
		START
		###########################################
		*/
		int k=0;
		long maxCTime = Long.MIN_VALUE;
		Map<String, Map<String, Map<DetailedType, Double>>> accs = loadAccs();
		PrintWriter writerTT = new PrintWriter("./data/hits/tTimes.txt");
		PrintWriter writerKP = new PrintWriter("./data/hits/kPlaces.txt");
		/*PrintWriter writerP = new PrintWriter("./data/hits/resP.txt");
		PrintWriter writerT = new PrintWriter("./data/hits/resT.txt");
		SortedMap<String, Boolean> wentType = new TreeMap<>();*/
		for(Entry<String, Set<String>> personPlaces:personPlacesT.entrySet()) {
			System.out.println(k+++"/"+personPlacesT.size());
			Tuple<Household, Person> pH = peopleTest.get(personPlaces.getKey());
			Person person = pH.getSecond();
			Set<String> placesA = personPlaces.getValue();
			Set<Map<LocationInfo, PlaceToLocation>> maps = new HashSet<>();
			PrintWriter writer=null;
			if(placesA.size()>1)
				writer = new PrintWriter("./data/hits/testPerson2.csv");
			int kkk=0;
			for(String placeA:placesA) {
				LocationInfo loc = placesT.get(placeA);
				maps.add(placesToAllLocations.get(loc));
				if(placesA.size()>1)
					writer.println(loc.coord.getX()+","+loc.coord.getY()+","+kkk);
			}
			Set<LocationInfo>[] knownPlacesA = getKnownPlacesTypes(person, pH.getFirst(), placesA, accs, allLocations, maps, distributions);
			knownPlacesA[0] = getKnownPlacesRandom(person, locations);
			knownPlacesA[1] = getKnownPlacesRandomTypes(person, allLocations, maps, distributions);
			if(placesA.size()>1) {
				kkk++;
				for(Set<LocationInfo> locs:knownPlacesA) {
					for(LocationInfo loc:locs)
						writer.println(loc.coord.getX()+","+loc.coord.getY()+","+kkk);
					kkk++;
				}
				writer.close();
			}
			System.out.println();
			/*ActivityFacilities[]  facilitiesA = new ActivityFacilities[]{
					FacilitiesUtils.createActivityFacilities(),
					FacilitiesUtils.createActivityFacilities(),
					FacilitiesUtils.createActivityFacilities(),
					FacilitiesUtils.createActivityFacilities()
			};
			PlacesConnoisseur[] placeConnoisseurA = new PlacesConnoisseur[]{
					new PlacesConnoisseur(), 
					new PlacesConnoisseur(),
					new PlacesConnoisseur(),
					new PlacesConnoisseur()
			};
			for(int i=0; i<knownPlacesA.length; i++) {
				Set<LocationInfo> knownPlaces = knownPlacesA[i];
				ActivityFacilities facilities = facilitiesA[i];
				PlacesConnoisseur placeConnoisseur = placeConnoisseurA[i];
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
			}
			/*Id<ActivityFacility> id = Id.create(homeA, ActivityFacility.class);
			((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, home.coord);
			placeConnoisseur.addKnownPlace(id, 0, 48*3600, Trip.Purpose.HOME.text);
			knownPlaces.add(home);*/
			/*List<Tuple<String, Tuple<Double, Double>>> previousActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
			List<Tuple<String, Tuple<Double, Double>>> followingActivities = new ArrayList<Tuple<String, Tuple<Double, Double>>>();
			Trip lastTrip = person.getTrips().get(person.getTrips().lastKey());
			int prevTime = 0;
			if(lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) == person.isStartHome())
				prevTime = getSeconds(lastTrip.getEndTime())-24*3600;
			String prevType = "";*/
			String prevAct = "";
			if(person.isStartHome())
				prevAct=Purpose.HOME.text;
			boolean previous = true, following = false;
			String origin = "", destination = "";
			/*modes.clear();
			modes.add("pt");
			int prevTTime = -1, numFlex = 0;
			String prevLoc = "";*/
			/*for(String t:FLEX_ATIVITIES)
				wentType.put(t, false);*/
			for(Trip trip:person.getTrips().values()) {
				/*for(Stage stage:trip.getStages().values())
					if(stage instanceof MotorDriverStage)
						modes.add("car");*/
				if(previous && !prevAct.isEmpty()) {
					//previousActivities.add(new Tuple<>(prevAct,new Tuple<>((double)getSeconds(trip.getStartTime()), (double)getSeconds(trip.getStartTime())-prevTime)));
					if(FLEX_ATIVITIES.contains(trip.getPurpose())) {
						previous = false;
						origin = trip.getStartPostalCode();
						/*Id<ActivityFacility> id = Id.create(origin, ActivityFacility.class);
						if(IMPORTANT_TYPES.contains(prevType))
							for(Entry<String, double[]> typePlaceInfo:typesPlaceInfo.get(prevType).purposes.entrySet())
								if(locations.get(prevType).get(origin)!=null)
									for(PlacesConnoisseur placeConnoisseur:placeConnoisseurA)
										placeConnoisseur.addKnownPlace(id, typePlaceInfo.getValue()[0], typePlaceInfo.getValue()[1], typePlaceInfo.getKey());
						for(PlacesConnoisseur placeConnoisseur:placeConnoisseurA)
							placeConnoisseur.addKnownPlace(id, 0, 48*3600, prevAct);*/
						LocationInfo origLoc = new LocationInfo(origin, net);
						for(int i=0; i<knownPlacesA.length; i++) {
							Set<LocationInfo> knownPlaces = knownPlacesA[i];
							//ActivityFacilities facilities = facilitiesA[i];
							knownPlaces.add(origLoc);
							/*if(facilities.getFacilities().get(id)==null)
								((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, origLoc.coord);*/
						}
					}
				}
				else if(following) {
					/*if(!prevAct.isEmpty())
						followingActivities.add(new Tuple<>(prevAct,new Tuple<>((double)getSeconds(trip.getStartTime()), (double)getSeconds(trip.getStartTime())-prevTime)));*/
				}
				else if(!previous && !FLEX_ATIVITIES.contains(trip.getPurpose())) {
					following = true;
					destination = trip.getEndPostalCode();
					/*Id<ActivityFacility> id = Id.create(destination, ActivityFacility.class);
					if(IMPORTANT_TYPES.contains(trip.getPlaceType()))
						for(Entry<String, double[]> typePlaceInfo:typesPlaceInfo.get(trip.getPlaceType()).purposes.entrySet())
							if(locations.get(trip.getPlaceType()).get(destination)!=null)
								for(PlacesConnoisseur placeConnoisseur:placeConnoisseurA)
									placeConnoisseur.addKnownPlace(id, typePlaceInfo.getValue()[0], typePlaceInfo.getValue()[1], typePlaceInfo.getKey());
					for(PlacesConnoisseur placeConnoisseur:placeConnoisseurA)
						placeConnoisseur.addKnownPlace(id, 0, 48*3600, trip.getPurpose());*/
					LocationInfo destLoc = new LocationInfo(destination, net);
					destLoc.types.add(trip.getPlaceType());
					for(int i=0; i<knownPlacesA.length; i++) {
						Set<LocationInfo> knownPlaces = knownPlacesA[i];
						//ActivityFacilities facilities = facilitiesA[i];
						knownPlaces.add(destLoc);
						/*if(facilities.getFacilities().get(id)==null)
							((ActivityFacilitiesImpl)facilities).createAndAddFacility(id, destLoc.coord);*/
					}
				}
				if(FLEX_ATIVITIES.contains(prevAct)) {
					/*writerT.println(person.getId()+",-1,-1,"+prevAct+","+getSeconds(trip.getStartTime())+","+(getSeconds(trip.getStartTime())-prevTime)+","+trip.getStartPostalCode()+","+prevTTime+","+prevLoc+","+prevType+"#");
					wentType.put(prevAct, true);
					numFlex++;*/
				}
				prevAct = trip.getPurpose();
				/*prevType = trip.getPlaceType();
				int prevTimeP = getSeconds(trip.getEndTime());
				if(prevTimeP<prevTime)
					prevTimeP+=24*3600;
				prevTime = prevTimeP;
				prevTTime = getSeconds(trip.getEndTime())-getSeconds(trip.getStartTime());
				prevLoc = trip.getStartPostalCode();*/
			}
			/*String typesChain = "";
			for(Entry<String, Boolean> wentE:wentType.entrySet())
				typesChain+=wentE.getValue()?"1,":"0,";
			String placesChain = "";
			for(String place:placesA)
				placesChain+=place+"#";
			writerP.println(person.getId()+",-1,-1,"+typesChain+numFlex+","+person.getAgeInterval().getCenter()+","+placesChain+",0");
			if(following) {
				if(!prevAct.isEmpty()) {
					boolean sameAct = lastTrip.getPurpose().equals(Trip.Purpose.HOME.text) == person.isStartHome();
					double endTime = (sameAct?(double)getSeconds(person.getTrips().get(person.getTrips().firstKey()).getStartTime()):0)+24*3600;
					followingActivities.add(new Tuple<>(prevAct ,new Tuple<>(endTime, endTime-prevTime)));
				}
			}*/
			for(int i=0; i<knownPlacesA.length; i++) {
				Set<LocationInfo> knownPlaces = knownPlacesA[i];
				//PlacesConnoisseur placeConnoisseur = placeConnoisseurA[i];
				for(LocationInfo locationInfo:knownPlaces) {
					writerKP.println(person.getId()+","+i+","+locationInfo.location);
					Set<Node> nodes = new HashSet<>();
					for(LocationInfo locationInfoO:knownPlaces)
						if(locationInfo!=locationInfoO) {
							nodes.add(locationInfoO.node);
							Path path = ((TransitRouterVariableImpl)transitRouter).calcPathRoute(locationInfo.coord, locationInfoO.coord, 8*3600, null);
							writerTT.println(person.getId()+","+i+","+locationInfo.location+","+locationInfoO.location+",pt,"+path.travelTime);
							/*for(Time.Period period:Time.Period.values())
								placeConnoisseur.addKnownTravelTime(Id.create(locationInfo.location, ActivityFacility.class), Id.create(locationInfoO.location, ActivityFacility.class), "pt", period.getMiddleTime(), path.travelTime);*/
						}
					Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(locationInfo.node, nodes, 8*3600, null, null);
					for(LocationInfo locationInfoO:knownPlaces)
						if(locationInfo!=locationInfoO) {
							double travelTime = paths.get(locationInfoO.node.getId()).travelTime;
							writerTT.println(person.getId()+","+i+","+locationInfo.location+","+locationInfoO.location+",car,"+travelTime);
							/*for(Time.Period period:Time.Period.values())
								placeConnoisseur.addKnownTravelTime(Id.create(locationInfo.location, ActivityFacility.class), Id.create(locationInfoO.location, ActivityFacility.class), "car", period.getMiddleTime(), travelTime);*/
						}
				}
			}
			writerTT.flush();
			writerKP.flush();
			/*if(previousActivities.size()>0 && followingActivities.size()>0) {
				double durat = (followingActivities.get(0).getSecond().getFirst()-followingActivities.get(0).getSecond().getSecond()-previousActivities.get(previousActivities.size()-1).getSecond().getFirst());
				System.out.println(person.getId()+": "+origin+"-->"+destination+"("+durat+")");
				Agenda[] agendas = getAgenda(person, pH.getFirst(), new HashSet<>(Arrays.asList(new String[]{origin, destination})), accs, totalDurations, durations, (int) Math.round(durat/7200));
				agendas[0] = getRandomAgenda(totalDurations, durations);
				for(int i=0; i<knownPlacesA.length; i++) {
					ActivityFacilities facilities = facilitiesA[i];
					PlacesConnoisseur placeConnoisseur = placeConnoisseurA[i];
					int j=0;
					for(Agenda agenda:agendas) {
						long timeS = -System.currentTimeMillis();
						List<SchedulingLink> path = new SchedulingNetwork().createNetwork(new CurrentTime(), facilities, Id.create(origin, ActivityFacility.class), Id.create(destination, ActivityFacility.class), 15*60, modes, placeConnoisseur, agenda, previousActivities, followingActivities, new MobsimStatus());
						timeS+=System.currentTimeMillis();
						if(path==null) {
							System.out.println("ppppppppppppppppppppppppppppppppppppppppppppp");
							//path = new SchedulingNetwork().createNetwork(new CurrentTime(), facilities, Id.create(origin, ActivityFacility.class), Id.create(destination, ActivityFacility.class), 15*60, modes, placeConnoisseur, agenda, previousActivities, followingActivities, new MobsimStatus());
						}
						else {
							SchedulingLink prevLink = null;
							for(String t:FLEX_ATIVITIES)
								wentType.put(t, false);
							numFlex = 0;
							for(SchedulingLink link:path)
								if(link instanceof ActivitySchedulingLink) {
									ActivitySchedulingLink aLink = (ActivitySchedulingLink)link;
									if(FLEX_ATIVITIES.contains(aLink.getActivityType())) {
										String locId = aLink.getFromNode().getId().toString().split("\\(")[0];
										LocationInfo locationInfo = allLocations.get(locId);
										typesChain = "";
										for(String type:locationInfo.types)
											typesChain+=type+"#";
										writerT.println(person.getId()+","+i+","+j+","+aLink.getActivityType()+","+((SchedulingNode)aLink.getToNode()).getTime()+","+aLink.getDuration()+","+locId+","+(int)(prevLink==null?0:prevLink.getDuration())+","+(prevLink==null?0:prevLink.getFromNode().getId().toString().split("\\(")[0])+","+typesChain);
										wentType.put(aLink.getActivityType(), true);
										numFlex++;
									}
								}
								else
									prevLink = link;
							typesChain = "";
							for(Entry<String, Boolean> wentE:wentType.entrySet())
								typesChain+=wentE.getValue()?"1,":"0,";
							placesChain = "";
							for(String place:placesA)
								placesChain+=place+"#";
							writerP.println(person.getId()+","+i+","+j+","+typesChain+numFlex+","+person.getAgeInterval().getCenter()+","+placesChain+","+timeS);
							System.out.println(path);
							/*if((followingActivities.get(0).getSecond().getFirst()-followingActivities.get(0).getSecond().getSecond()-previousActivities.get(previousActivities.size()-1).getSecond().getFirst())>5000 && path.size()==1)
								path = new SchedulingNetwork().createNetwork(new CurrentTime(), facilities, Id.create(origin, ActivityFacility.class), Id.create(destination, ActivityFacility.class), 15*60, modes, placeConnoisseur, agenda, previousActivities, followingActivities, new MobsimStatus());*/
						/*}
						j++;
					}
				}
			}
			cTime+=System.currentTimeMillis();
			maxCTime += cTime;
			writerP.flush();
			writerT.flush();*/
		}
		/*writerP.close();
		writerT.close();*/
		writerKP.close();
		writerTT.close();
		System.out.println(maxCTime/(1000*k));
		//writer.close();
	}
	private static Map<String, Map<String, Map<DetailedType, Double>>> loadAccs() throws IOException {
		Map<String, Map<String, Map<DetailedType, Double>>> accs = new HashMap<>();
		BufferedReader reader = new BufferedReader(new FileReader("./data/hits/accsN.txt"));
		String line = reader.readLine();
		while(line!=null) {
			String[] parts = line.split(",");
			Map<String, Map<DetailedType, Double>> mapPurpose = accs.get(parts[0]);
			if(mapPurpose == null) {
				mapPurpose = new HashMap<>();
				accs.put(parts[0], mapPurpose);
			}
			Map<DetailedType, Double> mapPlace = mapPurpose.get(parts[1]);
			if(mapPlace == null) {
				mapPlace = new HashMap<>();
				mapPurpose.put(parts[1], mapPlace);
			}
			mapPlace.put(DetailedType.valueOf(parts[2]), Double.parseDouble(parts[3]));
			line = reader.readLine();
		}
		reader.close();
		return accs;
	}
	private static int getSeconds(Date time) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(time);
		return cal.get(Calendar.HOUR_OF_DAY)*3600+cal.get(Calendar.MINUTE)*60+cal.get(Calendar.SECOND);
	}
	private static Set<LocationInfo> getKnownPlacesRandom(Person person, Map<String, Map<String, LocationInfo>> locations) {
		int[] nums = new int[IMPORTANT_TYPES.size()];
		for(int i=0; i<nums.length; i++)
			nums[i] = NUM_BUILDINGS/nums.length;
		Set<LocationInfo> locs = new HashSet<>();
		for(int i=0; i<NUM_BUILDINGS; i++) {
			Map<String, LocationInfo> map = locations.get(IMPORTANT_TYPES.toArray()[(int)(randomG.nextDouble()*IMPORTANT_TYPES.size())]);
			Object[] keys = map.keySet().toArray();
			locs.add(map.get(keys[(int)(randomG.nextDouble()*map.size())]));
		}
		return locs;
	}
	private static Set<LocationInfo> getKnownPlacesRandomType(Person person, Set<Map<LocationInfo, PlaceToLocation>> maps, Map<String, RealDistribution> distributions) {
		double[] rands = new double[IMPORTANT_TYPES.size()];
		double sum = 0;
		for(int i=0; i<rands.length; i++) {
			rands[i] = randomG.nextDouble();
			sum += rands[i];
		}
		Map<String, Integer> nums = new HashMap<>();
		int i=0;
		for(String type:IMPORTANT_TYPES)
			nums.put(type, (int)Math.round(rands[i++]*NUM_BUILDINGS/sum));
		Map<String, Map<LocationInfo, Collection<Double>>> scoresLoc = new HashMap<>();
		for(Map<LocationInfo, PlaceToLocation> map:maps)
			for(Entry<LocationInfo, PlaceToLocation> locationInfo:map.entrySet()) {
				for(String type:locationInfo.getKey().types) {
					Map<LocationInfo, Collection<Double>> mapLocations = scoresLoc.get(type);
					if(mapLocations==null) {
						mapLocations = new HashMap<>();
						scoresLoc.put(type, mapLocations);
					}
					Collection<Double> scores = mapLocations.get(locationInfo.getKey());
					if(scores==null) {
						scores = new ArrayList<>();
						mapLocations.put(locationInfo.getKey(), scores);
					}
					scores.add(distributions.get(type).density(locationInfo.getValue().travelTime));
				}
			}
		Map<String, List<LocationScore>> scores = new HashMap<>();
		for(String type:IMPORTANT_TYPES)
			scores.put(type, new ArrayList<LocationScore>());
		for(Entry<String, Map<LocationInfo, Collection<Double>>> scoresMap:scoresLoc.entrySet()) {
			List<LocationScore> list = scores.get(scoresMap.getKey());
			for(Entry<LocationInfo, Collection<Double>> scoresSet:scoresMap.getValue().entrySet()) {
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
				else
					System.out.println("??????????????????");
		return locs;
	}
	private static Set<LocationInfo> getKnownPlacesRandomTypes(Person person, Map<String, LocationInfo> allLocations, Set<Map<LocationInfo, PlaceToLocation>> maps, Map<String, RealDistribution> distributions) {
		Map<DetailedType, Double> rands = new HashMap<>();
		for(DetailedType type:Location.DetailedType.values()) {
			double rand = randomG.nextDouble();
			rands.put(type, rand);
		}
		List<LocationInfo> typeScores = new ArrayList<>();
		for(LocationInfo locationInfo:allLocations.values()) {
			double mult = 1;
			for(String type:locationInfo.types) {
				DetailedType dType = Household.LOCATIONS.get(locationInfo.location).getDetailedType(Trip.PlaceType.getPlaceType(type));
				mult*=(1-rands.get(dType));
			}
			mult = 1-mult;
			if(randomG.nextDouble()<mult)
				typeScores.add(locationInfo);
		}
		Map<LocationInfo, Map<String, Double>> distanceScoresType = new HashMap<>();
		for(Map<LocationInfo, PlaceToLocation> map:maps)
			for(Entry<LocationInfo, PlaceToLocation> locationInfo:map.entrySet()) {
				boolean is = false;
				for(LocationInfo locScore:typeScores)
					if(locScore.location.equals(locationInfo.getKey().location))
						is = true;
				if(is) {
					Map<String, Double> types = distanceScoresType.get(locationInfo.getKey());
					if(types==null) {
						types = new HashMap<>();
						distanceScoresType.put(locationInfo.getKey(), types);
					}
					for(String type:locationInfo.getKey().types) {
						Double scoreO = types.get(type);
						double score = distributions.get(type).density(locationInfo.getValue().travelTime);
						if(scoreO==null || score>scoreO)
							types.put(type, score);
					}
				}
			}
		List<LocationScore> distanceScores = new ArrayList<>();
		double sum = 0;
		for(Entry<LocationInfo, Map<String, Double>> entry:distanceScoresType.entrySet()) {
			double maxS = 0;
			for(Double score:entry.getValue().values())
				if(maxS<score)
					maxS = score;
			distanceScores.add(new LocationScore(entry.getKey(), maxS));
			sum+=maxS;
		}
		Set<LocationInfo> locs = new HashSet<>();
		for(int i=0; i<NUM_BUILDINGS; i++) {
			double rand = randomG.nextDouble()*sum;
			double sumL = 0;
			RANDOM:
			for(LocationScore locationScore:distanceScores) {
				sumL += locationScore.score;
				if(rand<sumL) {
					locs.add(locationScore.location);
					break RANDOM;
				}
			}
		}
		return locs;
	}
	private static Set<LocationInfo>[] getKnownPlacesTypes(Person person, Household household, Set<String> places, Map<String, Map<String, Map<DetailedType, Double>>> accs, Map<String, LocationInfo> allLocations, Set<Map<LocationInfo, PlaceToLocation>> maps, Map<String, RealDistribution> distributions) {
		Map<DetailedType, Double> rands = new HashMap<>();
		rands.put(DetailedType.SHOP_HIGH, goShopHigh(getValues(person, household, Trip.Purpose.EAT.text, places, accs)));
		rands.put(DetailedType.SHOP_LOW, goShopLow(getValues(person, household, Trip.Purpose.ERRANDS.text, places, accs)));
		rands.put(DetailedType.EAT_HIGH, goEatHigh(getValues(person, household, Trip.Purpose.EAT.text, places, accs)));
		rands.put(DetailedType.EAT_LOW, goEatLow(getValues(person, household, Trip.Purpose.ERRANDS.text, places, accs)));
		rands.put(DetailedType.CIVIC, goCivic(getValues(person, household, Trip.Purpose.REC.text, places, accs)));
		rands.put(DetailedType.HOME_OTHER, goHomeOther(getValues(person, household, Trip.Purpose.SHOP.text, places, accs)));
		rands.put(DetailedType.PARK_HIGH, goParkHigh(getValues(person, household, Trip.Purpose.SOCIAL.text, places, accs)));
		rands.put(DetailedType.PARK_LOW, goParkLow(getValues(person, household, Trip.Purpose.SOCIAL.text, places, accs)));
		rands.put(DetailedType.REC, goRec(getValues(person, household, Trip.Purpose.SOCIAL.text, places, accs)));
		List<LocationInfo> typeScores = new ArrayList<>();
		for(LocationInfo locationInfo:allLocations.values()) {
			double mult = 1;
			for(String type:locationInfo.types) {
				DetailedType dType = Household.LOCATIONS.get(locationInfo.location).getDetailedType(Trip.PlaceType.getPlaceType(type));
				mult*=(1-rands.get(dType));
			}
			mult = 1-mult;
			if(randomG.nextDouble()<mult)
				typeScores.add(locationInfo);
		}
		Map<LocationInfo, Map<String, Double>> ttScoresType = new HashMap<>();
		for(Map<LocationInfo, PlaceToLocation> map:maps)
			for(Entry<LocationInfo, PlaceToLocation> locationInfo:map.entrySet()) {
				boolean is = false;
				for(LocationInfo locScore:typeScores)
					if(locScore.location.equals(locationInfo.getKey().location))
						is = true;
				if(is) {
					Map<String, Double> types = ttScoresType.get(locationInfo.getKey());
					if(types==null) {
						types = new HashMap<>();
						ttScoresType.put(locationInfo.getKey(), types);
					}
					for(String type:locationInfo.getKey().types) {
						Double scoreO = types.get(type);
						double score = distributions.get(type).density(locationInfo.getValue().travelTime);
						if(scoreO==null || score>scoreO)
							types.put(type, score);
					}
				}
			}
		List<LocationScore> distanceScores = new ArrayList<>();
		double sum = 0;
		for(Entry<LocationInfo, Map<String, Double>> entry:ttScoresType.entrySet()) {
			double maxS = 0;
			for(Double score:entry.getValue().values())
				if(maxS<score)
					maxS = score;
			distanceScores.add(new LocationScore(entry.getKey(), maxS));
			sum+=maxS;
		}
		Set<LocationInfo> locs = new HashSet<>();
		for(int i=0; i<NUM_BUILDINGS; i++) {
			double rand = randomG.nextDouble()*sum;
			double sumL = 0;
			RANDOM:
			for(LocationScore locationScore:distanceScores) {
				sumL += locationScore.score;
				if(rand<sumL) {
					locs.add(locationScore.location);
					break RANDOM;
				}
			}
		}
		Set<LocationInfo> locs2 = new HashSet<>();
		for(int i=0; i<NUM_BUILDINGS; i++) {
			double max = 0;
			LocationScore maxL = null;
			for(LocationScore locationScore:distanceScores)
				if(locationScore.score>max) {
					max = locationScore.score;
					maxL = locationScore;
				}
			distanceScores.remove(maxL);
			locs2.add(maxL.location);
		}
		return new Set[]{null, null, locs, locs2};
	}
	public static void main0(String[] args) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(args[0]));
		reader.readLine();
		List<double[]> nums = new ArrayList<>();
		List<Boolean> choices = new ArrayList<>();
		String line =  reader.readLine();
		while(line!=null) {
			String[] parts = line.split("\t");
			double[] num = new double[parts.length-9];
			for(int i=0; i<num.length; i++)
				num[i] = Double.parseDouble(parts[i]);
			nums.add(num);
			choices.add(parts[parts.length-8].equals("1"));
			line =  reader.readLine();
		}
		reader.close();
		int numCorrect = 0, numTotal = 0, numYes = 0, numYesTotal = 0, numYesCorrect = 0;
		for(int i=0; i<nums.size()*0.2; i++) {
			numTotal++;
			if(choices.get(i))
				numYesTotal++;
			boolean is = randomG.nextDouble()<isShop(nums.get(i));
			if(is==choices.get(i)) {
				numCorrect++;
				if(choices.get(i))
					numYesCorrect++;
			}
			if(is)
				numYes++;
		}
		System.out.println(numCorrect+"/"+numTotal+"="+numCorrect/(double)numTotal);
		System.out.println(numYesTotal+"/"+numTotal+"="+numYesTotal/(double)numTotal);
		System.out.println(numYes+"/"+numTotal+"="+numYes/(double)numTotal);
		System.out.println(numYesCorrect+"/"+numTotal+"="+numYesCorrect/(double)numTotal);
	}
	
	private static double isShop(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_SHOP_HIGH = values[4];
		double ACC_SHOP_LOW = values[5];
		double ACC_EAT_HIGH = values[6];
		double ACC_EAT_LOW = values[7];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_29 = (AGE < 29?1:0) * AGE_SC + (AGE >= 29?1:0) *(29/100.0);
		double AGE_MORE_55 = (AGE >= 55?1:0) * (AGE < 77?1:0) * (AGE_SC - (55/100.0)) + (AGE >= 77?1:0) * (22/100.0);
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_2500 = (MAIN_INCOME < 2500?1:0) * MI_SC + (MAIN_INCOME >= 2500?1:0) *(2500/10000.0);
		double MI_MORE_2500 = (MAIN_INCOME >= 2500?1:0);
		double HS_SC = HOUSEHOLD_SIZE / 9.0;
		double ACC_A_SC = ACC_SHOP_HIGH / 5000.0;
		double ACC_B_SC = ACC_SHOP_LOW / 150.0;
		double ACC_B_L_86 = (ACC_SHOP_LOW < 86?1:0) * ACC_B_SC;
		double ACC_B_G_86 = (ACC_SHOP_LOW >= 86?1:0) * (ACC_B_SC - (86/150.0));
		double ACC_2_B_L_86 = ACC_B_L_86 * ACC_B_L_86;
		double ACC_2_B_G_86 = ACC_B_G_86 * ACC_B_G_86;
		double ACC_E_B_SC = ACC_2_B_L_86 - 2*(69/150.0) * ACC_B_L_86 + ((86/150.0)*(2*(69/150.0)-(86/150.0)) * ACC_2_B_G_86 )/((75/150.0)*((75/150.0) - 2*(24/150.0))) - ((86/150.0)*(2*(69/150.0)-(86/150.0))*2*(24/150.0) * ACC_B_G_86 )/((75/150.0)*((75/150.0) - 2*(24/150.0)))  + (86/150.0)*((86/150.0) - 2*(69/150.0));
		double ACC_C_SC = ACC_EAT_HIGH / 2000.0;
		double ACC_D_SC = ACC_EAT_LOW / 200.0;
		double HT_SC = HOME_TIME / 150000.0;
		double WT_SC = WORK_TIME / 150000.0;
		double WT_A = (WORK_TIME == 0?1:0);
		double ACC_A = 7.56;
		double ACC_B = 1.28;
		double ACC_C = 1.16;
		double ACC_D = -8.42;
		double AGE_A = 20;
		double AGE_B = 0.445;
		double CA = -0.166;
		double CHI = 0.099;
		double GEND = 0.822;
		double HS = 0.0426;
		double HT = 2.17;
		double INC = -2.18;
		double INC_0 = 0.0426;
		double IND = 0.207;
		double K = -9.79;
		double MAL = 0.184;
		double MI_A = -1.11;
		double MI_B = 0.367;
		double MN = -0.104;
		double PA = -0.155;
		double WT = -4.07;
		double WT_0 = 1.08;
		double YO = -0.269;
		double uYes = K * one
				+ AGE_A * AGE_LESS_29
				+ AGE_B * AGE_MORE_55
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI_A * MI_LESS_2500
				+ MI_B * MI_MORE_2500
				+ HS * HS_SC
				+ ACC_A * ACC_A_SC
				+ ACC_B * ACC_E_B_SC
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
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double isEat(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_SHOP_HIGH = values[4];
		double ACC_SHOP_LOW = values[5];
		double ACC_EAT_HIGH = values[6];
		double ACC_EAT_LOW = values[7];
		double ACC_HOME_OTHER = values[9];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_39 = (AGE < 39?1:0) * AGE_SC;
		double AGE_MORE_39 = (AGE >= 39?1:0) * AGE_SC;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_2500 = (MAIN_INCOME < 2500?1:0) * MI_SC;
		double MI_MORE_2500 = (MAIN_INCOME >= 2500?1:0) * MI_SC;
		double HS_SC = HOUSEHOLD_SIZE / 9.0;
		double ACC_A_SC = ACC_SHOP_HIGH / 100.0;
		double ACC_B_SC = (ACC_SHOP_LOW < 5?1:0) * (ACC_SHOP_LOW / 10.0) + (ACC_SHOP_LOW >= 5?1:0) * (5/10.0);
		double ACC_C_SC = ACC_EAT_HIGH / 50.0;
		double ACC_D_SC = (ACC_EAT_LOW < 5?1:0) * (ACC_EAT_LOW / 10.0) + (ACC_EAT_LOW >= 5?1:0) * (5/10.0);
		double ACC_E_SC = (ACC_HOME_OTHER < 6?1:0) * (ACC_HOME_OTHER / 10.0) + (ACC_HOME_OTHER >= 6?1:0) * (6/10.0);
		double HT_SC = (HOME_TIME < 70000?1:0) * HOME_TIME / 150000.0;
		double WT_SC = WORK_TIME / 150000.0;
		double WT_A = (WORK_TIME == 0?1:0);
		double ACC_A = 1.74;
		double ACC_B = -1.68;
		double ACC_C = 3.47;
		double ACC_D = 1;
		double ACC_E = 0.553;
		double AGE_A = 3.04;
		double AGE_B = 1.92;
		double CA = 0.284;
		double CHI = 0.113;
		double GEND = -0.0867;
		double HS = 1.04;
		double HT = -2.29;
		double INC = -0.256;
		double INC_0 = 0.0464;
		double IND = -0.0933;
		double K = -3.84;
		double MAL = -0.138;
		double MI_A = -1.79;
		double MI_B = -0.0902;
		double MN = 0.275;
		double PA = 0.0994;
		double WT = -4.5;
		double WT_0 = -0.743;
		double YO = 0.0592;
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
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double isErrands(double[] values) {
		double AGE_SC = values[0] / 100.0;
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_SHOP_HIGH = values[4];
		double ACC_SHOP_LOW = values[5];
		double ACC_EAT_HIGH = values[6];
		double ACC_CIVIC = values[8];
		double ACC_HOME_OTHER = values[9];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SQR = AGE_SC * AGE_SC;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = (MAIN_INCOME < 3000?1:0) * MAIN_INCOME / 10000.0 + (MAIN_INCOME >= 1000?1:0) * (3000/10000.0);
		double HS_SC = HOUSEHOLD_SIZE / 9;
		double ACC_A_SC = (ACC_SHOP_HIGH < 790?1:0) * (ACC_SHOP_HIGH / 1000.0) + (ACC_SHOP_HIGH >= 790?1:0) * (790/1000.0);
		double ACC_B_SC = (ACC_SHOP_LOW < 550?1:0) * (ACC_SHOP_LOW / 1000.0) + (ACC_SHOP_HIGH >= 550?1:0) * (550/1000.0);
		double ACC_C_SC = (ACC_EAT_HIGH < 900?1:0) * (ACC_EAT_HIGH / 600.0) + (ACC_EAT_HIGH >= 900?1:0) * (900/600.0);
		double ACC_D_SC = (ACC_HOME_OTHER < 940?1:0) * (ACC_HOME_OTHER / 1000.0) + (ACC_HOME_OTHER >= 940?1:0) * (940/1000.0);
		double ACC_E_SC = (ACC_CIVIC < 290?1:0) * (ACC_CIVIC / 300.0) + (ACC_CIVIC >= 290?1:0) * (290/300.0);
		double HT_SC = HOME_TIME / 150000.0;
		double WT_SC = WORK_TIME / 150000.0;
		double WT_A = (WORK_TIME == 0?1:0);
		double ACC_A = -55.2;
		double ACC_B = 2.04;
		double ACC_C = 217;
		double ACC_D = 27.1;
		double ACC_E = -336;
		double AGE = 1.6;
		double CA = 0.642;
		double CHI = 1.31;
		double GEND = 0.339;
		double HS = -0.278;
		double HT = -0.049;
		double INC = -0.328;
		double INC_0 = -0.522;
		double IND = 1.45;
		double K = 4.67;
		double MAL = 0.763;
		double MI_A = -0.263;
		double MN = 0.241;
		double PA = 0.297;
		double WT = -9.57;
		double WT_0 = 0.384;
		double YO = -0.638;
		double uYes = K * one
				+ AGE * AGE_SQR
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI_A * MI_SC
				+ HS * HS_SC
				+ ACC_A * ACC_A_SC
				+ ACC_B * ACC_B_SC
				+ ACC_C * ACC_C_SC
				+ ACC_D * ACC_D_SC
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
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double isRec(double[] values) {
		double AGE_SC = values[0] / 100.0;
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_CIVIC = values[8];
		double ACC_HOME_OTHER = values[9];
		double ACC_PARK_HIGH = values[10];
		double ACC_PARK_LOW = values[11];
		double ACC_REC = values[12];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = (MAIN_INCOME <= 830?1:0) * MAIN_INCOME / 10000.0 + (MAIN_INCOME >= 830?1:0)* 830 / 10000.0;
		double HS_SC = HOUSEHOLD_SIZE / 9.0;
		double ACC_A_L_SC = (ACC_REC < 118?1:0)*(ACC_REC - 108) / 20.0 + (ACC_REC >= 118?1:0)*(118 - 108) / 20.0;
		double ACC_A_G_SC = (ACC_REC >= 118?1:0)*(ACC_REC - 108) / 20.0;
		double ACC_B_SC = (ACC_HOME_OTHER - 488) / 100.0;
		double ACC_C_SC = (ACC_PARK_HIGH - 49) / 10.0;
		double ACC_D_SC = (ACC_PARK_LOW > 133?1:0)*(ACC_PARK_LOW - 130) / 10.0;
		double ACC_E_L_SC = (ACC_CIVIC < 153.7?1:0)*(ACC_CIVIC - 140) / 10.0 + (ACC_CIVIC >= 153.7?1:0)*(153.7 - 140) / 10.0;
		double ACC_E_G_SC = (ACC_CIVIC >= 153.7?1:0)*(ACC_CIVIC - 140) / 10.0;
		double HT_SC = HOME_TIME / 150000.0;
		double WT_SC = WORK_TIME / 150000.0;
		double ACC_A_G = 1.54;
		double ACC_A_L = 11.5;
		double ACC_B = 7.47;
		double ACC_C = 7.78;
		double ACC_D = -3.11;
		double ACC_E_G = -0.495;
		double ACC_E_L = -7.26;
		double AGE = 3.93;
		double CA = 0.596;
		double CHI = 1.04;
		double GEND = -0.104;
		double HS = 0.702;
		double HT = 3.74;
		double INC = 0.011;
		double INC_0 = 0.78;
		double IND = 0.826;
		double K = -10.1;
		double MAL = 0.251;
		double MI = -4.7;
		double MN = 0.0263;
		double PA = 0.0392;
		double WT = -5.66;
		double YO = 0.477;
		double uYes = K * one
				+ AGE * AGE_SC
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI * MI_SC
				+ HS * HS_SC
				+ ACC_A_L * ACC_A_L_SC
				+ ACC_A_G * ACC_A_G_SC
				+ ACC_B * ACC_B_SC
				+ ACC_C * ACC_C_SC
				+ ACC_D * ACC_D_SC
				+ ACC_E_L * ACC_E_L_SC
				+ ACC_E_G * ACC_E_G_SC
				+ HT * HT_SC
				+ WT * WT_SC
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double isSocial(double[] values) {
		double AGE_SC = values[0] / 100.0;
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_SHOP_HIGH = values[4];
		double ACC_CIVIC = values[8];
		double ACC_HOME_OTHER = values[9];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double HS_SC = HOUSEHOLD_SIZE / 9;
		double ACC_A_L_SC = (ACC_SHOP_HIGH < 89?1:0) * (ACC_SHOP_HIGH / 150.0) + (ACC_SHOP_HIGH >= 89?1:0) * (89 / 150.0);
		double ACC_A_G_SC = (ACC_SHOP_HIGH >= 89?1:0) * (ACC_SHOP_HIGH / 150.0);
		double ACC_B_L_SC = (ACC_HOME_OTHER < 104?1:0) * (ACC_HOME_OTHER / 150.0) + (ACC_HOME_OTHER >= 104?1:0) * (104 / 150.0);
		double ACC_B_G_SC = (ACC_HOME_OTHER >= 104?1:0) * (ACC_HOME_OTHER / 150.0);
		double ACC_C_SC = ACC_CIVIC / 50.0;
		double HT_SC = HOME_TIME / 150000.0;
		double WT_SC = WORK_TIME / 150000.0;
		double WT_A = (WORK_TIME == 0?1:0);
		double ACC_A_G = -0.461;
		double ACC_A_L = 2.01;
		double ACC_B_G = -0.608;
		double ACC_B_L = -2.13;
		double ACC_C = 3.5;
		double AGE = 2.28;
		double CA = 0.366;
		double CHI = -0.272;
		double GEND = -0.168;
		double HS = -1.51;
		double HT = -1.6;
		double INC = -1.65;
		double INC_0 = 0.682;
		double IND = -0.142;
		double K = -5.97;
		double MAL = -0.447;
		double MI = 0.594;
		double MN = -0.501;
		double PA = -0.365;
		double WT = -2.22;
		double WT_0 = 0.732;
		double YO = -1.26;
		double uYes = K * one
				+ AGE * AGE_SC
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI * MI_SC
				+ HS * HS_SC
				+ ACC_A_L * ACC_A_L_SC
				+ ACC_A_G * ACC_A_G_SC
				+ ACC_B_L * ACC_B_L_SC
				+ ACC_B_G * ACC_B_G_SC
				+ ACC_C * ACC_C_SC
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
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goShopHigh(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_SHOP_HIGH = values[4];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_17 = (AGE < 17?1:0) * AGE_SC + (AGE >= 17?1:0) * (17/100.0);
		double AGE_MORE_17 = (AGE >= 17?1:0) * (AGE < 49?1:0) * (AGE_SC - (17/100.0)) + (AGE >= 49?1:0) * (32/100.0);
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_7000 = (MAIN_INCOME < 7000?1:0) * MI_SC;
		double MI_MORE_7000 = (MAIN_INCOME >= 7000?1:0) * MI_SC;
		double HS_SC = (HOUSEHOLD_SIZE >= 5?1:0) * (HOUSEHOLD_SIZE - 5)/ 9.0;
		double ACC_SC = ACC_SHOP_HIGH / 5000.0;
		double ACC_L_700 = (ACC_SHOP_HIGH < 700?1:0) * ACC_SC + (ACC_SHOP_HIGH >= 700?1:0) * (700/5000.0);
		double ACC_M_700 = (ACC_SHOP_HIGH >= 700?1:0) * (ACC_SC - (700/5000.0));
		double HT_SC = HOME_TIME / 150000.0;
		double WT_SC = WORK_TIME / 150000.0;
		double WT_IS_0 = (WORK_TIME == 0?1:0);
		double WT_LESS_7 = (WORK_TIME > 0?1:0) * (WORK_TIME < 7*3600?1:0) * WT_SC;
		double WT_MORE_9 = (WORK_TIME >= 9*3600?1:0)*(WORK_TIME < 14.5*3600?1:0) * (WT_SC - (9*3600/150000.0));
		double ACC = 4.25;
		double ACC_0 = -2.71;
		double AGE_A = 11.4;
		double AGE_B = -2.91;
		double CA = 0.302;
		double CHI = -0.172;
		double GEND = 0.0533;
		double HS = -1.85;
		double HT = -2.07;
		double INC = -0.811;
		double INC_0 = 0.408;
		double IND = -0.681;
		double K = -1.69;
		double MAL = -0.464;
		double MI_A = 1.05;
		double MI_B = 0.663;
		double MN = 0.389;
		double PA = 0.112;
		double WT_0 = -0.212;
		double WT_A = -2.49;
		double WT_B = -12.5;
		double YO = 0.407;
		double uYes = K * one
				+ AGE_A * AGE_LESS_17
				+ AGE_B * AGE_MORE_17
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI_A * MI_LESS_7000
				+ MI_B * MI_MORE_7000
				+ HS * HS_SC
				+ ACC_0 * ACC_L_700
				+ ACC * ACC_M_700
				+ HT * HT_SC
				+ WT_0 * WT_IS_0
				+ WT_A * WT_LESS_7
				+ WT_B * WT_MORE_9
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goShopLow(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_SHOP_LOW = values[5];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_27 = (AGE < 27?1:0) * AGE_SC;
		double AGE_MORE_27 = (AGE >= 27?1:0) * AGE_SC;
		double AGE_SQR_SC = AGE_SC * AGE_SC;
		double I_SC = INCOME / 10000.0;
		double I_LESS_1000 = (INCOME < 1000?1:0);
		double I_MORE_1000 = (INCOME >= 1000?1:0) * I_SC;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_6000 = (MAIN_INCOME < 6000?1:0) * MI_SC + (MAIN_INCOME >= 6000?1:0) * (MAIN_INCOME < 10500?1:0) * 6000/10000.0;
		double MI_MORE_6000 = (MAIN_INCOME >= 6000?1:0) * (MAIN_INCOME < 10500?1:0) * (MI_SC - 6000/10000.0);
		double HS_SC = HOUSEHOLD_SIZE / 4.0;
		double HS_LESS_2 = (HOUSEHOLD_SIZE < 2?1:0) * HS_SC;
		double HS_MORE_2 = (HOUSEHOLD_SIZE >= 2?1:0) * HS_SC;
		double ACC_SC = ACC_SHOP_LOW / 500.0;
		double ACC_LESS_100 = (ACC_SHOP_LOW < 100?1:0) * ACC_SC;
		double ACC_LESS_170 = (ACC_SHOP_LOW < 170?1:0) * (ACC_SHOP_LOW >=100?1:0) * ACC_SC;
		double ACC_MORE_170 = (ACC_SHOP_LOW >= 170?1:0) * ACC_SC;
		double HT_SC = HOME_TIME / 150000;
		double WT_A = (WORK_TIME == 0?1:0);
		double WT_MORE_0 = (WORK_TIME > 0?1:0) * WORK_TIME / 150000;
		double ACC_A = 6.62;
		double ACC_B = 3.3;
		double ACC_C = 0.705;
		double AGE_A = 11.3;
		double AGE_B = 7.56;
		double AGE_SQR = -8.06;
		double CA = -0.387;
		double CHI = -0.19;
		double GEND = -0.359;
		double HS_A = 2.78;
		double HS_B = 0.551;
		double HT = -0.533;
		double INC = -0.0626;
		double INC_0 = -0.354;
		double IND = -0.57;
		double K = -4.84;
		double MAL = -0.219;
		double MI_A = -1.72;
		double MI_B = 5.04;
		double MN = 0.000119;
		double PA = 0.323;
		double WT = -1.02;
		double WT_0 = -0.0161;
		double YO = -0.561;
		double uYes = K * one
				+ AGE_A * AGE_LESS_27
				+ AGE_B * AGE_MORE_27
				+ AGE_SQR * AGE_SQR_SC
				+ INC_0 * I_LESS_1000
				+ INC * I_MORE_1000
				+ MI_A * MI_LESS_6000
				+ MI_B * MI_MORE_6000
				+ HS_A * HS_LESS_2
				+ HS_B * HS_MORE_2
				+ ACC_A * ACC_LESS_100
				+ ACC_B * ACC_LESS_170
				+ ACC_C * ACC_MORE_170
				+ HT * HT_SC
				+ WT_0 * WT_A
				+ WT * WT_MORE_0
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goEatHigh(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_EAT_HIGH = values[6];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_24 = (AGE < 24?1:0) * AGE_SC;
		double AGE_MORE_24 = (AGE >= 24?1:0) * (AGE < 58?1:0) * AGE_SC;
		double AGE_MORE_58 = (AGE >= 58?1:0) * AGE_SC;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_6800 = (MAIN_INCOME < 6800?1:0) * MI_SC;
		double MI_MORE_6800 = (MAIN_INCOME >= 6800?1:0) * MI_SC;
		double HS_SC = HOUSEHOLD_SIZE / 4.0;
		double HS_LESS_3 = (HOUSEHOLD_SIZE < 3?1:0) * HS_SC;
		double HS_MORE_3 = (HOUSEHOLD_SIZE >= 3?1:0) * HS_SC;
		double ACC_A = (ACC_EAT_HIGH < 300?1:0);
		double ACC_SC = (ACC_EAT_HIGH >= 300?1:0) * ACC_EAT_HIGH / 2000.0;
		double ACC_SQR_SC = ACC_SC * ACC_SC;
		double HT_SC = HOME_TIME / 150000.0;
		double HT_LESS_17 = (HOME_TIME < 63000?1:0) * HT_SC;
		double HT_MORE_17 = (HOME_TIME >= 63000?1:0) * HT_SC;
		double WT_A = (WORK_TIME == 0?1:0);
		double WT_MORE_0 = (WORK_TIME > 0?1:0) * WORK_TIME / 150000.0;
		double ACC = 26.1;
		double ACC_0 = 9.76;
		double ACC_SQR = -19.6;
		double AGE_A = -5.61;
		double AGE_B = 2.01;
		double AGE_C = 1.91;
		double CA = -0.708;
		double CHI = 0.201;
		double GEND = -0.119;
		double HS_A = 0.678;
		double HS_B = 0.585;
		double HT_A = 1.66;
		double HT_B = 2.42;
		double INC = 0.982;
		double INC_0 = -0.415;
		double IND = 0.688;
		double K = -12.7;
		double MAL = 0.404;
		double MI_A = -0.592;
		double MI_B = -1.19;
		double MN = -0.539;
		double PA = -0.0633;
		double WT = 6.63;
		double WT_0 = 1.48;
		double YO = 0.259;
		double uYes = K * one
				+ AGE_A * AGE_LESS_24
				+ AGE_B * AGE_MORE_24
				+ AGE_C * AGE_MORE_58
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI_A * MI_LESS_6800
				+ MI_B * MI_MORE_6800
				+ HS_A * HS_LESS_3
				+ HS_B * HS_MORE_3
				+ ACC_0 * ACC_A
				+ ACC * ACC_SC
				+ ACC_SQR * ACC_SQR_SC
				+ HT_A * HT_LESS_17
				+ HT_B * HT_MORE_17
				+ WT_0 * WT_A
				+ WT * WT_MORE_0
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goEatLow(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_EAT_LOW = values[7];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_45 = (AGE < 45?1:0) * AGE_SC;
		double AGE_MORE_45 = (AGE >= 45?1:0) * AGE_SC;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double HS_SC = HOUSEHOLD_SIZE / 4.0;
		double ACC_A = (ACC_EAT_LOW < 18?1:0)* ACC_EAT_LOW / 200.0;
		double ACC_SC = (ACC_EAT_LOW >= 18?1:0)* ACC_EAT_LOW / 200.0;
		double HT_SC = HOME_TIME / 150000.0;
		double HT_LESS_48000 = (HOME_TIME < 48000?1:0) * HT_SC;
		double HT_MORE_48000 = (HOME_TIME >= 48000?1:0) * HT_SC;
		double WT_A = (WORK_TIME == 0?1:0);
		double WT_MORE_0 = (WORK_TIME > 0?1:0) * WORK_TIME / 150000.0;
		double ACC = -3.03;
		double ACC_0 = 12.3;
		double AGE_A = 0.277;
		double AGE_B = -0.246;
		double CA = 0.324;
		double CHI = 0.418;
		double GEND = -0.0522;
		double HS = 0.156;
		double HT_A = 3.09;
		double HT_B = 2.46;
		double INC = 1.07;
		double INC_0 = 0.0434;
		double IND = 0.261;
		double K = -4.01;
		double MAL = 0.636;
		double MI = -0.82;
		double MN = 0.53;
		double PA = 0.263;
		double WT = 0.336;
		double WT_0 = -0.339;
		double YO = 0.177;
		double uYes = K * one
				+ AGE_A * AGE_LESS_45
				+ AGE_B * AGE_MORE_45
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI * MI_SC
				+ HS * HS_SC
				+ ACC_0 * ACC_A
				+ ACC * ACC_SC
				+ HT_A * HT_LESS_48000
				+ HT_B * HT_MORE_48000
				+ WT_0 * WT_A
				+ WT * WT_MORE_0
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goCivic(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_CIVIC = values[8];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_A = (MAIN_INCOME == 0?1:0);
		double HS_SC = HOUSEHOLD_SIZE / 4.0;
		double ACC_SC = ACC_CIVIC / 500.0;
		double HT_SC = HOME_TIME / 150000.0;
		double HT_LESS_22 = (HOME_TIME < 79000?1:0) * HT_SC;
		double HT_MORE_22 = (HOME_TIME >= 79000?1:0);
		double WT_A = (WORK_TIME == 0?1:0);
		double WT_MORE_0 = (WORK_TIME > 0?1:0) * WORK_TIME / 150000.0;
		double ACC_A = 2.35;
		double AGE_A = 2.08;
		double CA = -0.145;
		double CHI = -0.368;
		double GEND = -0.048;
		double HS_A = -0.521;
		double HT_A = 5.28;
		double HT_B = 1.49;
		double INC = 0.446;
		double INC_0 = 0.999;
		double IND = -0.134;
		double K = -7.69;
		double MAL = -0.978;
		double MI = -0.211;
		double MI_0 = -0.277;
		double MN = 0.382;
		double PA = 0.166;
		double WT = 9.2;
		double WT_0 = 0.83;
		double YO = 0.522;
		double uYes = K * one
				+ AGE_A * AGE_SC
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI_0 * MI_A
				+ MI * MI_SC
				+ HS_A * HS_SC
				+ ACC_A * ACC_SC
				+ HT_A * HT_LESS_22
				+ HT_B * HT_MORE_22
				+ WT_0 * WT_A
				+ WT * WT_MORE_0
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goHomeOther(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_HOME_OTHER = values[9];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_57 = (AGE < 57?1:0) * AGE_SC;
		double AGE_MORE_57 = (AGE >= 57?1:0) * AGE_SC;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_4600 = (MAIN_INCOME < 4700?1:0);
		double HS_SC = HOUSEHOLD_SIZE / 4.0;
		double ACC_SC = ACC_HOME_OTHER / 500.0;
		double HT_SC = HOME_TIME / 150000.0;
		double HT_LESS_16 = (HOME_TIME < 64000?1:0) * HT_SC;
		double HT_MORE_16 = (HOME_TIME >= 64000?1:0) * HT_SC;
		double WT_A = (WORK_TIME == 0?1:0);
		double WT_MORE_0 = (WORK_TIME > 0?1:0) * WORK_TIME / 150000.0;
		double ACC_A = 1.16;
		double AGE_A = -3.2;
		double AGE_B = -1.31;
		double CA = 0.0283;
		double CHI = -0.279;
		double GEND = 0.14;
		double HS = -0.881;
		double HT_A = 3.69;
		double HT_B = 1.84;
		double INC = -0.348;
		double INC_0 = -0.502;
		double IND = -0.147;
		double K = -1.56;
		double MAL = 0.145;
		double MI_A = -0.715;
		double MI_B = -0.789;
		double MN = -0.232;
		double PA = 0.146;
		double WT = 1.35;
		double WT_0 = 0.602;
		double YO = -0.246;
		double uYes = K * one
				+ AGE_A * AGE_LESS_57
				+ AGE_B * AGE_MORE_57
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI_A * MI_LESS_4600
				+ MI_B * MI_SC
				+ HS * HS_SC
				+ ACC_A * ACC_SC
				+ HT_A * HT_LESS_16
				+ HT_B * HT_MORE_16
				+ WT_0 * WT_A
				+ WT * WT_MORE_0
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goParkHigh(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_PARK_HIGH = values[10];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double AGE_LESS_24 = (AGE < 24?1:0) * AGE_SC;
		double AGE_MORE_24 = (AGE >= 24?1:0) * AGE_SC;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double HS_SC = HOUSEHOLD_SIZE / 4.0;
		double ACC_A = (ACC_PARK_HIGH < 52?1:0)* ACC_PARK_HIGH / 56.0;
		double ACC_SC = (ACC_PARK_HIGH >= 52?1:0)* ACC_PARK_HIGH / 56.0;
		double HT_SC = HOME_TIME / 150000.0;
		double WT_A = (WORK_TIME == 0?1:0);
		double WT_MORE_0 = (WORK_TIME > 0?1:0) * WORK_TIME / 150000.0;
		double ACC = 14.4;
		double ACC_0 = 15.3;
		double AGE_A = -11.8;
		double AGE_B = 3.18;
		double CA = 0.219;
		double CHI = 2.76;
		double GEND = 0.669;
		double HS = 0.633;
		double HT = 3.52;
		double INC = 3.89;
		double INC_0 = 2.65;
		double IND = 4.74;
		double K = -21;
		double MAL = 1.86;
		double MI = -2.6;
		double MN = -0.276;
		double PA = -0.442;
		double WT = -13.7;
		double WT_0 = -3.39;
		double YO = 3.27;
		double uYes = K * one
				+ AGE_A * AGE_LESS_24
				+ AGE_B * AGE_MORE_24
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI * MI_SC
				+ HS * HS_SC
				+ ACC_0 * ACC_A
				+ ACC * ACC_SC
				+ HT * HT_SC
				+ WT_0 * WT_A
				+ WT * WT_MORE_0
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goParkLow(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double I_A = (INCOME == 0?1:0);
		double I_SC = INCOME / 10000.0;
		double MI_SC = MAIN_INCOME / 10000.0;
		double HS_SC = HOUSEHOLD_SIZE / 4.0;
		double HS_LESS_4 = (HOUSEHOLD_SIZE < 4?1:0) * HS_SC;
		double HS_MORE_4 = (HOUSEHOLD_SIZE >= 4?1:0) * HS_SC;
		double HT_SC = HOME_TIME / 150000.0;
		double WT_A = (WORK_TIME == 0?1:0);
		double WT_MORE_0 = (WORK_TIME > 0?1:0) * WORK_TIME / 150000.0;
		double AGE_0 = 2.91;
		double CA = -1.57;
		double CHI = -2.49;
		double GEND = -0.494;
		double HS_A = 1.16;
		double HS_B = 0.912;
		double HT = 2.81;
		double INC = -3.63;
		double INC_0 = -0.764;
		double IND = -2.49;
		double K = 2.34;
		double MAL = -2.58;
		double MI = -0.72;
		double MN = 0.489;
		double PA = 0.469;
		double WT = -22;
		double WT_0 = -4.58;
		double YO = 0.122;
		double uYes = K * one
				+ AGE_0 * AGE_SC
				+ INC_0 * I_A
				+ INC * I_SC
				+ MI * MI_SC
				+ HS_A * HS_LESS_4
				+ HS_B * HS_MORE_4
				+ HT * HT_SC
				+ WT_0 * WT_A
				+ WT * WT_MORE_0
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	
	private static double goRec(double[] values) {
		double AGE = values[0];
		double INCOME = values[1];
		double MAIN_INCOME = values[2];
		double HOUSEHOLD_SIZE = values[3];
		double ACC_REC = values[12];
		double HOME_TIME = values[13];
		double WORK_TIME = values[14];
		double GENDER = values[15];
		double CAR_AVAIL = values[16];
		double CHINESE = values[17];
		double INDIAN = values[18];
		double MALAY = values[19];
		double MAIN = values[20];
		double PARTNER = values[21];
		double YOUNGER = values[22];
		double one = 1;
		double AGE_SC = AGE / 100.0;
		double I_SC = INCOME / 10000.0;
		double I_LESS_3500 = (INCOME < 3500?1:0) * I_SC;
		double I_MORE_3500 = (INCOME >= 3500?1:0) * I_SC;
		double I_A = (INCOME == 0?1:0);
		double MI_SC = MAIN_INCOME / 10000.0;
		double MI_LESS_1400 = (MAIN_INCOME < 1400?1:0) * MI_SC;
		double MI_LESS_3500 = (MAIN_INCOME >= 1400?1:0) * (MAIN_INCOME < 3000?1:0) * MI_SC;
		double MI_MORE_3500 = (MAIN_INCOME >= 3000?1:0) * MI_SC;
		double HS_SC = HOUSEHOLD_SIZE / 4.0;
		double HS_LESS_3 = (HOUSEHOLD_SIZE < 3?1:0) * HS_SC;
		double HS_MORE_3 = (HOUSEHOLD_SIZE >= 3?1:0) * HS_SC;
		double ACC_SC = ACC_REC / 500.0;
		double ACC_LESS_117 = (ACC_REC < 117?1:0) * ACC_SC;
		double ACC_MORE_117 = (ACC_REC >= 117?1:0) * ACC_SC;
		double HT_SC = HOME_TIME / 150000.0;
		double HT_LESS_25 = (HOME_TIME < 90000?1:0) * HT_SC;
		double HT_MORE_25 = (HOME_TIME >= 90000?1:0) * HT_SC;
		double WT_A = (WORK_TIME == 0?1:0);
		double WT_MORE_0 = (WORK_TIME > 0?1:0) * WORK_TIME / 150000.0;
		double ACC_A = 92;
		double ACC_B = 93.2;
		double AGE_A = -5.73;
		double CA = 0.358;
		double CHI = -0.846;
		double GEND = -0.488;
		double HS_A = 0.925;
		double HS_B = 0.723;
		double HT_A = -2.37;
		double HT_B = -1.35;
		double INC_0 = 2.05;
		double INC_A = 8.65;
		double INC_B = 5.44;
		double IND = -2.5;
		double K = -30.3;
		double MAL = -0.35;
		double MI_A = -6.21;
		double MI_B = -0.76;
		double MI_C = 1.15;
		double MN = -0.696;
		double PA = -0.084;
		double WT = 43.5;
		double WT_0 = 11;
		double YO = -1.98;
		double uYes = K * one
				+ AGE_A * AGE_SC
				+ INC_0 * I_A
				+ INC_A * I_LESS_3500
				+ INC_B * I_MORE_3500
				+ MI_A * MI_LESS_1400
				+ MI_B * MI_LESS_3500
				+ MI_C * MI_MORE_3500
				+ HS_A * HS_LESS_3
				+ HS_B * HS_MORE_3
				+ ACC_A * ACC_LESS_117
				+ ACC_B * ACC_MORE_117
				+ HT_A * HT_LESS_25
				+ HT_B * HT_MORE_25
				+ WT_0 * WT_A
				+ WT * WT_MORE_0
				+ GEND * GENDER
				+ CA * CAR_AVAIL
				+ CHI * CHINESE
				+ IND * INDIAN
				+ MAL * MALAY
				+ MN * MAIN
				+ PA * PARTNER
				+ YO * YOUNGER;
		return 1/(Math.exp(-uYes)+1);
	}
	private static double[] getValues(Person person, Household household, String purpose, Set<String> places, Map<String, Map<String, Map<DetailedType, Double>>> accs) {
		String prevPurpose = person.getFirstActivity();
		long time = 0, homeTime = 0, workTime = 0;
		for(Trip trip:person.getTrips().values()) {
			if(prevPurpose.equals(Trip.Purpose.HOME.text)) {
				long duration = (trip.getStartTime().getTime()-time)/1000;
				if(duration<0)
					if(duration<-15*3600)
						duration += 24*3600;
					else
						duration = -duration;
				else if(duration==0)
					duration = 60;
				homeTime+=duration;
			}
			else if(prevPurpose.equals(Trip.Purpose.WORK.text)) {
				long duration = (trip.getStartTime().getTime()-time)/1000;
				if(duration<0)
					if(duration<-15*3600)
						duration += 24*3600;
					else
						duration = -duration;
				else if(duration==0)
					duration = 60;
				workTime+=duration;
			}
			prevPurpose = trip.getPurpose();
			time = trip.getEndTime().getTime();
		}
		if(prevPurpose.equals(Trip.Purpose.HOME.text)) {
			long duration = (24*3600000-time)/1000;
			if(duration<0)
				if(duration<-15*3600)
					duration += 24*3600;
				else
					duration = -duration;
			else if(duration==0)
				duration = 60;
			homeTime+=duration;
		}
		else if(prevPurpose.equals(Trip.Purpose.WORK.text)) {
			long duration = (24*3600000-time)/1000;
			if(duration<0)
				if(duration<-15*3600)
					duration += 24*3600;
				else
					duration = -duration;
			else if(duration==0)
				duration = 60;
			workTime+=duration;
		}
		double[] values = new double[23];
		values[0] = (double) person.getAgeInterval().getCenter();
		values[1] = (double) person.getIncomeInterval().getCenter();
		values[2] = (double) person.getMainIncome(household);
		values[3] = (double) household.getPersons().size();
		int i = 4;
		for(Location.DetailedType detailedType:Location.DetailedType.values()) {
			double maxAcc = -Double.MAX_VALUE; 
			for(String zip:places) {
				double acc = accs.get(purpose).get(zip).get(detailedType);
				if(acc>maxAcc)
					maxAcc = acc;
			}
			values[i++] = maxAcc; 
		}
		values[13] = (double) homeTime;
		values[14] = (double) workTime;
		values[15] = (double) (person.getGender().equals("Male")?0:1);
		values[16] = (double) (person.hasCar()?1:0);
		values[17] = (double) (household.getEthnic().equals("Chinese")?1:0);
		values[18] = (double) (household.getEthnic().equals("Indian")?1:0);
		values[19] = (double) (household.getEthnic().equals("Malay")?1:0);
		values[20] = (double) (person.getRole().equals(Role.MAIN)?1:0);
		values[21] = (double) (person.getRole().equals(Role.PARTNER)?1:0);
		values[22] = (double) (person.getRole().equals(Role.YOUNGER)?1:0);
		return values;
	}
	
}

