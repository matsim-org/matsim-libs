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
import playground.sergioo.hits2012.Person.AgeInterval;
import playground.sergioo.hits2012.Person.IncomeInterval;
import playground.sergioo.hits2012.Trip;
import playground.sergioo.hits2012.Trip.PlaceType;

public class PlaceTypes {
	
	private static CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
	
	private static Set<String> FIXED_ACTIVITIES = new HashSet<String>(Arrays.asList(new String[]{Trip.Purpose.HOME.text, Trip.Purpose.WORK.text, Trip.Purpose.EDU.text,
			Trip.Purpose.WORK_FLEX.text, Trip.Purpose.RELIGION.text, Trip.Purpose.P_U_D_O.text, Trip.Purpose.DRIVE.text, Trip.Purpose.MEDICAL.text}));
	private static Set<String> FLEX_ATIVITIES = new HashSet<String>(Arrays.asList(new String[]{Trip.Purpose.ACCOMP.text, Trip.Purpose.EAT.text,
			Trip.Purpose.ERRANDS.text, Trip.Purpose.REC.text, Trip.Purpose.SHOP.text, Trip.Purpose.SOCIAL.text}));
	private static Set<PlaceType> IMPORTANT_TYPES = new HashSet<PlaceType>(Arrays.asList(new PlaceType[]{PlaceType.SHOP, PlaceType.EAT,
			PlaceType.CIVIC, PlaceType.HOME_OTHER, PlaceType.PARK, PlaceType.REC}));
	
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0], args[1]);
		IncomeEstimation.init();
		IncomeEstimation.setIncome(households);
		Map<String, Map<String, Integer>> typesPlaces = new HashMap<String, Map<String, Integer>>();
		for(Household household:households.values())
			for(Person person:household.getPersons().values())
				for(Trip trip:person.getTrips().values()) {
					Map<String, Integer> typesPurpose = typesPlaces.get(trip.getPurpose());
					if(typesPurpose==null) {
						typesPurpose = new HashMap<String, Integer>();
						typesPlaces.put(trip.getPurpose(), typesPurpose);
					}
					Integer num = typesPurpose.get(trip.getPlaceType());
					if(num==null)
						num = 0;
					typesPurpose.put(trip.getPlaceType(), num+1);
				}
		for(Entry<String, Map<String, Integer>>tPEntry:typesPlaces.entrySet()) {
			System.out.println(tPEntry.getKey());
			for(Entry<String, Integer> typePlace:tPEntry.getValue().entrySet())
				System.out.print(typePlace.getKey()+"("+typePlace.getValue()+") | ");
			System.out.println();
		}
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
			//new BarChart(type.text, map);
		}
		for(Location location:Household.LOCATIONS.values())
			location.setDetailedTypes(distributions);
		/*Map<Set<String>, Set<String>> typeSets = new HashMap<Set<String>, Set<String>>();
		for(PlaceType type:IMPORTANT_TYPES)
			for(Location location:Household.LOCATIONS.values())
				if(location.getTypes().contains(type.text)) {
					postals.add(location.getPostalCode());
					Set<String> types = new HashSet<String>(location.getTypes());
					int home=0;
					for(String oldType:location.getTypes()) {
						if(!IMPORTANT_TYPES.contains(PlaceType.getPlaceType(oldType)) && !PlaceType.HOME.text.equals(oldType))
							types.remove(oldType);
						if(PlaceType.HOME_OTHER.text.equals(oldType)) {
							types.remove(oldType);
							types.add(PlaceType.HOME.text);
						}
					}
					Set<String> postalCodes = typeSets.get(types);
					if(postalCodes==null) {
						postalCodes = new HashSet<String>();
						typeSets.put(types, postalCodes);
					}
					postalCodes.add(location.getPostalCode());
				}
		PrintWriter printWriter = new PrintWriter(new File("./data/typeSets.csv"));
		for(Entry<Set<String>, Set<String>> entry:typeSets.entrySet()) {
			for(String placeType:entry.getKey())
				printWriter.print(placeType+"-");
			printWriter.print(",");
			for(String postalCode:entry.getValue())
				printWriter.print(postalCode+",");
			printWriter.println();
		}
		printWriter.close();*/
		/*Set<String> postals = new HashSet<String>();
		Map<Set<String>, Set<String>> purposeSets = new HashMap<Set<String>, Set<String>>();
		for(String purpose:FLEX_ATIVITIES)
			for(Location location:Household.LOCATIONS.values())
				if(location.getPurposes().contains(purpose)) {
					postals.add(location.getPostalCode());
					Set<String> purposes = new HashSet<String>(location.getPurposes());
					for(String oldPurpose:location.getPurposes())
						if(!FLEX_ATIVITIES.contains(oldPurpose) && !Purpose.HOME.text.equals(oldPurpose))
							purposes.remove(oldPurpose);
					Set<String> postalCodes = purposeSets.get(purposes);
					if(postalCodes==null) {
						postalCodes = new HashSet<String>();
						purposeSets.put(purposes, postalCodes);
					}
					postalCodes.add(location.getPostalCode());
				}
		PrintWriter printWriter = new PrintWriter(new File("./data/purposeSets.csv"));
		for(Entry<Set<String>, Set<String>> entry:purposeSets.entrySet()) {
			for(String placeType:entry.getKey())
				printWriter.print(placeType+"-");
			printWriter.print(",");
			for(String postalCode:entry.getValue())
				printWriter.print(postalCode+",");
			printWriter.println();
		}
		printWriter.close();*/
		//CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile("C:/Users/sergioo/workspace2/playgrounds/sergioo/input/network/network100.xml.gz");
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		NetworkImpl net = NetworkImpl.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		Map<String, String> nodes = new HashMap<String, String>();
		/*Map<String, Node> nodes = new HashMap<String, Node>();
		Set<String> locs = new HashSet<String>();
		for(DetailedType detailedType:Location.DetailedType.values())
			for(Location location:Household.LOCATIONS.values())
				if(location.getDetailedTypes().contains(detailedType)) {
					locs.add(location.getPostalCode());
					if(nodes.get(location.getPostalCode())==null)
						nodes.put(location.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(location.getCoord())));
				}*/
		
		for(Household household:households.values()) {
			for(Person person:household.getPersons().values())
				for(Trip trip:person.getTrips().values())
					if(FLEX_ATIVITIES.contains(trip.getPurpose())) {
						Location startLocation = Household.LOCATIONS.get(trip.getStartPostalCode());
						Location endLocation = Household.LOCATIONS.get(trip.getEndPostalCode());
						if(nodes.get(startLocation.getPostalCode())==null)
							nodes.put(startLocation.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(startLocation.getCoord())).getId().toString());
						if(nodes.get(endLocation.getPostalCode())==null)
							nodes.put(endLocation.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(endLocation.getCoord())).getId().toString());
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
		System.out.println("TTs done");		
		Map<String, Map<DetailedType, Double>> accs = new HashMap<String, Map<DetailedType, Double>>();
		for(Entry<String, String> nodeE:nodes.entrySet()) {
			Map<DetailedType, Double> accMap = new HashMap<Location.DetailedType, Double>();
			for(DetailedType detailedType:Location.DetailedType.values()) {
				double sum=0;
				for(String nodeId:locs.get(detailedType).values())
					sum+=Math.exp(-0.2*ttMap.get(nodeE.getValue()).get(nodeId));
				accMap.put(detailedType, sum);
			}
			accs.put(nodeE.getKey(), accMap);
		}
		System.out.println("Accs done");
		/*try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Location.SINGAPORE_COORDS_FILE));
			accs = (Map<String, Map<DetailedType, Double>>) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}*/
		//Set<String> postalCodes = new HashSet<String>();
		//for(String purpose:Trip.PURPOSES)
			//if(FLEX_ATIVITIES.contains(purpose)) {
				PrintWriter printer = new PrintWriter("./data/purposes/all"+/*purpose+*/".dat");
				writeHeader(printer);
				for(Household household:households.values()) {
					for(Person person:household.getPersons().values())
						for(Trip trip:person.getTrips().values())
							if(FLEX_ATIVITIES.contains(trip.getPurpose())/*trip.getPurpose().equals(purpose)*/) {
								//Location startLocation = Household.LOCATIONS.get(trip.getStartPostalCode());
								Location endLocation = Household.LOCATIONS.get(trip.getEndPostalCode());
								double sum = 0;
								for(Integer num:endLocation.getTypes().values())
									sum += num;
								/*postalCodes.add(trip.getStartPostalCode());
								if(nodes.get(startLocation.getPostalCode())==null)
									nodes.put(startLocation.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(startLocation.getCoord())).getId().toString());
								postalCodes.add(trip.getEndPostalCode());
								if(nodes.get(endLocation.getPostalCode())==null)
									nodes.put(endLocation.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(endLocation.getCoord())).getId().toString());*/
								/*for(DetailedType detailedType:endLocation.getDetailedTypes()) {*/
								DetailedType[] detailedTypes = Location.DETAILED_TYPES.get(Trip.PlaceType.getPlaceType(trip.getPlaceType()));
								if(detailedTypes!=null) {
									DetailedType detailedType = null;
									for(DetailedType detailedTypeI:endLocation.getDetailedTypes())
										for(DetailedType detailedTypeII:detailedTypes)
											if(detailedTypeI.equals(detailedTypeII))
												detailedType = detailedTypeI;
									printer.print(person.getAgeInterval().getCenter()+"\t");
									printer.print((person.getGender().equals("Male")?0:1)+"\t");
									printer.print(person.getIncomeInterval().getCenter()+"\t");
									printer.print(household.getPersons().size()+"\t");
									for(DetailedType type:Location.DetailedType.values())
										printer.print(Math.max(accs.get(trip.getEndPostalCode()).get(type), accs.get(trip.getStartPostalCode()).get(type))+"\t");
									printer.print(endLocation.getTypes().get(Location.getTypeOfDetailedType(detailedType).text)/sum+"\t");
									printer.println(detailedType.ordinal());
								}
							}
				}
				printer.close();
			//}
		/*TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());
		EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
		eventsManager.addHandler(travelTimeCalculator);
		new EventsReaderXMLv1(eventsManager).parse("C:/Users/sergioo/workspace2/playgrounds/sergioo/input/events/150.events.xml.gz");
		TravelDisutility disutilityFunction = (new TravelTimeAndDistanceBasedTravelDisutilityFactory()).createTravelDisutility(travelTimeCalculator.getLinkTravelTimes(), scenario.getConfig().planCalcScore());
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(scenario.getNetwork());
		MultiDestinationDijkstra dijkstra = new MultiDestinationDijkstra(scenario.getNetwork(), disutilityFunction, travelTimeCalculator.getLinkTravelTimes(), preProcessDijkstra);
		PrintWriter printer = new PrintWriter("./data/pairs.txt");
		for(String postalCode:postalCodes) {
			//long t = System.currentTimeMillis();
			Set<Node> nodesPC = new HashSet<Node>();
			for(String loc:locs)
				nodesPC.add(nodes.get(loc));
			Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(nodes.get(postalCode), nodesPC, 8*3600, null, null);
			for(Entry<Id<Node>, Path> e:paths.entrySet())
				printer.println(nodes.get(postalCode).getId().toString()+" "+e.getKey().toString()+" "+e.getValue().travelTime+" "+e.getValue().travelCost);
			//System.out.println(System.currentTimeMillis()-t);
		}
		printer.close();*/
		/*PrintWriter printer = new PrintWriter("./data/pairs.txt");
		for(String postalCode:postalCodes) {
			for(String loc:locs) {
				printer.println(postalCode+" "+loc+" "+links.get(postalCode)+" "+links.get(loc));
			}
		}
		printer.close();*/
		/*PrintWriter printer = new PrintWriter("./data/accLocations.dat");
		for(String postalCode:postalCodes)
			printer.println(postalCode);
		printer.close();*/
	}

	private static Integer getIncomeCateg(IncomeInterval incomeInterval) {
		if(incomeInterval.getCenter()<2000)
			return 0;
		else if(incomeInterval.getCenter()<8000)
			return 1;
		else
			return 2;
	}

	private static Integer getAgeCateg(AgeInterval ageInterval) {
		if(ageInterval.getCenter()<20)
			return 0;
		else if(ageInterval.getCenter()<60)
			return 1;
		else
			return 2;
	}

	private static void writeHeader(PrintWriter printer) {
		printer.print("AGE\t");
		printer.print("GENDER\t");
		printer.print("INCOME\t");
		printer.print("HOUSEHOLD_SIZE\t");
		for(DetailedType type:Location.DetailedType.values())
			printer.print("ACC_"+type.name().toUpperCase()+"\t");
		printer.print("WEIGHT\t");
		printer.println("CHOICE");
	}

}
