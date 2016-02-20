package playground.southafrica.affordability;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.utilities.Header;

/**
 * Calculating the accessibility of households. Currently (Sep '12) the calculations
 * are based on the final project work of Jeanette de Hoog.
 *  
 * @author jwjoubert
 */
public class AccessibilityCalculator {
	private final static Logger LOG = Logger.getLogger(AccessibilityCalculator.class);
	
	private MutableScenario sc;
	private Households hhs;
	private Network transitNetwork;
	
	private QuadTree<ActivityFacility> facilityQT;
	private QuadTree<ActivityFacility> schoolQT;
	private QuadTree<ActivityFacility> healthcareQT;
	private QuadTree<ActivityFacility> shoppingQT;
	private QuadTree<Coord> busStops;
	private QuadTree<Coord> railStops;
	private QuadTree<Coord> taxiStops;
	
	private Map<Integer, String> classDescription = new TreeMap<Integer, String>();
	private AStarEuclidean routerDrive;
	private AStarEuclidean routerWalk;
	private static Map<String, Integer> activityOptions = new TreeMap<String, Integer>();
	
	/*TODO Remove after validation. */
	private List<Integer> numberInClasses;
	private static int workAtHomeCounter = 0;
	private static int noEducationCounter = 0;

	/**
	 * Run this class to generate the accessibility scores for households. The
	 * class requires the following arguments. 
	 * @param args absolute paths of the following files:
	 * <ol>
	 * 		<li> household file;
	 * 		<li> population (plans) file;
	 * 		<li> person attributes file;
	 * 		<li> consolidated facilities file;
	 * 		<li> network file;
	 * 		<li> transit schedule;
	 * 		<li> transit network file. This is necessary since the transit 
	 * 			 network may have additional links resulting from the 
	 * 			 <code>GTFS2MATSim</code> contribution;
	 * 		<li> the output folder.
	 */
	public static void main(String[] args) {
		Header.printHeader(AccessibilityCalculator.class.toString(), args);
		
		Config config = ConfigUtils.createConfig();
		config.transit().setUseTransit(true);
//		config.scenario().setUseVehicles(true);
		MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(config);
		
		/* Read households. */
		String householdFile = args[0];
		Households hhs = new HouseholdsImpl();
		HouseholdsReaderV10 hhr = new HouseholdsReaderV10(hhs);
		hhr.readFile(householdFile);
		LOG.info("Number of households: " + hhs.getHouseholds().size());
		
		/* Read population */
		String populationFile = args[1];
		sc.getTransitSchedule();
		MatsimPopulationReader mpr = new MatsimPopulationReader(sc);
		mpr.readFile(populationFile);
		LOG.info("Number of persons: " + sc.getPopulation().getPersons().size());
//		PopulationUtils.printActivityStatistics(populationFile);
		
		/* Read population attributes */
		String personAttributesFile = args[2];
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
		oar.parse(personAttributesFile);
		/* Add attributes to population. */
		for(Id<Person> id : sc.getPopulation().getPersons().keySet()){
			String hhId = (String) oa.getAttribute(id.toString(), "householdId");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("householdId", Id.create(hhId, Household.class));
			Double hhIncome = (Double) oa.getAttribute(id.toString(), "householdIncome");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("householdIncome", hhIncome);
			String race = (String) oa.getAttribute(id.toString(), "race");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("race", race);
			String school = (String) oa.getAttribute(id.toString(), "school");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("school", school);
		}
		LOG.info("Done adding custom attributes: household Id; household income; and race.");
		
		/* Read facilities */
		String facilitiesFile = args[3];
		FacilitiesReaderMatsimV1 fr = new FacilitiesReaderMatsimV1(sc);
		fr.readFile(facilitiesFile);
		LOG.info("Number of facilities: " + sc.getActivityFacilities().getFacilities().size());
		for(ActivityFacility af : sc.getActivityFacilities().getFacilities().values()){
			for(String s : af.getActivityOptions().keySet()){
				if(!activityOptions.containsKey(s)){
					activityOptions.put(s, 1);
				} else{
					int oldValue = activityOptions.get(s);
					activityOptions.put(s, oldValue+1);
				}
			}
		}
		LOG.info("----------------------------------------------------");
		LOG.info("Summary of activity options offered at facilities:");
		for(String s : activityOptions.keySet()){
			LOG.info("   " + s + ": " + activityOptions.get(s));
		}
		
		
		/* Read network */
		String networkFile = args[4];
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc.getNetwork());
		mnr.readFile(networkFile);
		
		
		/* Read the transit schedule. */
		String transitFile = args[5];
		TransitScheduleReader tsr = new TransitScheduleReader(sc);
		tsr.readFile(transitFile);
		Scenario scTransit = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader transitNetworkReader = new MatsimNetworkReader(scTransit.getNetwork());
		transitNetworkReader.readFile(args[6]);
		
		AccessibilityCalculator ac = new AccessibilityCalculator(sc, hhs, scTransit.getNetwork());
		String outputFolder = args[7];
		ac.run(outputFolder);
		
		/* Report some statistics. */
		LOG.info("----------------------------------------------------");
		LOG.info("Exceptions handled:");
		LOG.info("   Number of people working from home: " + workAtHomeCounter);
		LOG.info("   Number of educationless scholars  : " + noEducationCounter);
		
		Header.printFooter();
	}
	
	
	/**
	 * Constructor to set up accessibility calculator. A number of procedures
	 * are executed:
	 * <ol>
	 * 		<li> {@link QuadTree}s are set up for all facilities, distinguishing
	 * 			 between school, health care and shopping facilities;
	 * 		<li> {@link QuadTree}s are set up for all taxi, bus and rail stops.
	 * 		<li> two routers are created, one for driving and another for 
	 * 			 walking.
	 * @param scenario
	 * @param households
	 * @param transitNetwork
	 */
	public AccessibilityCalculator(Scenario scenario, Households households, Network transitNetwork) {
		this.sc = (MutableScenario) scenario;
		this.hhs = households;
		this.transitNetwork = transitNetwork;
		
		/* Build QuadTree of facilities. */
		Map<Id<ActivityFacility>, ? extends ActivityFacility> facilities = sc.getActivityFacilities().getFacilities();
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for(ActivityFacility af : facilities.values()){
			minX = Math.min(minX, af.getCoord().getX());
			maxX = Math.max(maxX, af.getCoord().getX());
			minY = Math.min(minY, af.getCoord().getY());
			maxY = Math.max(maxY, af.getCoord().getY());
		}
		this.facilityQT = new QuadTree<ActivityFacility>(minX, minY, maxX, maxY);
		this.schoolQT = new QuadTree<ActivityFacility>(minX, minY, maxX, maxY);
		this.healthcareQT = new QuadTree<ActivityFacility>(minX, minY, maxX, maxY);
		this.shoppingQT = new QuadTree<ActivityFacility>(minX, minY, maxX, maxY);
		
		this.busStops = new QuadTree<Coord>(minX, minY, maxX, maxY);
		this.railStops = new QuadTree<Coord>(minX, minY, maxX, maxY);
		this.taxiStops = new QuadTree<Coord>(minX, minY, maxX, maxY);
		
		for(ActivityFacility af : facilities.values()){
			this.facilityQT.put(af.getCoord().getX(), af.getCoord().getY(), af);
			
			/* Build separate education QuadTree. Used for children who SHOULD
			 * be going to school, but who do not have education as an activity. */
			if(af.getActivityOptions().containsKey("e")){
				this.schoolQT.put(af.getCoord().getX(), af.getCoord().getY(), af);
			}
			
			/* Build separate QuadTree for healthcare facilities. */
			if(af.getActivityOptions().containsKey("m")){
				this.healthcareQT.put(af.getCoord().getX(), af.getCoord().getY(), af);
			}
			
			/* Build separate QuadTree for shopping & leisure facilities. */
			if(af.getActivityOptions().containsKey("s") || 
			   af.getActivityOptions().containsKey("l")){
				this.shoppingQT.put(af.getCoord().getX(), af.getCoord().getY(), af);
			}
		}
		
		TravelDisutility travelCost = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength();
			}
		};
		
		LOG.info("Preprocessing the network for travel time calculation.");
		PreProcessLandmarks pp = new PreProcessLandmarks(travelCost);
		pp.run(this.sc.getNetwork());
		setupRouterForDriving(pp);
		setupRouterForWalking(pp);

		/* Set up the transit QuadTrees. */
		setupTransitQuadTrees();
		
		
		/* Validation */
		numberInClasses = new ArrayList<Integer>();
		for(int i = 0; i < 6; i++){
			numberInClasses.add(0);
		}
		classDescription.put(0, "Should not happen.");
		classDescription.put(1, "School-going.");
		classDescription.put(2, "Employed, accompanying scholar.");
		classDescription.put(3, "Employed, no children.");
		classDescription.put(4, "Unemployed, accompanying scholar.");
		classDescription.put(5, "Unemployed, no children.");
	}


	/**
	 * Sets up the {@link AStarEuclidean} router for walking using an average
	 * walk time of 3km/h (0.8333 m/s).
	 * @param pp
	 */
	private void setupRouterForWalking(PreProcessLandmarks pp) {
		TravelTime travelTimeWalk = new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				double travelTime = link.getLength() / (3.0*1000.0 / (60.0 * 60.0));
				if(Double.isNaN(travelTime)){
					LOG.warn("NaN travel time.");
				}
				return travelTime;
			}
		};
		routerWalk = new AStarEuclidean(sc.getNetwork(), pp, travelTimeWalk);
	}


	/** 
	 * Sets up the {@link AStarEuclidean} router for driving, estimating the 
	 * link travel time as 1.2 times the free speed travel time for each link. 
	 * @param pp
	 */
	private void setupRouterForDriving(PreProcessLandmarks pp) {
		TravelTime travelTimeDrive = new TravelTime() {

			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				double travelTime = (link.getLength() / link.getFreespeed()) * 1.2;
				if(Double.isNaN(travelTime)){
					LOG.warn("NaN travel time.");
				}
				return travelTime;
			}
		};
		routerDrive = new AStarEuclidean(sc.getNetwork(), pp, travelTimeDrive);
	}
	
	
	/**
	 * Setting up the {@link QuadTree}s for transit stops. For bus and rail
	 * we simply get the {@link TransitStopFacility}s of all lines., and add 
	 * their {@link Coord}s to the associated {@link QuadTree}. For paratransit,
	 * i.e. minibus taxis, we use the bus lines, and add all intersections,
	 * that is {@link Node}s with more than two {@link Link}s connected to it.
	 * We do ensure (currently, Sep '12) that no paratransit stops are within
	 * 200m of one another.  
	 */
	private void setupTransitQuadTrees(){
		Map<Id<TransitLine>, TransitLine> lines = sc.getTransitSchedule().getTransitLines();
		LOG.info("Setting up the transit QuadTrees... (" + lines.size() + ")");
		Counter counter = new Counter("   lines # ");
		for(TransitLine line : lines.values()){
			for(TransitRoute route : line.getRoutes().values()){
				if(route.getTransportMode().equalsIgnoreCase("bus")){
					for(TransitRouteStop trs : route.getStops()){
						Coord busStop = trs.getStopFacility().getCoord();
						Coord albersBusStop = new Coord(busStop.getX(), busStop.getY());
						busStops.put(
								albersBusStop.getX(), 
								albersBusStop.getY(), 
								albersBusStop);
					}
					
					/* Also need to deal with taxis here. */
					for(Id<Link> linkId : route.getRoute().getLinkIds()){
						NodeImpl toNode = (NodeImpl) transitNetwork.getLinks().get(linkId).getToNode();
						if(toNode.getOutLinks().size() > 1 || toNode.getInLinks().size() > 1){
							/* Only consider intersections. */
							Coord albersIntersection = new Coord(toNode.getCoord().getX(), toNode.getCoord().getY());
							Coord closestTaxiStop = taxiStops.getClosest(albersIntersection.getX(), albersIntersection.getY());
							if(closestTaxiStop == null){
								taxiStops.put(albersIntersection.getX(), albersIntersection.getY(), albersIntersection);
							} else{
								double distanceToClosestTaxiStop = CoordUtils.calcEuclideanDistance(albersIntersection, closestTaxiStop); 
//								LOG.info("Distance: " + distanceToClosestTaxiStop);
								if( distanceToClosestTaxiStop > 200){
									taxiStops.put(albersIntersection.getX(), albersIntersection.getY(), albersIntersection);
								}
							}
						}
					}
				} else if(route.getTransportMode().equalsIgnoreCase("rail")){
					for(TransitRouteStop trs : route.getStops()){
						Coord railStop = trs.getStopFacility().getCoord();
						Coord albersRailStop = new Coord(railStop.getX(), railStop.getY());
						railStops.put(
								albersRailStop.getX(), 
								albersRailStop.getY(), 
								albersRailStop);
					}
				
				} else {
					LOG.warn("Could not find mode " + route.getTransportMode());
				}
			}
			counter.incCounter();
		}
		counter.printCounter();
		LOG.info("Done setting up transit QuadTrees.");
		LOG.info("    Bus: " + busStops.size());
		LOG.info("   Taxi: " + taxiStops.size());
		LOG.info("   Rail: " + railStops.size());
	}
	
	
	/**
	 * Method executing the overall accessibility calculation. It includes
	 * the writing of the final output.
	 * @param outputFolder
	 */
	public void run(String outputFolder){
		LOG.info("Start running...");
		Counter counter = new Counter("   person # ");
		
		Map<Id<Household>, Tuple<Double, Integer>> householdScoreMap = new TreeMap<Id<Household>, Tuple<Double,Integer>>();

		String bwName = outputFolder + "accessibility.txt";
		BufferedWriter bw = IOUtils.getBufferedWriter(bwName);
		try{
			for(Person person : this.sc.getPopulation().getPersons().values()){
				/* Calculate the individual's accessibility score. */
				double accessibility = calculateAccessibility(person);
				
				/* Add the individual's score to that of the household. */
				Id<Household> householdId = Id.create(person.getCustomAttributes().get("householdId").toString(), Household.class);
				if(!householdScoreMap.containsKey(householdId)){
					householdScoreMap.put(householdId, new Tuple<Double, Integer>(accessibility, 1));
				} else{
					double oldScore = householdScoreMap.get(householdId).getFirst();
					int oldCount = householdScoreMap.get(householdId).getSecond();
					householdScoreMap.put(householdId, new Tuple<Double, Integer>(oldScore + accessibility, oldCount + 1));
				}
				
				bw.write(String.valueOf(accessibility));
				bw.newLine();
				counter.incCounter();
			}
			counter.printCounter();
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + bwName);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + bwName);
			}
		}
		
		LOG.info("----------------------------------------------------");
		LOG.info("Number of persons in different classes:");
		for(int i = 0; i < numberInClasses.size(); i++){
			LOG.info("   " + i + ": " + numberInClasses.get(i));
		}
		
		LOG.info("----------------------------------------------------");
		LOG.info("Number of households observed: " + householdScoreMap.size());
		bwName = outputFolder + "householdAccessibility.txt";
		bw = IOUtils.getBufferedWriter(bwName);
		try{
			bw.write("Id,Long,Lat,AccessScore");
			bw.newLine();
			
			/* Calculate the household average. */
			for(Id<Household> householdId : householdScoreMap.keySet()){
				Tuple<Double, Integer> tuple = householdScoreMap.get(householdId);
				double householdAverage = tuple.getFirst() / ((double) tuple.getSecond());
				
				/* Find an individual in the household to get their home coordinate. */
				List<Id<Person>> members = hhs.getHouseholds().get(householdId).getMemberIds();
				Person person = null;
				int index = 0;
				while(person == null && index < members.size()){
					person = sc.getPopulation().getPersons().get(members.get(index));
					index++;
				}
				if(person != null){
					Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();					
					bw.write(String.format("%s,%.0f,%.0f,%.2f\n", householdId, homeCoord.getX(), homeCoord.getY(), householdAverage));
				} else{
					LOG.warn("Couldn't find any members for household " + householdId + " - household is ignored.");
				}
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not write to BufferedWriter " + bwName);
		} finally{
			try {
				bw.close();
			} catch (IOException e) {
				throw new RuntimeException("Could not close BufferedWriter " + bwName);
			}
		}
	}	
	
	
	/**
	 * 
	 * @param person
	 * @return
	 */
	public double calculateAccessibility(Person person){
	
		double mobility = getMobilityScore(person);
		double transportOptions = getTransportOptionsScore(person);
		double chosenMode = getChosenModeScore(person);
		double availableFacilities = getAvailableFacilityScore(person);
		
		return 10*mobility + 10*transportOptions + 10*chosenMode + 10*availableFacilities;
	}
	
	
	private double getMobilityScore(Person person){
		int accessibilityClass = getAccessibilityClass(person);
		
		/*TODO Remove after validation */
		int oldValue = numberInClasses.get(accessibilityClass);
		numberInClasses.set(accessibilityClass, oldValue+1);
		
		Coord homeCoord = ((ActivityImpl)person.getSelectedPlan().getPlanElements().get(0)).getCoord();

		/*--- Mobility ---*/
		double tt_work;
		double tt_education;
		double tt_healthcare;
		double tt_shopping;
		double score_mobility = 0;
		
		switch (accessibilityClass) {
		case 1:
			/* Education */
			double s1 = getEducationScore( getTravelTimeToEducation(person) );
			s1 += getHealthcareScore( getTravelTimeToHealthcare(person) );
			s1 += getShoppingScore( getTravelTimeToShopping(person) );
			score_mobility = s1/3;
			break;
		case 2:
			double s2 = getWorkScore( getTravelTimeToWork(person) );
			s2 += getEducationScore( getTravelTimeToEducation(person) );
			s2 += getHealthcareScore( getTravelTimeToHealthcare(person) );
			s2 += getShoppingScore( getTravelTimeToShopping(person) );
			score_mobility = s2/4;
			break;
		case 3:
			double s3 = getWorkScore( getTravelTimeToWork(person) );
			s3 += getHealthcareScore( getTravelTimeToHealthcare(person) );
			s3 += getShoppingScore( getTravelTimeToShopping(person) );
			score_mobility = s3/3;
			break;
		case 4:
			double s4 = getEducationScore( getTravelTimeToEducation(person) );
			s4 += getHealthcareScore( getTravelTimeToHealthcare(person) );
			s4 += getShoppingScore( getTravelTimeToShopping(person) );
			score_mobility = s4/3;
			break;
		case 5:
			double s5 = 0.0;
			s5 = getHealthcareScore( getTravelTimeToHealthcare(person) );
			s5 += getShoppingScore( getTravelTimeToShopping(person) );
			score_mobility = s5/2;
			break;			
		default:
			break;
		}
		return score_mobility;
	}
	
	private double getEducationScore(double traveltime){
		double score = 0.0;
		if(traveltime <= 30*60){
			score = 2.0;
		} else if(traveltime <= 60*60){
			score = 1.0;
		} 
		return score;
	}

	private double getWorkScore(double traveltime){
		double score = 0.0;
		if(traveltime <= 30*60){
			score = 2.0;
		} else if(traveltime <= 90*60){
			score = 1.0;
		} 
		return score;
	}

	private double getHealthcareScore(double traveltime){
		double score = 0.0;
		if(traveltime <= 30*60){
			score = 2.0;
		} else if(traveltime <= 60*60){
			score = 1.0;
		} 
		return score;
	}

	private double getShoppingScore(double traveltime){
		double score = 0.0;
		if(traveltime <= 15*60){
			score = 2.0;
		} else if(traveltime <= 30*60){
			score = 1.0;
		} 
		return score;
	}
	
	private double getTransportOptionsScore(Person person){
		
		/*TODO Remove after debugging. */
//		LOG.info("                              Person " + person.getId());
		if(person.getId().toString().equalsIgnoreCase("1000928")){
//			LOG.info("... found looping person...");
		}
		

		Coord homeCoord = ((ActivityImpl)person.getSelectedPlan().getPlanElements().get(0)).getCoord();

		double score = 0.0;
		
		/* Car. Assuming that if you have a car, you'll use it. Therefore,
		 * check if any leg mode contains "car". */
		score += travelByCar(person.getSelectedPlan()) ? 5 : 0;
		
		/* Check for short distance walking to destination. */
		score += hasClosePrimaryActivity(person.getSelectedPlan()) ? 4 : 0;
		
		/* check for transit access. */
		score += hasTransitAccess(homeCoord, taxiStops) ? 3 : 0;
		score += hasTransitAccess(homeCoord, busStops) ? 2 : 0;
		score += hasTransitAccess(homeCoord, railStops) ? 1 : 0;
		
		if(score <= 2){
			return 0.0;
		} else if (score <= 9){
			return 1.0;
		} else{
			return 2;
		}
	}


	private double getChosenModeScore(Person person){
		double time = 0.0;
		for(int i = 0; i < person.getSelectedPlan().getPlanElements().size()-1; i++){
			PlanElement pe = person.getSelectedPlan().getPlanElements().get(i);
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				Leg leg = (Leg) person.getSelectedPlan().getPlanElements().get(i+1);
				String chosenMode = leg.getMode();
				if(chosenMode.equalsIgnoreCase("car")){
					/* Keep the zero time. */
				} else if(chosenMode.equalsIgnoreCase("walk")){
					/* Use the entire journey's travel time. */
					time += leg.getTravelTime();
				} else if(chosenMode.equalsIgnoreCase("pt1")){ /*TODO Change if "pt1" becomes "bus" */
					Node fromNode = ((NetworkImpl)sc.getNetwork()).getNearestNode(act.getCoord());
					Node toNode = ((NetworkImpl)sc.getNetwork()).getNearestNode(busStops.getClosest(fromNode.getCoord().getX(), fromNode.getCoord().getY()));
					Path path = routerWalk.calcLeastCostPath(fromNode, toNode, act.getEndTime(), null, null);
					time += path.travelTime;
				} else if(chosenMode.equalsIgnoreCase("pt2")){ /*TODO Change if "pt2" becomes "rail" */
					Node fromNode = ((NetworkImpl)sc.getNetwork()).getNearestNode(act.getCoord());
					Node toNode = ((NetworkImpl)sc.getNetwork()).getNearestNode(railStops.getClosest(fromNode.getCoord().getX(), fromNode.getCoord().getY()));
					Path path = routerWalk.calcLeastCostPath(fromNode, toNode, act.getEndTime(), null, null);
					time += path.travelTime;
				}else if(chosenMode.equalsIgnoreCase("taxi")){
					Node fromNode = ((NetworkImpl)sc.getNetwork()).getNearestNode(act.getCoord());
					Node toNode = ((NetworkImpl)sc.getNetwork()).getNearestNode(taxiStops.getClosest(fromNode.getCoord().getX(), fromNode.getCoord().getY()));
					Path path = routerWalk.calcLeastCostPath(fromNode, toNode, act.getEndTime(), null, null);
					time += path.travelTime;
				}
			}
		}
		double avgTime = time / ( (person.getSelectedPlan().getPlanElements().size()-1) / 2);
		if(avgTime <= 15*60){
			return 2.0;
		} else if(avgTime <= 30){
			return 1.0;
		} else{
			return 0.0;
		}
	}
	
	
	private double getAvailableFacilityScore(Person person){
		Coord homeCoord = ((ActivityImpl)person.getSelectedPlan().getPlanElements().get(0)).getCoord();
		
		Collection<ActivityFacility> facilities = facilityQT.getDisk(homeCoord.getX(), homeCoord.getY(), 1000);
		if(facilities.size() <= 5){
			return 0.0;
		} else if(facilities.size() <= 10){
			return 1.0;
		} else{
			return 2.0;
		}
	}
	
	private boolean travelByCar(Plan plan){
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof Leg){
				Leg leg = (Leg) pe;
				if(leg.getMode().equalsIgnoreCase("car") || leg.getMode().equalsIgnoreCase("ride")){
					return true;
				}
			}
		}
		return false;
	}
	
	
	private boolean hasClosePrimaryActivity(Plan plan){
		Coord homeCoord = ((ActivityImpl)plan.getPlanElements().get(0)).getCoord();
		Node homeNode = ((NetworkImpl) sc.getNetwork()).getNearestNode(homeCoord);
		
		ActivityImpl primary = null;
		ActivityImpl secondary = null;
		int index = 0;
		while(primary == null && index < plan.getPlanElements().size()){
			PlanElement pe = plan.getPlanElements().get(index);
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().contains("w") || act.getType().contains("e1")){
					primary = act;
				} else if(act.getType().contains("s") ){
					/* Implication... keep track of FIRST secondary activity. */
					if(secondary == null){
						secondary = act;	
					} 
					index++;
				} else {
					index++;
				}
			} else{
				index++;
			}
		}
		
		if(primary != null){
			Node primaryNode = ((NetworkImpl) sc.getNetwork()).getNearestNode(primary.getCoord());
			Path path = routerWalk.calcLeastCostPath(homeNode, primaryNode, 25200, null, null);
			
			double travelTime = path.travelTime;
			
			/*TODO Remove after debugging. */
			if(Double.isNaN(travelTime)){
				LOG.warn("Travel time is NaN.");
			}
			
			if(travelTime <= 20*60){
				return true;
			}
		} else if (secondary != null){
			/* See if there is a shopping activity close by. */
			Node secondaryNode = ((NetworkImpl) sc.getNetwork()).getNearestNode(secondary.getCoord());
			Path path = routerWalk.calcLeastCostPath(homeNode, secondaryNode, 25200, null, null);
			
			double travelTime = path.travelTime;
			
			/*TODO Remove after debugging. */
			if(Double.isNaN(travelTime)){
				LOG.warn("Travel time is NaN.");
			}
			
			if(travelTime <= 20*60){
				return true;
			}
		}
		
		return false;
	}
	
	
	private boolean hasTransitAccess(Coord coord, QuadTree<Coord> qt){
		Node homeNode = ((NetworkImpl) transitNetwork).getNearestNode(coord);
		Node transitNode = ((NetworkImpl) transitNetwork).getNearestNode(qt.getClosest(coord.getX(), coord.getY()));
		Path path = routerWalk.calcLeastCostPath(homeNode, transitNode, 25200, null, null);
		if(path == null){
			LOG.error("No route found!");
		}
		double travelTime = path.travelTime;
		
		/*TODO Remove after debugging. */
		if(Double.isNaN(travelTime)){
			LOG.warn("Travel time is NaN.");
		}
		
		if(travelTime <= 20*60){
			return true;
		}
		return false;
	}
	
	
	private double getTravelTimeToClosestActivity(Coord coord, String activityType){
		double time = 0.0;
		QuadTree<Coord> qt = new QuadTree<Coord>(
				this.facilityQT.getMinEasting(),
				this.facilityQT.getMinNorthing(),
				this.facilityQT.getMaxEasting(),
				this.facilityQT.getMaxNorthing()
				);
		for(ActivityFacility af : ((ActivityFacilitiesImpl) this.sc.getActivityFacilities()).getFacilitiesForActivityType(activityType).values()) {
			qt.put(af.getCoord().getX(), af.getCoord().getY(), af.getCoord());
		}
		
		/*TODO This can become much more comprehensive */
//		double distance = ((CoordImpl)coord).calcDistance(qt.get(coord.getX(), coord.getY()));
//		double speed = WALK_SPEED;
//		if(activityType.equalsIgnoreCase("work")){
//			speed = CAR_SPEED * 0.75;
//		}
//		time = (distance * STRAIGHT_LINE_FACTOR) / speed;
		
		/* Or using the A*-Euclidean router. */
		Node fromNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(coord);
		Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(qt.getClosest(coord.getX(), coord.getY()));
		Path path = routerDrive.calcLeastCostPath(fromNode, toNode, 25200, null, null);
		time = path.travelTime;		

		/*TODO Remove after debugging. */
		if(Double.isNaN(time)){
			LOG.warn("Travel time is NaN.");
		}

		return time;
	}
	
	private double getTravelTimeToWork(Person person){
		double time = 0.0;
		
		Coord coord = null;
		for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().contains("w")){
					coord = act.getCoord();
				}
			}
		}
		if(coord == null){
			/* People that have indicated that they are employed, but didn't
			 * have a work activity in their chains, are assumed to be home-based
			 * workers. Their travel time to work is assumed to be zero. */
//			LOG.warn("Person should have work: " + person.toString() + "; travel time zero.");
			workAtHomeCounter++;
		} else{
			/* Using the A*-Euclidean router. */
			Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
			Node fromNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(homeCoord);
			Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(coord);
			if(toNode != null){
				Path path = routerDrive.calcLeastCostPath(fromNode, toNode, 25200, null, null);
				time = path.travelTime;		
			}
		}
		
		/*TODO Remove after debugging. */
		if(Double.isNaN(time)){
			LOG.warn("Travel time is NaN.");
		}

		
		return time;
	}
	

	/**
	 * Get travel time (in seconds) from the given {@link Person}'s home 
	 * location (assumed as the first activity in the selected {@link Plan})
	 * to the education facility. If more than one educational facility exist, 
	 * then the <i>farthest</i> one is used. Also, if the educational activity 
	 * is going to <i>school</i> (either at primary or secondary level), that 
	 * is the activity type is <i>e1</i>, then walking is assumed as mode, and 
	 * the travel time is calculated accordingly. If the activity type is 
	 * attending a tertiary education facility (<i>e2</i>), or dropping/
	 * collecting children from school (<i>e3</i>), motorised transport
	 * is assumed and the travel time is calculated using the driving router.
	 * @param person
	 * @return travel time (in seconds) (TODO check that it is indeed seconds)
	 * @see {@link #setupRouterForWalking(PreProcessLandmarks)}
	 * @see {@link #setupRouterForDriving(PreProcessLandmarks)}
	 */
	private double getTravelTimeToEducation(Person person){
		Double time = 0.0;
		String educationType = null;
		
		Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
		Coord coord = null;
		double maxDistance = Double.NEGATIVE_INFINITY;
		
		for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().contains("e")){
					Coord thisCoord = act.getCoord();
					double distance = CoordUtils.calcEuclideanDistance(homeCoord, thisCoord);
					if(distance > maxDistance){
						maxDistance = distance;
						coord = act.getCoord();
						educationType = act.getType();
					}
				}
			}
		}
		if(coord == null){
			/* A child that should be going to school, i.e. older than 6 and 
			 * younger than 16, does not have education ("e1" or "e2") as an
			 * activity. These exceptions are handled by assigning the 
			 * closest education facility to the person, and using that 
			 * facility's coordinate to calculate the "travel time to
			 * education. */
//			LOG.warn("Person should have education: " + person.toString() + "; travel time zero.");
			noEducationCounter++;
			coord = this.schoolQT.getClosest(homeCoord.getX(), homeCoord.getY()).getCoord();
			educationType = "e1";
		} 
		/* Using the A*-Euclidean router. */
		Node fromNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(homeCoord);
		Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(coord);

		Path path = null;
		if(educationType.equalsIgnoreCase("e1")){
			path = routerWalk.calcLeastCostPath(fromNode, toNode, 25200, null, null);
		} else {
			path = routerDrive.calcLeastCostPath(fromNode, toNode, 25200, null, null);
		}

		//			if(toNode != null){
		time = path.travelTime;		
		//			}
		
		/*TODO Remove after debugging. */
		if(Double.isNaN(time)){
			LOG.warn("Travel time is NaN.");
		}
//		LOG.info("Time: " + time);
		
		return time;
	}
	
	
	/**
	 * Get the walking time from the given {@link Person}'s home location (assumed 
	 * to be the first activity in the selected plan) to the closest healthcare 
	 * facility.
	 * 
	 * FIXME JWJ: In July'15 I realized that there are no health care facilities 
	 * in the facilities file we used for the accessibility calculation. The 
	 * short-term plan is to simply return a fixed travel time equal to 20min. 
	 * This will result in everyone having the highest access-to-health-care
	 * score. This, however, must be fixed!!
	 * 
	 * @param person
	 * @return walking time (in seconds) (TODO check that it is indeed seconds)
	 * @see {@link #setupRouterForWalking(PreProcessLandmarks)}
	 */
	private double getTravelTimeToHealthcare(Person person){
		Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
		Node fromNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(homeCoord);
		
		ActivityFacility healthCareFacility = healthcareQT.getClosest(homeCoord.getX(), homeCoord.getY());
		if(healthCareFacility != null){
			Coord healthcareCoord = healthCareFacility.getCoord();
			Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(healthcareCoord);
			
			Path path = routerWalk.calcLeastCostPath(fromNode, toNode, 25200, null, null);
			
			return path.travelTime;		
		}else{
			return 20.0*60.0;
		}
	}
	
	
	/**
	 * Get the average walking time from the given {@link Person}'s home location 
	 * (assumed to be the first activity in the selected plan) to the five closest 
	 * shopping and leisure facilities.
	 * @param person
	 * @return walking time (in seconds) (TODO check that it is indeed seconds)
	 * @see {@link #setupRouterForWalking(PreProcessLandmarks)}
	 */
	private double getTravelTimeToShopping(Person person){
//		Logger.getLogger( this.getClass() ).fatal("other than stated in the javadoc of this method, the code "
//				+ "actually returns the same result as getTravelTimeToHealthcare(...).  kai/dz, mar'15") ;
//		System.exit(-1);
		
		final Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
		Node fromNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(homeCoord);
		
		/* New code. ====================== */
		double searchDistance = 100.0;
		int searchTries = 0;

		Collection<ActivityFacility> facilitiesInSearchArea = new HashSet<ActivityFacility>();
		while(facilitiesInSearchArea.size() < 5 && searchTries < 1000){
			facilitiesInSearchArea = shoppingQT.getDisk(homeCoord.getX(), homeCoord.getY(), searchDistance);
			
			searchTries++;
			searchDistance *= 1.5;
		}
		if(searchTries >= 1000){
			LOG.error("Could not find 5 shopping facilities within 1000 tries!!");
			if(facilitiesInSearchArea.size() == 0){
				LOG.fatal("No facilities found. Terminating.");
				System.exit(-1);
			} else{
				LOG.error("Using " + facilitiesInSearchArea.size() + " instead.");
			}
		}

		/* Create the comparator that will sort the facilities from closest to farthest. */
		Comparator<ActivityFacility> comparator = new Comparator<ActivityFacility>() {
			
			@Override
			public int compare(ActivityFacility o1, ActivityFacility o2) {
				Double d1 = CoordUtils.calcEuclideanDistance(homeCoord, o1.getCoord());
				Double d2 = CoordUtils.calcEuclideanDistance(homeCoord, o2.getCoord());
				return d1.compareTo(d2);
			}
		};
		Collection<ActivityFacility> sortedFacilities = new TreeSet<ActivityFacility>(comparator);
		sortedFacilities.addAll(facilitiesInSearchArea);
		
		int numberOfElements = 0;
		double total = 0.0;
		Iterator<ActivityFacility> iterator = sortedFacilities.iterator();
		while(iterator.hasNext() && numberOfElements++ < 5){
			ActivityFacility shop = iterator.next();
			Node shopNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(shop.getCoord());
			Path shopPath = routerWalk.calcLeastCostPath(fromNode, shopNode, 25200, null, null);
			
			total += shopPath.travelTime;
		}
		double meanTravelTime = total / ((double)numberOfElements);
		return meanTravelTime;
		/* New code. ====================== */
		
		/* Old code. ====================== */
//		Coord healthcareCoord = healthcareQT.get(homeCoord.getX(), homeCoord.getY()).getCoord();
//		Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(healthcareCoord);
//		Path path = routerWalk.calcLeastCostPath(fromNode, toNode, 25200, null, null);
//		return path.travelTime;		
		/* Old code. ====================== */
	}

	
	
	private int getAccessibilityClass(Person person){
	
		/* If the person has a custom attribute for school with value "School" or
		 * "PreSchool", s/he is school-going. 
		 * TODO Can challenge this: if the person is younger than 16, s/he SHOULD 
		 * be going to school... and we treat them as if they WERE school-going. 
		 * TODO Changed school to those that HAVE education (e1 or e2) as activity */
//		boolean attendSchool = ((String)person.getCustomAttributes().get("school")).contains("School")  ||
//				person.getAge() < 16 ? true : false;
		boolean attendSchool = activityChainContainsEducation(person.getSelectedPlan())  ||
				(PersonUtils.getAge(person) >= 6 && PersonUtils.getAge(person) < 16) ? true : false;
		if(attendSchool){
			return 1;
		}
		
		boolean isWorking = PersonUtils.isEmployed(person) || activityChainContainsWork(person.getSelectedPlan()) ? true : false;
		boolean isAccompanyingScholar = activityChainContainsDroppingScholar(person.getSelectedPlan());
		
		if(isWorking){
			if(isAccompanyingScholar){
				return 2;
			} else{
				return 3;
			}
		} else{
			if(isAccompanyingScholar){
				return 4;
			} else{
				return 5;
			}
		}
	}
	
	
	private boolean activityChainContainsWork(Plan plan){
		boolean hasWork = false;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().contains("w")){
					hasWork = true;
				}
			}
		}
		return hasWork;
	}
	
	
	private boolean activityChainContainsEducation(Plan plan){
		boolean hasEducation = false;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().equalsIgnoreCase("e1") || act.getType().equalsIgnoreCase("e2")){
					hasEducation = true;
				}
			}
		}
		return hasEducation;
	}

	
	
	private boolean activityChainContainsDroppingScholar(Plan plan){
		boolean accompanyingScholar = false;
		for(PlanElement pe : plan.getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().equalsIgnoreCase("e3")){
					accompanyingScholar = true;
				}
			}
		}
		return accompanyingScholar;
	}
	

}
