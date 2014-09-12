package playground.sergioo.hits2012Scheduling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.sergioo.hits2012.HitsReader;
import playground.sergioo.hits2012.Household;
import playground.sergioo.hits2012.Location;
import playground.sergioo.hits2012.Person;
import playground.sergioo.hits2012.Location.DetailedType;
import playground.sergioo.hits2012.Person.Role;
import playground.sergioo.hits2012.Trip;
import playground.sergioo.hits2012.Trip.PlaceType;

public class BinaryPlaceTypes {
	
	private static final Integer MIN_OCURR = 10;

	private static CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
	
	private static Set<String> FIXED_ACTIVITIES = new HashSet<String>(Arrays.asList(new String[]{Trip.Purpose.HOME.text, Trip.Purpose.WORK.text, Trip.Purpose.EDU.text,
			Trip.Purpose.WORK_FLEX.text, Trip.Purpose.RELIGION.text, Trip.Purpose.P_U_D_O.text, Trip.Purpose.DRIVE.text, Trip.Purpose.MEDICAL.text}));
	private static Set<String> FLEX_ATIVITIES = new HashSet<String>(Arrays.asList(new String[]{Trip.Purpose.ACCOMP.text, Trip.Purpose.EAT.text,
			Trip.Purpose.ERRANDS.text, Trip.Purpose.REC.text, Trip.Purpose.SHOP.text, Trip.Purpose.SOCIAL.text}));
	private static Set<PlaceType> IMPORTANT_TYPES = new HashSet<PlaceType>(Arrays.asList(new PlaceType[]{PlaceType.SHOP, PlaceType.EAT,
			PlaceType.CIVIC, PlaceType.HOME_OTHER, PlaceType.PARK, PlaceType.REC}));
	
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0]);
		IncomeEstimation.init();
		IncomeEstimation.setIncome(households);
		for(Household household:households.values())
			household.setRoles();
		Map<PlaceType, Map<Object, Integer>> distributions = new HashMap<PlaceType, Map<Object, Integer>>();
		for(PlaceType type:IMPORTANT_TYPES) {
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
			distributions.put(type, map);
		}
		for(Location location:Household.LOCATIONS.values())
			location.setDetailedTypes(distributions);
		Map<String, Map<DetailedType, Integer>> typesPlaces = new HashMap<String, Map<DetailedType, Integer>>();
		for(Household household:households.values())
			for(Person person:household.getPersons().values())
				for(Trip trip:person.getTrips().values()) {
					DetailedType detailedType = Household.LOCATIONS.get(trip.getEndPostalCode()).getDetailedType(Trip.PlaceType.getPlaceType(trip.getPlaceType()));
					if(detailedType!=null) {
						Map<DetailedType, Integer> typesPurpose = typesPlaces.get(trip.getPurpose());
						if(typesPurpose==null) {
							typesPurpose = new HashMap<DetailedType, Integer>();
							typesPlaces.put(trip.getPurpose(), typesPurpose);
						}
						Integer num = typesPurpose.get(detailedType);
						if(num==null)
							num = 0;
						typesPurpose.put(detailedType, num+1);
					}
				}
		for(Entry<String, Map<DetailedType, Integer>>tPEntry:typesPlaces.entrySet()) {
			System.out.println(tPEntry.getKey());
			for(Entry<DetailedType, Integer> typePlace:tPEntry.getValue().entrySet())
				System.out.print(typePlace.getKey().name().toLowerCase()+"("+typePlace.getValue()+") | ");
			System.out.println();
		}
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile("C:/Users/sergioo/workspace2/playgrounds/sergioo/input/network/network100.xml.gz");
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		NetworkImpl net = NetworkImpl.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		Map<String, Node> nodes = new HashMap<String, Node>();
		for(String purpose:FLEX_ATIVITIES)
			for(Household household:households.values()) {
				for(Person person:household.getPersons().values())
					for(Trip trip:person.getTrips().values())
						/*if(trip.getPurpose().equals(purpose))*/ {
							Location startLocation = Household.LOCATIONS.get(trip.getStartPostalCode());
							Location endLocation = Household.LOCATIONS.get(trip.getEndPostalCode());
							if(nodes.get(startLocation.getPostalCode())==null)
								nodes.put(startLocation.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(startLocation.getCoord())));
							if(nodes.get(endLocation.getPostalCode())==null)
								nodes.put(endLocation.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(endLocation.getCoord())));
						}
			}
		Map<DetailedType, Map<String, String>> locs = new HashMap<DetailedType, Map<String, String>>();
		for(DetailedType detailedType:Location.DetailedType.values()) {
			Map<String, String> los = new HashMap<String, String>();
			locs.put(detailedType, los);
			for(Location location:Household.LOCATIONS.values())
				if(location.getDetailedTypes().contains(detailedType))
					los.put(location.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(location.getCoord())).getId().toString());
		}
		Map<String, Map<String, Double>> ttMap = new HashMap<String, Map<String, Double>>();
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
		/*TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(travelTimeCalculator);
		new EventsReaderXMLv1(eventsManager).parse("C:/Users/sergioo/workspace2/playgrounds/sergioo/input/events/150.events.xml.gz");
		TravelDisutility disutilityFunction = (new TravelTimeAndDistanceBasedTravelDisutilityFactory()).createTravelDisutility(travelTimeCalculator.getLinkTravelTimes(), scenario.getConfig().planCalcScore());
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(scenario.getNetwork(), disutilityFunction, travelTimeCalculator.getLinkTravelTimes(), preProcessDijkstra);
		PrintWriter printerP = new PrintWriter("./data/pairs.txt");
		for(String postalCode:nodes.keySet()) {
			//long t = System.currentTimeMillis();
			Set<Node> nodesPC = new HashSet<Node>();
			for(Map<String, String> los:locs.values())
				for(String loc:los.keySet())
					nodesPC.add(nodes.get(loc));
			Map<Id, Path> paths = dijkstra.calcLeastCostPath(nodes.get(postalCode), nodesPC, 8*3600, null, null);
			for(Entry<Id, Path> e:paths.entrySet())
				printerP.println(nodes.get(postalCode).getId().toString()+" "+e.getKey().toString()+" "+e.getValue().travelTime+" "+e.getValue().travelCost);
			//System.out.println(System.currentTimeMillis()-t);
		}
		printerP.close();*/
		System.out.println("TTs done");		
		Map<String, Map<DetailedType, Double>> accs = new HashMap<String, Map<DetailedType, Double>>();
		for(Entry<String, Node> nodeE:nodes.entrySet()) {
			Map<DetailedType, Double> accMap = new HashMap<Location.DetailedType, Double>();
			for(DetailedType detailedType:Location.DetailedType.values()) {
				double sum=0;
				for(String nodeId:locs.get(detailedType).values())
					sum+=Math.exp(-0.2*ttMap.get(nodeE.getValue().getId().toString()).get(nodeId));
				accMap.put(detailedType, sum);
			}
			accs.put(nodeE.getKey(), accMap);
		}
		System.out.println("Accs done");
		for(DetailedType detailedType:Location.DetailedType.values()) {
			PrintWriter printer = new PrintWriter("./data/types/"+detailedType.name().toLowerCase()+".dat");
			writeHeaderT(printer, detailedType.name());
			for(Household household:households.values()) {
				for(Person person:household.getPersons().values())
					for(Trip trip:person.getTrips().values())
						if(FLEX_ATIVITIES.contains(trip.getPurpose())) {
							Integer num = typesPlaces.get(trip.getPurpose()).get(detailedType);
							if(num!=null && num>MIN_OCURR) {
								printer.print(person.getAgeInterval().getCenter()+"\t");
								printer.print(person.getIncomeInterval().getCenter()+"\t");
								printer.print(person.getMainIncome(household)+"\t");
								printer.print(household.getPersons().size()+"\t");
								printer.print((person.getGender().equals("Male")?0:1)+"\t");
								printer.print((person.hasCar()?1:0)+"\t");
								printer.print((household.getEthnic().equals("Chinese")?1:0)+"\t");
								printer.print((household.getEthnic().equals("Indian")?1:0)+"\t");
								printer.print((household.getEthnic().equals("Malay")?1:0)+"\t");
								printer.print((person.getRole().equals(Role.MAIN)?1:0)+"\t");
								printer.print((person.getRole().equals(Role.PARTNER)?1:0)+"\t");
								printer.print((person.getRole().equals(Role.YOUNGER)?1:0)+"\t");
								printer.print(Math.max(accs.get(trip.getEndPostalCode()).get(detailedType), accs.get(trip.getStartPostalCode()).get(detailedType))+"\t");
								printer.print(trip.getFactor()+"\t");
								printer.println(detailedType.equals(Household.LOCATIONS.get(trip.getEndPostalCode()).getDetailedType(Trip.PlaceType.getPlaceType(trip.getPlaceType())))?"1":"0");
							}
						}
				}
				printer.close();
		}
		for(String purpose:FLEX_ATIVITIES) {
			Set<DetailedType> detailedTypes = new HashSet<DetailedType>();
			for(DetailedType detailedType:Location.DetailedType.values()) {
				Integer num = typesPlaces.get(purpose).get(detailedType);
				if(num!=null && num>MIN_OCURR)
					detailedTypes.add(detailedType);
			}
			PrintWriter printerB = new PrintWriter("./data/purposesB/"+purpose+".dat");
			writeHeaderPB(printerB, detailedTypes);
			PrintWriter printer = new PrintWriter("./data/purposes/"+purpose+".dat");
			writeHeaderP(printer, detailedTypes);
			for(Household household:households.values())
				for(Person person:household.getPersons().values()) {
					boolean isPurpose = false;
					String prevPurpose = person.getFirstActivity();
					String prevZip = person.getTrips().values().iterator().next().getStartPostalCode();
					long time = 0;
					for(Trip trip:person.getTrips().values()) {
						if(purpose.equals(prevPurpose)) {
							isPurpose = true;
							long duration = (trip.getStartTime().getTime()-time)/1000;
							if(duration<0)
								duration += 24*3600;
							else if(duration==0)
								duration = 60;
							printer.print(duration+"\t");
							printer.print(Math.pow(duration, 2)+"\t");
							printer.print(person.getAgeInterval().getCenter()+"\t");
							printer.print(person.getIncomeInterval().getCenter()+"\t");
							printer.print(person.getMainIncome(household)+"\t");
							printer.print(household.getPersons().size()+"\t");
							printer.print((person.getGender().equals("Male")?0:1)+"\t");
							printer.print((person.hasCar()?1:0)+"\t");
							printer.print((household.getEthnic().equals("Chinese")?1:0)+"\t");
							printer.print((household.getEthnic().equals("Indian")?1:0)+"\t");
							printer.print((household.getEthnic().equals("Malay")?1:0)+"\t");
							printer.print((person.getRole().equals(Role.MAIN)?1:0)+"\t");
							printer.print((person.getRole().equals(Role.PARTNER)?1:0)+"\t");
							printer.print((person.getRole().equals(Role.YOUNGER)?1:0)+"\t");
							for(DetailedType detailedType:detailedTypes)
								printer.print(accs.get(prevZip).get(detailedType)+"\t");
							printer.println();
						}
						prevPurpose = trip.getPurpose();
						prevZip = trip.getEndPostalCode();
						time = trip.getEndTime().getTime();
					}
					printerB.print(person.getAgeInterval().getCenter()+"\t");
					printerB.print(person.getIncomeInterval().getCenter()+"\t");
					printerB.print(person.getMainIncome(household)+"\t");
					printerB.print((person.getGender().equals("Male")?0:1)+"\t");
					printerB.print(household.getPersons().size()+"\t");
					printerB.print((person.hasCar()?1:0)+"\t");
					printerB.print((household.getEthnic().equals("Chinese")?1:0)+"\t");
					printerB.print((household.getEthnic().equals("Indian")?1:0)+"\t");
					printerB.print((household.getEthnic().equals("Malay")?1:0)+"\t");
					printerB.print((person.getRole().equals(Role.MAIN)?1:0)+"\t");
					printerB.print((person.getRole().equals(Role.PARTNER)?1:0)+"\t");
					printerB.print((person.getRole().equals(Role.YOUNGER)?1:0)+"\t");
					for(DetailedType detailedType:detailedTypes) {
						double maxAcc = 0;
						for(Trip trip:person.getTrips().values()) {
							Integer num = typesPlaces.get(trip.getPurpose()).get(detailedType);
							if(num!=null && num>MIN_OCURR) {
								double startAcc = accs.get(trip.getStartPostalCode()).get(detailedType);
								if(startAcc>maxAcc)
									maxAcc = startAcc;
								double endAcc = accs.get(trip.getEndPostalCode()).get(detailedType);
								if(endAcc>maxAcc)
									maxAcc = endAcc;
							}
						}
						printerB.print(maxAcc+"\t");
					}
					printerB.print(person.getFactor()+"\t");
					printerB.println(isPurpose?"1":"0");
				}
			printer.close();
			printerB.close();
		}
	}

	private static void writeHeaderP(PrintWriter printer, Set<DetailedType> detailedTypes) {
		printer.print("DURATION\t");
		printer.print("DURATION_SQR\t");
		printer.print("AGE\t");
		printer.print("INCOME\t");
		printer.print("MAIN_INCOME\t");
		printer.print("HOUSEHOLD_SIZE\t");
		printer.print("GENDER\t");
		printer.print("CAR_AVAIL\t");
		printer.print("CHINESE\t");
		printer.print("INDIAN\t");
		printer.print("MALAY\t");
		printer.print("MAIN\t");
		printer.print("PARTNER\t");
		printer.print("YOUNGER\t");
		for(DetailedType detailedType:detailedTypes)
			printer.print("ACC_"+detailedType.name().toUpperCase()+"\t");
		printer.println();
	}
	
	private static void writeHeaderPB(PrintWriter printer, Set<DetailedType> detailedTypes) {
		printer.print("AGE\t");
		printer.print("INCOME\t");
		printer.print("MAIN_INCOME\t");
		printer.print("HOUSEHOLD_SIZE\t");
		printer.print("CAR_AVAIL\t");
		printer.print("GENDER\t");
		printer.print("CHINESE\t");
		printer.print("INDIAN\t");
		printer.print("MALAY\t");
		printer.print("MAIN\t");
		printer.print("PARTNER\t");
		printer.print("YOUNGER\t");
		for(DetailedType detailedType:detailedTypes)
			printer.print("ACC_"+detailedType.name().toUpperCase()+"\t");
		printer.print("WEIGHT\t");
		printer.println("CHOICE");
	}

	private static void writeHeaderT(PrintWriter printer, String detailedType) {
		printer.print("AGE\t");
		printer.print("INCOME\t");
		printer.print("MAIN_INCOME\t");
		printer.print("HOUSEHOLD_SIZE\t");
		printer.print("GENDER\t");
		printer.print("CAR_AVAIL\t");
		printer.print("CHINESE\t");
		printer.print("INDIAN\t");
		printer.print("MALAY\t");
		printer.print("MAIN\t");
		printer.print("PARTNER\t");
		printer.print("YOUNGER\t");
		printer.print("ACC_"+detailedType+"\t");
		printer.print("WEIGHT\t");
		printer.println("CHOICE");
	}

}
