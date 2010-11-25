package playground.ciarif.retailers.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;

import playground.ciarif.retailers.IO.FileRetailerReader;
import playground.ciarif.retailers.preprocess.AssignFacility;

public class ActivityDifferentiator {

		private final static Logger log = Logger.getLogger(ActivityDifferentiator.class);

		// from Microcensus for all modes
		private final double groceryShare = 0.66;
		private int numberOfShopActs;

		private final ScenarioImpl scenario;

		//private ActivityFacilitiesImpl facilitiesActDiff;
		private final String retailersFacilitiesPath = "../../matsim/input/triangle/Retailers.txt";
		
		public ActivityDifferentiator(ScenarioImpl scenario) {
			this.scenario = scenario;
		}

		public void run() {
			this.init();
			this.modifyFacilities();
			this.assignGroceryAndNonGrocery();
			
		}

		

		private void init() {
			this.numberOfShopActs = this.getNumberOfShopActs();
		}

		private int getNumberOfShopActs() {
			int numberOfShopActs = 0;

			for (Person person : this.scenario.getPopulation().getPersons().values()) {

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
		
		private void modifyFacilities() {
			
			log.info("  reading file " + retailersFacilitiesPath);
			final FileRetailerReader retailersReader = new FileRetailerReader(this.retailersFacilitiesPath) ;
			ArrayList<Id> retailersFacilities = new ArrayList<Id>();
			retailersFacilities.addAll(retailersReader.readRetailersFacilities());
			for (ActivityFacility  af:this.scenario.getActivityFacilities().getFacilities().values()) {
				ActivityFacilityImpl afi = (ActivityFacilityImpl)af; 
				log.info("The facility " + afi.getId() + " is being checked now, it allows the following activities: " + afi.getActivityOptions().toString());
				if (afi.getActivityOptions().containsKey("shop")){
					log.info("Modifying the facility " + afi.getId());
					if (retailersFacilities.contains(af.getId())) {
						afi.getActivityOptions().remove("shop");
						afi.createActivityOption("shop_grocery");
					}
					else {
						afi.getActivityOptions().remove("shop");
						log.info("the shop activity for the facility " + afi.getId()+ " has been removed");
						afi.createActivityOption("shop_nongrocery");
					}
				}
						
			}
		
		}

		private void assignGroceryAndNonGrocery() {

			int assignedNumberOf_GroceryActs = 0;
			int assignedNumberOf_NonGroceryActs = 0;

			for (Person person : this.scenario.getPopulation().getPersons().values()) {

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
								assignFacility(act);
								act.setLinkId(null);
								assignedNumberOf_GroceryActs++;
							}
							else {
								act.setType("shop_nongrocery");
								assignFacility(act);
								act.setLinkId(null);
								assignedNumberOf_NonGroceryActs++;
							}
						}
						else {
							if (assignedNumberOf_NonGroceryActs < (1.0 - groceryShare) * numberOfShopActs) {
								act.setType("shop_nongrocery");
								assignFacility(act);
								act.setLinkId(null);
								assignedNumberOf_NonGroceryActs++;
							}
							else {
								act.setType("shop_grocery");
								//act.setFacilityId(null);
								assignFacility(act);
								act.setLinkId(null);
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

		private void assignFacility(ActivityImpl act) {
			AssignFacility assigner = new AssignFacility(act, scenario.getActivityFacilities().getFacilities());
			assigner.run();
			
		}

}
