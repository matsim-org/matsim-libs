package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.List;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.FacilitiesReaderMatsimV1;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.locationchoice.utils.QuadTreeRing;
import org.matsim.world.World;

import playground.anhorni.locationchoice.preprocess.facilities.FacilityQuadTreeBuilder;


public class ActivityDifferentiationShop {
	
	private PopulationImpl plans = new PopulationImpl();
	private final static Logger log = Logger.getLogger(ActivityDifferentiationShop.class);
	
	// from Microcensus for all modes
	private double groceryShare = 0.66;
	private int numberOfShopActs;
	
	private ActivityFacilitiesImpl facilitiesActDiff;
	private String facilitiesActDifffilePath = "input/facilities/facilitiesActDiff.xml.gz";
	
	public ActivityDifferentiationShop(PopulationImpl plans) {
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
		this.facilitiesActDiff =(ActivityFacilitiesImpl)world.createLayer(ActivityFacilitiesImpl.LAYER_TYPE, null);
		new FacilitiesReaderMatsimV1(this.facilitiesActDiff).readFile(facilitiesActDifffilePath);		
	}
	
	private int getNumberOfShopActs() {
		int numberOfShopActs = 0;
		
		for (Person person : this.plans.getPersons().values()) {
			
			// intitially only one plan is available
			if (person.getPlans().size() > 1) {
				log.error("More than one plan for person: " + person.getId());
			}
			PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan();
						
			final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
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
		
		for (Person person : this.plans.getPersons().values()) {
			
			// intitially only one plan is available
			if (person.getPlans().size() > 1) {
				log.error("More than one plan for person: " + person.getId());
			}
			PlanImpl selectedPlan = (PlanImpl)person.getSelectedPlan();
						
			final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
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
		
		List<ActivityFacilityImpl> groceryFacilities = new Vector<ActivityFacilityImpl>();
		for (int i = 0; i < nogatypes.shopGrocery.length; i++) {
			groceryFacilities.addAll(this.facilitiesActDiff.getFacilitiesForActivityType(nogatypes.shopGrocery[i]).values());
		}
				
		List<ActivityFacilityImpl> nonGroceryFacilities = new Vector<ActivityFacilityImpl>();
		for (int i = 0; i < nogatypes.shopNonGrocery.length; i++) {
			nonGroceryFacilities.addAll(this.facilitiesActDiff.getFacilitiesForActivityType(nogatypes.shopNonGrocery[i]).values());
		}

		FacilityQuadTreeBuilder builder = new FacilityQuadTreeBuilder();
		QuadTree<ActivityFacilityImpl> groceryTree = builder.buildFacilityQuadTree("shop_grocery", groceryFacilities);
		QuadTree<ActivityFacilityImpl> nongroceryTree = builder.buildFacilityQuadTree("shop_nongrocery", nonGroceryFacilities);
		
		AssignLocations assignShops = new AssignLocations((QuadTreeRing<ActivityFacilityImpl>)nongroceryTree);
		assignShops.run(this.plans, "shop_nongrocery");
		
		assignShops = new AssignLocations((QuadTreeRing<ActivityFacilityImpl>) groceryTree);
		assignShops.run(this.plans, "shop_grocery");	
	}
	
	public PopulationImpl getPlans() {
		return plans;
	}

	public void setPlans(PopulationImpl plans) {
		this.plans = plans;
	}
}
