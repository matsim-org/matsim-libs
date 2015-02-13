package playground.staheale.preprocess;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.MatsimFacilitiesReader;

public class SetSecondaryLocation  {

	private static Logger log = Logger.getLogger(SetSecondaryLocation.class);
	private QuadTree<ActivityFacilityImpl> leisureQuadTree;
	private QuadTree<ActivityFacilityImpl> educationQuadTree;
	private QuadTree<ActivityFacilityImpl> shopQuadTree;
	private double radius;

	private static final String H = "home";
	private static final String W = "work";
	private static final String L = "leisure";
	private static final String S = "shop";
	private static final String E = "education";
	private static final String EDUCATION = "education";
	private static final String LEISURE = "leisure";
	private static final String SHOP = "shop";
	private static final String SHOPACT = "shopping";

	Coord home_coord = null;
	Coord work_coord = null;
	boolean educ = false;
	boolean work = false;
	boolean secondaryAct = false;
	int countPop = 0;

	public SetSecondaryLocation() {
		super();		
	}

	public static void main(String[] args) throws IOException {
		SetSecondaryLocation setSecondaryLocation = new SetSecondaryLocation();
		setSecondaryLocation.run();
	}

	public void run() {
		Scenario sc = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = sc.getPopulation();
				
		//////////////////////////////////////////////////////////////////////
		// read in population

		log.info("Reading plans...");	
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(sc); 
		PlansReader.readFile("./input/population2030combined_without_facilities.xml.gz");
		log.info("Reading plans...done.");
		log.info("Population size is " +population.getPersons().size());

		//////////////////////////////////////////////////////////////////////
		//read in facilities

		MatsimFacilitiesReader FacReader = new MatsimFacilitiesReader((ScenarioImpl) sc);  
		log.info("Reading facilities xml file... ");
		FacReader.readFile("./input/facilities2012.xml.gz");
		log.info("Reading facilities xml file...done.");
		ActivityFacilities facilities = sc.getActivityFacilities();
		log.info("Number of facilities: " +facilities.getFacilities().size());

		//////////////////////////////////////////////////////////////////////
		//build quadTrees


		TreeMap<Id<ActivityFacility>, ActivityFacility> leisureFacilities = facilities.getFacilitiesForActivityType("leisure");
		log.info("Leisure facilities: " +leisureFacilities.size());
		TreeMap<Id<ActivityFacility>, ActivityFacility> educationFacilities = facilities.getFacilitiesForActivityType("education");
		log.info("Education facilities: " +educationFacilities.size());
		TreeMap<Id<ActivityFacility>, ActivityFacility> shopFacilities = facilities.getFacilitiesForActivityType("shop");
		log.info("Shop facilities: " +shopFacilities.size());


		leisureQuadTree = buildLeisureQuadTree(leisureFacilities);
		log.info("leisureQuadTree size: " +leisureQuadTree.size());

		educationQuadTree = buildEducationQuadTree(educationFacilities);
		log.info("educationQuadTree size: " +educationQuadTree.size());

		shopQuadTree = buildShopQuadTree(shopFacilities);
		log.info("shopQuadTree size: " +shopQuadTree.size());

		//////////////////////////////////////////////////////////////////////
		//set location

		PopulationWriter pw = new PopulationWriter(population, null);
		pw.writeStartPlans("./output/population2030combined.xml.gz");

		for (Person p : population.getPersons().values()) {
			if (p.getSelectedPlan() != null) {
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl) pe;

						if (E.equals(act.getType())){
							educ = true;
						}
						else if (H.equals(act.getType())) {
							if (act.getCoord() == null) {
								throw new RuntimeException("Person id=" + p.getId() + " has no home coord!");
							}
							home_coord = act.getCoord();
						}
						else if (W.equals(act.getType())){
							work_coord = act.getCoord();
							work = true;
						}
						else if (L.equals(act.getType()) || SHOPACT.equals(act.getType())){
							secondaryAct = true;
						}
					}
				}

				// case 0: activity type education --> closest to home location
				if (educ != false) {
					for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
						if (pe instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl) pe;
							if (E.equals(act.getType())){
								Double x = home_coord.getX();
								Double y = home_coord.getY();
								ActivityFacility closestEducationFacility = educationQuadTree.get(x,y);
								act.setFacilityId(closestEducationFacility.getId());
								act.setCoord(closestEducationFacility.getCoord()); 
							}
						}
					}
				}

				// case 1: home/secondary activity - secondary activity - home/secondary activity --> radius around home location
				if (secondaryAct != false && work != true){
					for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
						if (pe instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl) pe;
							if (SHOPACT.equals(act.getType())) {
								radius = 5000.0; // arbitrarily set to 5km
								ActivityFacilityImpl f = getFacility(home_coord,radius,S);
								act.setCoord(f.getCoord());
								act.setFacilityId(f.getId());
							}
							else if (L.equals(act.getType())){
								radius = 20000.0; // arbitrarily set to 20km
								ActivityFacilityImpl f = getFacility(home_coord,radius,act.getType());
								act.setCoord(f.getCoord());
								act.setFacilityId(f.getId());
							}

						}
					}
				}

				// case 2: working is the previous or next activity --> radius dependent on distance home - work (according to code of anhorni)
				//
				//           c1               c2
				//    home ---|---|---|---|---|--- work
				//             \             /
				//              \ r       r /
				//               \         /
				//
				else if (secondaryAct != false && work != false){
					for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
						if (pe instanceof ActivityImpl) {
							ActivityImpl act = (ActivityImpl) pe;
							if (L.equals(act.getType()) || SHOPACT.equals(act.getType())) {
								double dx = work_coord.getX() - home_coord.getX();
								double dy = work_coord.getY() - home_coord.getY();
								radius = Math.max(Math.sqrt(dx*dx+dy*dy)/3.0,1);
								dx = dx/6.0;
								dy = dy/6.0;
								CoordImpl coord1 = new CoordImpl(home_coord.getX()+dx,home_coord.getY()+dy);
								CoordImpl coord2 = new CoordImpl(work_coord.getX()-dx,work_coord.getY()+dy);
								ActivityFacilityImpl f = null;
								if (SHOPACT.equals(act.getType())) {
									f = getFacility(coord1,coord2,radius,S);
								}
								else {
									f = getFacility(coord1,coord2,radius,act.getType());
								}
								act.setCoord(f.getCoord());
								act.setFacilityId(f.getId());
							}
						}
					}
				}
			}
			pw.writePerson(p);
			countPop += 1;
			educ = false;
			work = false;
			secondaryAct = false;
		}	
		pw.writeEndPlans();
		log.info("Writing plans...done");
		log.info("final population size is: " +countPop);
		
	}

	//////////////////////////////////////////////////////////////////////
	//build methods

	private QuadTree<ActivityFacilityImpl> buildLeisureQuadTree(TreeMap<Id<ActivityFacility>, ActivityFacility> leisureFacilities) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : leisureFacilities.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");

		QuadTree<ActivityFacilityImpl> leisureQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : leisureFacilities.values()) {
			leisureQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacilityImpl) f);
		}
		return leisureQuadTree;
	}

	private QuadTree<ActivityFacilityImpl> buildEducationQuadTree(TreeMap<Id<ActivityFacility>, ActivityFacility> educationFacilities) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : educationFacilities.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");

		QuadTree<ActivityFacilityImpl> educationQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : educationFacilities.values()) {
			educationQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacilityImpl) f);
		}
		return educationQuadTree;
	}

	private QuadTree<ActivityFacilityImpl> buildShopQuadTree(TreeMap<Id<ActivityFacility>, ActivityFacility> shopFacilities) {
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;

		for (final ActivityFacility f : shopFacilities.values()) {
			if (f.getCoord().getX() < minx) { minx = f.getCoord().getX(); }
			if (f.getCoord().getY() < miny) { miny = f.getCoord().getY(); }
			if (f.getCoord().getX() > maxx) { maxx = f.getCoord().getX(); }
			if (f.getCoord().getY() > maxy) { maxy = f.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("        xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");

		QuadTree<ActivityFacilityImpl> shopQuadTree = new QuadTree<ActivityFacilityImpl>(minx, miny, maxx, maxy);
		for (final ActivityFacility f : shopFacilities.values()) {
			shopQuadTree.put(f.getCoord().getX(),f.getCoord().getY(),(ActivityFacilityImpl) f);
		}
		return shopQuadTree;
	}

	//////////////////////////////////////////////////////////////////////
	// private methods

	private final QuadTree<ActivityFacilityImpl> getFacilities(String act_type) {
		if (E.equals(act_type)) { return educationQuadTree; }
		else if (S.equals(act_type)) { return shopQuadTree; }
		else if (L.equals(act_type)) { return leisureQuadTree; }
		else { throw new RuntimeException("act_type=" + act_type + " not allowed!"); }
	}

	private final String getFacilityActType(String act_type) {
		if (E.equals(act_type)) { return EDUCATION; }
		else if (S.equals(act_type)) { return SHOP; }
		else if (L.equals(act_type)) { return LEISURE; }
		else { throw new RuntimeException("act_type=" + act_type + " not allowed!"); }
	}

	private final ActivityFacilityImpl getFacility(Collection<ActivityFacilityImpl> fs, String act_type) {
		act_type = getFacilityActType(act_type);
		int i = 0;
		int[] dist_sum = new int[fs.size()];
		Iterator<ActivityFacilityImpl> f_it = fs.iterator();
		ActivityFacilityImpl f = f_it.next();
		ActivityOptionImpl activityOption = (ActivityOptionImpl) f.getActivityOptions().get(act_type);
		dist_sum[i] = (int) activityOption.getCapacity();
		if ((dist_sum[i] == 0) || (dist_sum[i] == Integer.MAX_VALUE)) {
			dist_sum[i] = 1;
			activityOption.setCapacity((double) 1);
		}
		while (f_it.hasNext()) {
			f = f_it.next();
			i++;
			int val = (int) activityOption.getCapacity();
			if ((val == 0) || (val == Integer.MAX_VALUE)) {
				val = 1;
				activityOption.setCapacity((double) 1);
			}
			dist_sum[i] = dist_sum[i-1] + val;
		}

		int r = MatsimRandom.getRandom().nextInt(dist_sum[fs.size()-1]);

		i=-1;
		f_it = fs.iterator();
		while (f_it.hasNext()) {
			f = f_it.next();
			i++;
			if (r < dist_sum[i]) {
				return f;
			}
		}
		throw new RuntimeException("It should never reach this line!");
	}

	private final ActivityFacilityImpl getFacility(Coord coord, double radius, String act_type) {
		Collection<ActivityFacilityImpl> fs = getFacilities(act_type).get(coord.getX(),coord.getY(),radius);
		if (fs.isEmpty()) {
			if (radius > 200000) { throw new RuntimeException("radius>200'000 meters and still no facility found!"); }
			return getFacility(coord,2.0*radius,act_type);
		}
		return getFacility(fs,act_type);
	}

	private final ActivityFacilityImpl getFacility(CoordImpl coord1, CoordImpl coord2, double radius, String act_type) {
		Collection<ActivityFacilityImpl> fs = getFacilities(act_type).get(coord1.getX(),coord1.getY(),radius);
		fs.addAll(getFacilities(act_type).get(coord2.getX(),coord2.getY(),radius));
		if (fs.isEmpty()) {
			if (radius > 200000) { throw new RuntimeException("radius>200'000 meters and still no facility found!"); }
			return getFacility(coord1,coord2,2.0*radius,act_type);
		}
		return getFacility(fs,act_type);
	}
}