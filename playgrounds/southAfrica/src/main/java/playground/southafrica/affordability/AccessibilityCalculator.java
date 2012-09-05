package playground.southafrica.affordability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.AStarEuclidean;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.vehicles.Vehicle;

import playground.southafrica.utilities.Header;

public class AccessibilityCalculator {
	private final static Logger LOG = Logger.getLogger(AccessibilityCalculator.class);
	
	private ScenarioImpl sc;
	private Households hhs;
	private QuadTree<ActivityFacility> facilityQT;
	private QuadTree<ActivityFacility> schoolQT;
	private QuadTree<ActivityFacility> healthcareQT;
	private QuadTree<ActivityFacility> shoppingQT;
	private Map<Integer, String> classDescription = new TreeMap<Integer, String>();
	private AStarEuclidean routerDrive;
	private AStarEuclidean routerWalk;
	private static Map<String, Integer> activityOptions = new TreeMap<String, Integer>();
	
	/*TODO Remove after validation. */
	private List<Integer> numberInClasses;
	private static int workAtHomeCounter = 0;
	private static int noEducationCounter = 0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(AccessibilityCalculator.class.toString(), args);
		
		Config config = ConfigUtils.createConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
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
		
		/* Read population attributes */
		String personAttributesFile = args[2];
		ObjectAttributes oa = new ObjectAttributes();
		ObjectAttributesXmlReader oar = new ObjectAttributesXmlReader(oa);
		oar.parse(personAttributesFile);
		/* Add attributes to population. */
		for(Id id : sc.getPopulation().getPersons().keySet()){
			String hhId = (String) oa.getAttribute(id.toString(), "householdId");
			sc.getPopulation().getPersons().get(id).getCustomAttributes().put("householdId", new IdImpl(hhId));
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
		ActivityFacilities afs = new ActivityFacilitiesImpl();
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
		MatsimNetworkReader mnr = new MatsimNetworkReader(sc);
		mnr.readFile(networkFile);
		
		/* Read the transit schedule. */
		String transitFile = args[5];
		TransitScheduleReader tsr = new TransitScheduleReader(sc);
		tsr.readFile(transitFile);
		
		AccessibilityCalculator ac = new AccessibilityCalculator(sc, hhs);
		ac.testRun();
		
		/* Report some statistics. */
		LOG.info("----------------------------------------------------");
		LOG.info("Exceptions handled:");
		LOG.info("   Number of people working from home: " + workAtHomeCounter);
		LOG.info("   Number of educationless scholars  : " + noEducationCounter);
		
		Header.printFooter();
	}
	
	
	public AccessibilityCalculator(Scenario scenario, Households households) {
		this.sc = (ScenarioImpl) scenario;
		this.hhs = households;
		
		/* Build QuadTree of facilities. */
		Map<Id, ActivityFacility> facilities = sc.getActivityFacilities().getFacilities();
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
			public double getLinkTravelTime(Link link, double time) {
				return link.getLength() / (3 / (60 * 60));
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
			public double getLinkTravelTime(Link link, double time) {
				return (link.getLength() / link.getFreespeed()) * 1.2;
			}
		};
		routerDrive = new AStarEuclidean(sc.getNetwork(), pp, travelTimeDrive);
	}
	
	
	public void testRun(){
		LOG.info("Start running...");
		Counter counter = new Counter("   person # ");
		for(Person person : this.sc.getPopulation().getPersons().values()){
			calculateAccessibility((PersonImpl)person);
			counter.incCounter();
		}
		counter.printCounter();
		
		LOG.info("----------------------------------------------------");
		LOG.info("Number of persons in different classes:");
		for(int i = 0; i < numberInClasses.size(); i++){
			LOG.info("   " + i + ": " + numberInClasses.get(i));
		}
	}	
	
	
	public double calculateAccessibility(PersonImpl person){
	
		getMobilityScore(person);
		return 0.0;
	}
	
	
	private double getMobilityScore(PersonImpl person){
		double score = 0;
		int accessibilityClass = getAccessibilityClass(person);
		
		/*TODO Remove after validation */
		int oldValue = numberInClasses.get(accessibilityClass);
		numberInClasses.set(accessibilityClass, oldValue+1);
		
		CoordImpl homeCoord = (CoordImpl) ((ActivityImpl)person.getSelectedPlan().getPlanElements().get(0)).getCoord();
		double tt_work;
		double tt_education;
		double tt_healthcare;
		double tt_shopping;
		
		switch (accessibilityClass) {
		case 1:
			tt_education = getTravelTimeToEducation(person);
			tt_healthcare = getTravelTimeToHealthcare(person);
			tt_shopping = getTravelTimeToShopping(person);
			break;
		case 2:
			tt_work = getTravelTimeToWork(person);
			tt_education = getTravelTimeToEducation(person);
			tt_healthcare = getTravelTimeToHealthcare(person);
			
			break;
		case 3:
			tt_work = getTravelTimeToWork(person);
			tt_healthcare = getTravelTimeToHealthcare(person);
			
			break;
		case 4:
			tt_education = getTravelTimeToEducation(person);
			tt_healthcare = getTravelTimeToHealthcare(person);
			
			break;
		case 5:
			tt_healthcare = getTravelTimeToHealthcare(person);
			
			break;			
		default:
			break;
		}
		
		return score;
	}
	
	
	private double getTravelTimeToClosestActivity(Coord coord, String activityType){
		double time = 0.0;
		QuadTree<Coord> qt = new QuadTree<Coord>(
				this.facilityQT.getMinEasting(),
				this.facilityQT.getMinNorthing(),
				this.facilityQT.getMaxEasting(),
				this.facilityQT.getMaxNorthing()
				);
		for(ActivityFacility af : this.sc.getActivityFacilities().getFacilitiesForActivityType(activityType).values()){
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
		Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(qt.get(coord.getX(), coord.getY()));
		Path path = routerDrive.calcLeastCostPath(fromNode, toNode, Time.UNDEFINED_TIME, null, null);
		time = path.travelTime;		
		
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
				Path path = routerDrive.calcLeastCostPath(fromNode, toNode, Time.UNDEFINED_TIME, null, null);
				time = path.travelTime;		
			}
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
		double time = 0.0;
		String educationType = null;
		
		CoordImpl homeCoord = (CoordImpl) ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
		Coord coord = null;
		double maxDistance = Double.NEGATIVE_INFINITY;
		
		for(PlanElement pe : person.getSelectedPlan().getPlanElements()){
			if(pe instanceof ActivityImpl){
				ActivityImpl act = (ActivityImpl) pe;
				if(act.getType().contains("e")){
					Coord thisCoord = act.getCoord();
					double distance = homeCoord.calcDistance(thisCoord);
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
			coord = this.schoolQT.get(homeCoord.getX(), homeCoord.getY()).getCoord();
			educationType = "e1";
		} 
		/* Using the A*-Euclidean router. */
		Node fromNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(homeCoord);
		Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(coord);

		Path path = null;
		if(educationType.equalsIgnoreCase("e1")){
			path = routerWalk.calcLeastCostPath(fromNode, toNode, Time.UNDEFINED_TIME, null, null);
		} else {
			path = routerDrive.calcLeastCostPath(fromNode, toNode, Time.UNDEFINED_TIME, null, null);
		}

		//			if(toNode != null){
		time = path.travelTime;		
		//			}
		
		return time;
	}
	
	
	/**
	 * Get the walking time from the given {@link Person}'s home location (assumed 
	 * to be the first activity in the selected plan) to the closest healthcare 
	 * facility.
	 * @param person
	 * @return walking time (in seconds) (TODO check that it is indeed seconds)
	 * @see {@link #setupRouterForWalking(PreProcessLandmarks)}
	 */
	private double getTravelTimeToHealthcare(Person person){
		Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
		Node fromNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(homeCoord);
		
		Coord healthcareCoord = healthcareQT.get(homeCoord.getX(), homeCoord.getY()).getCoord();
		Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(healthcareCoord);
		
		Path path = routerWalk.calcLeastCostPath(fromNode, toNode, Time.UNDEFINED_TIME, null, null);
		
		return path.travelTime;		
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
		Coord homeCoord = ((ActivityImpl) person.getSelectedPlan().getPlanElements().get(0)).getCoord();
		Node fromNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(homeCoord);
		
		Coord healthcareCoord = healthcareQT.get(homeCoord.getX(), homeCoord.getY()).getCoord();
		Node toNode = ((NetworkImpl)this.sc.getNetwork()).getNearestNode(healthcareCoord);
		
		Path path = routerWalk.calcLeastCostPath(fromNode, toNode, Time.UNDEFINED_TIME, null, null);
		
		return path.travelTime;		
	}

	
	
	private int getAccessibilityClass(PersonImpl person){
	
		/* If the person has a custom attribute for school with value "School" or
		 * "PreSchool", s/he is school-going. 
		 * TODO Can challenge this: if the person is younger than 16, s/he SHOULD 
		 * be going to school... and we treat them as if they WERE school-going. 
		 * TODO Changed school to those that HAVE education (e1 or e2) as activity */
//		boolean attendSchool = ((String)person.getCustomAttributes().get("school")).contains("School")  ||
//				person.getAge() < 16 ? true : false;
		boolean attendSchool = activityChainContainsEducation(person.getSelectedPlan())  ||
				(person.getAge() >= 6 && person.getAge() < 16) ? true : false;
		if(attendSchool){
			return 1;
		}
		
		boolean isWorking = person.isEmployed() || activityChainContainsWork(person.getSelectedPlan()) ? true : false;
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
