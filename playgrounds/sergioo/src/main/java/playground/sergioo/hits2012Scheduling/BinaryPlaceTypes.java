package playground.sergioo.hits2012Scheduling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import playground.sergioo.hits2012.HitsReader;
import playground.sergioo.hits2012.Household;
import playground.sergioo.hits2012.Location;
import playground.sergioo.hits2012.Person;
import playground.sergioo.hits2012.Location.DetailedType;
import playground.sergioo.hits2012.Person.Day;
import playground.sergioo.hits2012.Person.Role;
import playground.sergioo.hits2012.Trip;
import playground.sergioo.hits2012.Trip.PlaceType;

public class BinaryPlaceTypes {
	
	private static final Integer MIN_OCURR = 10;

	private static CoordinateTransformation coordinateTransformation = TransformationFactory.getCoordinateTransformation(TransformationFactory.WGS84, TransformationFactory.WGS84_UTM48N);
	
	private static Set<String> FIXED_ACTIVITIES = new HashSet<>(Arrays.asList(new String[]{Trip.Purpose.HOME.text, Trip.Purpose.WORK.text, Trip.Purpose.EDU.text,
			Trip.Purpose.WORK_FLEX.text, Trip.Purpose.RELIGION.text, Trip.Purpose.P_U_D_O.text, Trip.Purpose.DRIVE.text, Trip.Purpose.MEDICAL.text}));
	private static Set<String> FLEX_ATIVITIES = new HashSet<>(Arrays.asList(new String[]{Trip.Purpose.ACCOMP.text, Trip.Purpose.EAT.text,
			Trip.Purpose.ERRANDS.text, Trip.Purpose.REC.text, Trip.Purpose.SHOP.text, Trip.Purpose.SOCIAL.text}));
	private static Set<PlaceType> IMPORTANT_TYPES = new HashSet<>(Arrays.asList(new PlaceType[]{PlaceType.SHOP, PlaceType.EAT,
			PlaceType.CIVIC, PlaceType.HOME_OTHER, PlaceType.PARK, PlaceType.REC/*, PlaceType.FUN, PlaceType.FINANTIAL*/}));
	
	private static class Observation {
		private final String zipCode;
		private final double duration;
		private DetailedType detailedType;
		private Observation(String zipCode, double duration, DetailedType detailedType) {
			super();
			this.zipCode = zipCode;
			this.duration = duration;
			this.detailedType = detailedType;
		}
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException, ParseException {
		Map<String, Household> households = HitsReader.readHits(args[0], args[1]);
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
		NetworkImpl net = (NetworkImpl) NetworkUtils.createNetwork();
		HashSet<String> modes = new HashSet<String>();
		modes.add(TransportMode.car);
		filter.filter(net, modes);
		Map<String, Node> nodes = new HashMap<String, Node>();
		Map<String, Set<Observation>> obsMap = new HashMap<>();
		//for(String purpose:FLEX_ATIVITIES)
			for(Household household:households.values()) {
				for(Person person:household.getPersons().values()) {
					long time = 0;
					String prevPurpose = person.getFirstActivity();
					Trip firstTrip = person.getTrips().values().iterator().next();
					String prevZip = firstTrip.getStartPostalCode();
					String prevType = null;
					for(Trip trip:person.getTrips().values()) {
						Location startLocation = Household.LOCATIONS.get(trip.getStartPostalCode());
						Location endLocation = Household.LOCATIONS.get(trip.getEndPostalCode());
						if(nodes.get(startLocation.getPostalCode())==null)
							nodes.put(startLocation.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(startLocation.getCoord())));
						if(nodes.get(endLocation.getPostalCode())==null)
							nodes.put(endLocation.getPostalCode(), net.getNearestNode(coordinateTransformation.transform(endLocation.getCoord())));
						if(FLEX_ATIVITIES.contains(prevPurpose) && prevType!=null) {
							long duration = (trip.getStartTime().getTime()-time)/1000;
							if(duration<0)
								if(duration<-15*3600)
									duration += 24*3600;
								else
									duration = -duration;
							else if(duration==0)
								duration = 60;
							Set<Observation> obs = obsMap.get(prevPurpose);
							if(obs==null) {
								obs = new HashSet<>();
								obsMap.put(prevPurpose, obs);
							}
							if(startLocation.getDetailedType(Trip.PlaceType.getPlaceType(prevType))!=null)
								obs.add(new Observation(startLocation.getPostalCode(), duration, startLocation.getDetailedType(Trip.PlaceType.getPlaceType(prevType))));
						}
						prevZip = endLocation.getPostalCode();
						prevType = trip.getPlaceType();
						time = trip.getEndTime().getTime();
						prevPurpose = trip.getPurpose();
					}
					if(FLEX_ATIVITIES.contains(prevPurpose) && prevType!=null) {
						long duration = (24*3600000-time)/1000;
						if(duration<0)
							if(duration<-15*3600)
								duration += 24*3600;
							else
								duration = -duration;
						else if(duration==0)
							duration = 60;
						Set<Observation> obs = obsMap.get(prevPurpose);
						if(obs==null) {
							obs = new HashSet<>();
							obsMap.put(prevPurpose, obs);
						}
						if(Household.LOCATIONS.get(prevZip).getDetailedType(Trip.PlaceType.getPlaceType(prevType))!=null)
							obs.add(new Observation(prevZip, duration, Household.LOCATIONS.get(prevZip).getDetailedType(Trip.PlaceType.getPlaceType(prevType))));
					}
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
			Map<Id<Node>, Path> paths = dijkstra.calcLeastCostPath(nodes.get(postalCode), nodesPC, 8*3600, null, null);
			for(Entry<Id<Node>, Path> e:paths.entrySet())
				printerP.println(nodes.get(postalCode).getId().toString()+" "+e.getKey().toString()+" "+e.getValue().travelTime+" "+e.getValue().travelCost);
			//System.out.println(System.currentTimeMillis()-t);
		}
		printerP.close();*/
		System.out.println("TTs done");	
		Map<String, Map<String, Map<DetailedType, Double>>> accs = calculateAccessibilities(nodes, locs, ttMap, obsMap);
		System.out.println("Accs done");
		for(DetailedType detailedType:Location.DetailedType.values()) {
			PrintWriter printer = new PrintWriter("./data/types/"+detailedType.name().toLowerCase()+".dat");
			writeHeaderT(printer, detailedType.name());
			for(Household household:households.values())
				for(Person person:household.getPersons().values()) {
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
					for(Trip trip:person.getTrips().values())
						if(FLEX_ATIVITIES.contains(trip.getPurpose())) {
							Integer num = typesPlaces.get(trip.getPurpose()).get(detailedType);
							if(num!=null && num>MIN_OCURR) {
								printer.print(person.getAgeInterval().getCenter()+"\t");
								printer.print(person.getIncomeInterval().getCenter()+"\t");
								printer.print(person.getMainIncome(household)+"\t");
								printer.print(household.getPersons().size()+"\t");
								printer.print(Math.max(accs.get(trip.getPurpose()).get(trip.getEndPostalCode()).get(detailedType), accs.get(trip.getPurpose()).get(trip.getStartPostalCode()).get(detailedType))+"\t");
								printer.print(homeTime+"\t");
								printer.print(workTime+"\t");
								printer.print((person.getGender().equals("Male")?0:1)+"\t");
								printer.print((person.hasCar()?1:0)+"\t");
								printer.print((household.getEthnic().equals("Chinese")?1:0)+"\t");
								printer.print((household.getEthnic().equals("Indian")?1:0)+"\t");
								printer.print((household.getEthnic().equals("Malay")?1:0)+"\t");
								printer.print((person.getRole().equals(Role.MAIN)?1:0)+"\t");
								printer.print((person.getRole().equals(Role.PARTNER)?1:0)+"\t");
								printer.print((person.getRole().equals(Role.YOUNGER)?1:0)+"\t");
								printer.print(trip.getFactor()+"\t");
								printer.print(detailedType.equals(Household.LOCATIONS.get(trip.getEndPostalCode()).getDetailedType(Trip.PlaceType.getPlaceType(trip.getPlaceType())))?"1":"0");
								for(Day day:Day.values())
									printer.print((person.getSurveyDay().equals(day)?1:0)+"\t");
								printer.println();
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
					boolean isPurpose = false;
					prevPurpose = person.getFirstActivity();
					time = 0;
					String prevZip = person.getTrips().values().iterator().next().getStartPostalCode();
					for(Trip trip:person.getTrips().values()) {
						if(purpose.equals(prevPurpose)) {
							isPurpose = true;
							long duration = (trip.getStartTime().getTime()-time)/1000;
							if(duration<0)
								if(duration<-12*3600)
									duration += 24*3600;
								else
									duration = -duration;
							else if(duration==0)
								duration = 60;
							printer.print(duration+"\t");
							printer.print(Math.pow(duration, 2)+"\t");
							List<Double> params = new ArrayList<>();
							params.add((double) person.getAgeInterval().getCenter());
							params.add((double) person.getIncomeInterval().getCenter());
							params.add((double) person.getMainIncome(household));
							params.add((double) household.getPersons().size());
							for(DetailedType detailedType:detailedTypes)
								params.add(accs.get(purpose).get(prevZip).get(detailedType));
							params.add((double) homeTime);
							params.add((double) workTime);
							params.add((double) (person.getGender().equals("Male")?0:1));
							params.add((double) (person.hasCar()?1:0));
							params.add((double) (household.getEthnic().equals("Chinese")?1:0));
							params.add((double) (household.getEthnic().equals("Indian")?1:0));
							params.add((double) (household.getEthnic().equals("Malay")?1:0));
							params.add((double) (person.getRole().equals(Role.MAIN)?1:0));
							params.add((double) (person.getRole().equals(Role.PARTNER)?1:0));
							params.add((double) (person.getRole().equals(Role.YOUNGER)?1:0));
							writeLine(params,printer);
							for(Day day:Day.values())
								printer.print((person.getSurveyDay().equals(day)?1:0)+"\t");
							printer.println();
						}
						prevPurpose = trip.getPurpose();
						prevZip = trip.getEndPostalCode();
						time = trip.getEndTime().getTime();
					}
					if(purpose.equals(prevPurpose)) {
						isPurpose = true;
						long duration = (24*3600000-time)/1000;
						if(duration<0)
							if(duration<-12*3600)
								duration += 24*3600;
							else
								duration = -duration;
						else if(duration==0)
							duration = 60;
						printer.print(duration+"\t");
						printer.print(Math.pow(duration, 2)+"\t");
						List<Double> params = new ArrayList<>();
						params.add((double) person.getAgeInterval().getCenter());
						params.add((double) person.getIncomeInterval().getCenter());
						params.add((double) person.getMainIncome(household));
						params.add((double) household.getPersons().size());
						for(DetailedType detailedType:detailedTypes)
							params.add(accs.get(purpose).get(prevZip).get(detailedType));
						params.add((double) homeTime);
						params.add((double) workTime);
						params.add((double) (person.getGender().equals("Male")?0:1));
						params.add((double) (person.hasCar()?1:0));
						params.add((double) (household.getEthnic().equals("Chinese")?1:0));
						params.add((double) (household.getEthnic().equals("Indian")?1:0));
						params.add((double) (household.getEthnic().equals("Malay")?1:0));
						params.add((double) (person.getRole().equals(Role.MAIN)?1:0));
						params.add((double) (person.getRole().equals(Role.PARTNER)?1:0));
						params.add((double) (person.getRole().equals(Role.YOUNGER)?1:0));
						writeLine(params,printer);
						for(Day day:Day.values())
							printer.print((person.getSurveyDay().equals(day)?1:0)+"\t");
						printer.println();
					}
					printerB.print(person.getAgeInterval().getCenter()+"\t");
					printerB.print(person.getIncomeInterval().getCenter()+"\t");
					printerB.print(person.getMainIncome(household)+"\t");
					printerB.print(household.getPersons().size()+"\t");
					for(DetailedType detailedType:detailedTypes) {
						double maxAcc = 0;
						for(Trip trip:person.getTrips().values()) {
							double startAcc = accs.get(purpose).get(trip.getStartPostalCode()).get(detailedType);
							if(startAcc>maxAcc)
								maxAcc = startAcc;
							double endAcc = accs.get(purpose).get(trip.getEndPostalCode()).get(detailedType);
							if(endAcc>maxAcc)
								maxAcc = endAcc;
						}
						if(maxAcc==0)
							System.out.println();
						printerB.print(maxAcc+"\t");
					}
					printerB.print(homeTime+"\t");
					printerB.print(workTime+"\t");
					printerB.print((person.getGender().equals("Male")?0:1)+"\t");
					printerB.print((person.hasCar()?1:0)+"\t");
					printerB.print((household.getEthnic().equals("Chinese")?1:0)+"\t");
					printerB.print((household.getEthnic().equals("Indian")?1:0)+"\t");
					printerB.print((household.getEthnic().equals("Malay")?1:0)+"\t");
					printerB.print((person.getRole().equals(Role.MAIN)?1:0)+"\t");
					printerB.print((person.getRole().equals(Role.PARTNER)?1:0)+"\t");
					printerB.print((person.getRole().equals(Role.YOUNGER)?1:0)+"\t");
					printerB.print(person.getFactor()+"\t");
					printerB.print((isPurpose?"1":"0")+"\t");
					for(Day day:Day.values())
						printerB.print((person.getSurveyDay().equals(day)?1:0)+"\t");
					printerB.println();
				}
			printer.close();
			printerB.close();
		}
	}

	private static Map<String, Map<String, Map<DetailedType, Double>>> calculateAccessibilities(Map<String, Node> nodes, Map<DetailedType, Map<String, String>> locs, Map<String, Map<String, Double>> ttMap, Map<String, Set<Observation>> obsMap) {
		Map<String, Map<String, Map<DetailedType, Double>>> accs = new HashMap<>();
		OLSMultipleLinearRegression mlr = new OLSMultipleLinearRegression();
		Map<String,double[]> maxMinsA = new HashMap<>();
		maxMinsA.put("rec", new double[]{1E-4, 1.1E-4, 0.00002});
		maxMinsA.put("errands", new double[]{-1.3E-4, -1.2E-4, 0.00002});
		maxMinsA.put("shop", new double[]{1.6E-3, 1.7E-3, 0.0002});
		maxMinsA.put("social", new double[]{2E-3, 2.1E-3, 0.0001});
		maxMinsA.put("accomp", new double[]{4.2E-3, 4.3E-3, 0.0002});
		maxMinsA.put("eat", new double[]{1.43E-2, 1.44E-2, 0.0002});
		Map<String,double[]> maxMinsB = new HashMap<>();
		maxMinsB.put("rec", new double[]{-0.4, -0.3, 0.1});
		maxMinsB.put("errands", new double[]{0.3, 0.4, 0.1});
		maxMinsB.put("shop", new double[]{1.4, 1.5 ,0.1});
		maxMinsB.put("social", new double[]{0.2, 0.3, 0.1});
		maxMinsB.put("accomp", new double[]{-0.1, 0.0, 0.1});
		maxMinsB.put("eat", new double[]{0.82, 0.83, 0.02});
		for(String purpose:FLEX_ATIVITIES) {
			Set<Observation> obs = obsMap.get(purpose);
			double maxT = 0, maxA = Double.NaN, maxB = Double.NaN;
			double[] maxMinA = maxMinsA.get(purpose);
			double[] maxMinB = maxMinsB.get(purpose);
			for(double a=maxMinA[0]; a==maxMinA[0]; a+=maxMinA[2])
				for(double b=maxMinB[0]; b==maxMinB[0]; b+=maxMinB[2]) {
					Map<String, Map<DetailedType, Double>> accsPurpose = new HashMap<>();
					for(Observation ob:obs) {
						Node node = nodes.get(ob.zipCode);
						Map<DetailedType, Double> accMap = new HashMap<Location.DetailedType, Double>();
						for(DetailedType detailedType:Location.DetailedType.values()) {
							double sum=0;
							for(Entry<String, String> entry:locs.get(detailedType).entrySet()) {
								Location location = Household.LOCATIONS.get(entry.getKey());
								sum+=Math.pow(location.getTypes().get(location.getType(detailedType).text), b)*Math.exp(-a*ttMap.get(node.getId().toString()).get(entry.getValue()));
							}
							accMap.put(detailedType, sum);
						}
						accsPurpose.put(ob.zipCode, accMap);
					}
					double[] y = new double[obs.size()];
					double[][] x = new double[obs.size()][1];
					int o = 0;
					for(Observation ob:obs) {
						y[o] = ob.duration;
						x[o++][0] = accsPurpose.get(ob.zipCode).get(ob.detailedType);
					}
					mlr.newSampleData(y, x);
					System.out.println(a+" "+b+" "+mlr.calculateRSquared()+" "+mlr.estimateRegressionParameters()[1]/mlr.estimateRegressionParametersStandardErrors()[1]+" "+mlr.estimateRegressionParameters()[0]+" "+mlr.estimateRegressionParameters()[1]+" "+obs.size()+" "+purpose);
					if(mlr.estimateRegressionParameters()[1]/mlr.estimateRegressionParametersStandardErrors()[1]>maxT) {
						maxT = mlr.estimateRegressionParameters()[1]/mlr.estimateRegressionParametersStandardErrors()[1];
						maxA = a;
						maxB = b;
					}
				}
			Map<String, Map<DetailedType, Double>> maxAccsPurpose = new HashMap<>();
			for(Entry<String, Node> nodeE:nodes.entrySet()) {
				Map<DetailedType, Double> accMap = new HashMap<Location.DetailedType, Double>();
				for(DetailedType detailedType:Location.DetailedType.values()) {
					double sum=0;
					for(Entry<String, String> entry:locs.get(detailedType).entrySet()) {
						Location location = Household.LOCATIONS.get(entry.getKey());
						sum+=Math.pow(location.getTypes().get(location.getType(detailedType).text), maxB)*Math.exp(-maxA*ttMap.get(nodeE.getValue().getId().toString()).get(entry.getValue()));
					}
					accMap.put(detailedType, sum);
				}
				maxAccsPurpose.put(nodeE.getKey(), accMap);
			}
			System.out.println(maxA+" "+maxB+" "+maxT+" "+obs.size()+" "+purpose);
			accs.put(purpose, maxAccsPurpose);
		}
		return accs;
	}

	private static void writeLine(List<Double> params, PrintWriter printer) {
		for(Double number:params)
			printer.print(number+"\t");
		for(int i=0; i<params.size(); i++)
			for(int j=i+1; j<params.size(); j++)
				printer.print(params.get(i)*params.get(j)+"\t");
	}

	private static void writeHeaderT(PrintWriter printer, String detailedType) {
		printer.print("AGE\t");
		printer.print("INCOME\t");
		printer.print("MAIN_INCOME\t");
		printer.print("HOUSEHOLD_SIZE\t");
		printer.print("ACC_"+detailedType+"\t");
		printer.print("HOME_TIME\t");
		printer.print("WORK_TIME\t");
		printer.print("GENDER\t");
		printer.print("CAR_AVAIL\t");
		printer.print("CHINESE\t");
		printer.print("INDIAN\t");
		printer.print("MALAY\t");
		printer.print("MAIN\t");
		printer.print("PARTNER\t");
		printer.print("YOUNGER\t");
		printer.print("WEIGHT\t");
		printer.print("CHOICE\t");
		for(Day day:Day.values())
			printer.print(day.name().toUpperCase()+"\t");
		printer.println();
	}

	private static void writeHeaderP(PrintWriter printer, Set<DetailedType> detailedTypes) {
		printer.print("DURATION\t");
		printer.print("DURATION_SQR\t");
		List<String> titles = new ArrayList<>();
		titles.add("AGE");
		titles.add("INCOME");
		titles.add("MAIN_INCOME");
		titles.add("HOUSEHOLD_SIZE");
		for(DetailedType detailedType:detailedTypes)
			titles.add("ACC_"+detailedType.name().toUpperCase()+"");
		titles.add("HOME_TIME");
		titles.add("WORK_TIME");
		titles.add("GENDER");
		titles.add("CAR_AVAIL");
		titles.add("CHINESE");
		titles.add("INDIAN");
		titles.add("MALAY");
		titles.add("MAIN");
		titles.add("PARTNER");
		titles.add("YOUNGER");
		writeTitles(titles, printer);
		for(Day day:Day.values())
			printer.print(day.name().toUpperCase()+"\t");
		printer.println();
	}
	private static void writeTitles(List<String> params, PrintWriter printer) {
		for(String number:params)
			printer.print(number+"\t");
		for(int i=0; i<params.size(); i++)
			for(int j=i+1; j<params.size(); j++)
				printer.print(params.get(i)+params.get(j)+"\t");
	}

	private static void writeHeaderPB(PrintWriter printer, Set<DetailedType> detailedTypes) {
		printer.print("AGE\t");
		printer.print("INCOME\t");
		printer.print("MAIN_INCOME\t");
		printer.print("HOUSEHOLD_SIZE\t");
		for(DetailedType detailedType:detailedTypes)
			printer.print("ACC_"+detailedType.name().toUpperCase()+"\t");
		printer.print("HOME_TIME\t");
		printer.print("WORK_TIME\t");
		printer.print("GENDER\t");
		printer.print("CAR_AVAIL\t");
		printer.print("CHINESE\t");
		printer.print("INDIAN\t");
		printer.print("MALAY\t");
		printer.print("MAIN\t");
		printer.print("PARTNER\t");
		printer.print("YOUNGER\t");
		printer.print("WEIGHT\t");
		printer.print("CHOICE\t");
		for(Day day:Day.values())
			printer.print(day.name().toUpperCase()+"\t");
		printer.println();
	}

}
