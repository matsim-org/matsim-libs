package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.api.basic.v01.population.BasicPlanElement;
import org.matsim.core.api.experimental.population.Population;
import org.matsim.core.api.facilities.ActivityFacilities;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.world.World;

import playground.anhorni.locationchoice.preprocess.facilities.FacilityQuadTreeBuilder;
import playground.anhorni.locationchoice.preprocess.helper.QuadTreeRing;


public class ActivityDifferentiationShop {
	
	private Population plans = new PopulationImpl();
	private final static Logger log = Logger.getLogger(ActivityDifferentiationShop.class);
	
	// from Microcensus for all modes
	private double groceryShare = 0.66;
	private int numberOfShopActs;
	
	private ActivityFacilities facilitiesActDiff;
	private String facilitiesActDifffilePath = "input/facilities/facilitiesActDiff.xml.gz";
	
	public ActivityDifferentiationShop(Population plans) {
		this.plans = plans;
	}
	
	public void run() {
		this.init();
		this.assignGroceryAndNonGrocery();
		this.assignFacility();
	}	
	
	private void init() {	
		this.numberOfShopActs = this.getNumberOfShopActs();
		
		log.info("reading facilitiesActDiff ...");
		World world = Gbl.getWorld();
		this.facilitiesActDiff =(ActivityFacilities)world.createLayer(ActivityFacilities.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilitiesActDiff).readFile(facilitiesActDifffilePath);		
	}
	
	private int getNumberOfShopActs() {
		int numberOfShopActs = 0;
		
		Iterator<PersonImpl> person_it = this.plans.getPersons().values().iterator();
		while (person_it.hasNext()) {
			PersonImpl person = person_it.next();
			
			// intitially only one plan is available
			if (person.getPlans().size() > 1) {
				log.error("More than one plan for person: " + person.getId());
			}
			PlanImpl selectedPlan = person.getSelectedPlan();
						
			final List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);
				if (act.getType().startsWith("shop")) {
					numberOfShopActs++;
				}
			}				
		}
		return numberOfShopActs;
	}
	
	private void assignGroceryAndNonGrocery() {
		
		int assignedNumberOf_GroceryActs = 0;
		int assignedNumberOf_NonGroceryActs = 0;
		
		Iterator<PersonImpl> person_it = this.plans.getPersons().values().iterator();
		while (person_it.hasNext()) {
			PersonImpl person = person_it.next();
			
			// intitially only one plan is available
			if (person.getPlans().size() > 1) {
				log.error("More than one plan for person: " + person.getId());
			}
			PlanImpl selectedPlan = person.getSelectedPlan();
						
			final List<? extends BasicPlanElement> actslegs = selectedPlan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);
				if (act.getType().startsWith("shop")) {
					double random = MatsimRandom.getRandom().nextDouble();					
					if (random <= 0.66) {
						if (assignedNumberOf_GroceryActs < groceryShare * numberOfShopActs) {
							act.setType("shop_grocery");
							assignedNumberOf_GroceryActs++;
						}
						else {
							act.setType("shop_nongrocery");
							assignedNumberOf_NonGroceryActs++;
						}
					}
					else {
						if (assignedNumberOf_NonGroceryActs < (1.0 - groceryShare) * numberOfShopActs) {
							act.setType("shop_nongrocery");
							act.setFacility(null);
							assignedNumberOf_NonGroceryActs++;
						}
						else {
							act.setType("shop_grocery");
							act.setFacility(null);
							assignedNumberOf_GroceryActs++;
						}
					}	
				}
			}				
		}
		log.info("Number of shopping activities:\t" + this.numberOfShopActs);
		log.info("Number of grocery shopping acts:\t" + assignedNumberOf_GroceryActs);
		log.info("Number of non-grocery acts:\t" + assignedNumberOf_NonGroceryActs);
		log.info("Share:\t"+ (100.0* assignedNumberOf_GroceryActs / this.numberOfShopActs));
	}
	
	
	private void assignFacility() {
		
		NOGATypes nogatypes = new NOGATypes();
		
		List<ActivityFacility> groceryFacilities = new Vector<ActivityFacility>();
		for (int i = 0; i < nogatypes.shopGrocery.length; i++) {
			groceryFacilities.addAll(this.facilitiesActDiff.getFacilitiesForActivityType(nogatypes.shopGrocery[i]).values());
		}
				
		List<ActivityFacility> nonGroceryFacilities = new Vector<ActivityFacility>();
		for (int i = 0; i < nogatypes.shopNonGrocery.length; i++) {
			nonGroceryFacilities.addAll(this.facilitiesActDiff.getFacilitiesForActivityType(nogatypes.shopNonGrocery[i]).values());
		}

		FacilityQuadTreeBuilder builder = new FacilityQuadTreeBuilder();
		QuadTree<ActivityFacility> groceryTree = builder.buildFacilityQuadTree("shop_grocery", groceryFacilities);
		QuadTree<ActivityFacility> nongroceryTree = builder.buildFacilityQuadTree("shop_nongrocery", nonGroceryFacilities);
		
		AssignLocations assignShops = new AssignLocations((QuadTreeRing<ActivityFacility>)nongroceryTree);
		assignShops.run(this.plans, "shop_nongrocery");
		
		assignShops = new AssignLocations((QuadTreeRing<ActivityFacility>) groceryTree);
		assignShops.run(this.plans, "shop_grocery");	
	}
	
	public Population getPlans() {
		return plans;
	}

	public void setPlans(Population plans) {
		this.plans = plans;
	}
}
