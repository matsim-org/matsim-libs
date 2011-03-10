package playground.ciarif.retailers.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ciarif.retailers.IO.FileRetailerReader;
import playground.ciarif.retailers.preprocess.AssignFacility;
import playground.ciarif.retailers.preprocess.ModifyDesires;

public class ActivityDifferentiator {

		private final static Logger log = Logger.getLogger(ActivityDifferentiator.class);

		// from Microcensus for all modes
		private final double groceryShare = 0.30;
		private final int groceryShopsNumber = 100;
		private int numberOfShopActs;
		private final ScenarioImpl scenario;
		private ArrayList<Id> groceryFacilities = new ArrayList<Id>();
		private TreeMap<Id,ActivityFacility> shopFacilities = new TreeMap<Id,ActivityFacility>();
		//private final String retailersFacilitiesPath = "../../matsim/input/triangle/Retailers.txt";
		private final String retailersFacilitiesPath = "/data/matsim/ciarif/input/zurich_10pc/retailersWithoutRepeatedLinks_MaxAct.txt";

		
				
		public ActivityDifferentiator(ScenarioImpl scenario) {
			this.scenario = scenario;
			this.shopFacilities= this.scenario.getActivityFacilities().getFacilitiesForActivityType("shop");
		}

		public void run() {
			this.init();
			this.modifyFacilities();
			this.assignGroceryAndNonGrocery();
			
		}

		

		private void init() {
			this.numberOfShopActs = this.getNumberOfShopActs();
			final FileRetailerReader retailersReader = new FileRetailerReader(this.retailersFacilitiesPath) ;
			//ArrayList<Id> retailersFacilities = new ArrayList<Id>();
			this.groceryFacilities.addAll(retailersReader.readRetailersFacilities());
			log.info("Grocery shops are= " + groceryShopsNumber);
			while (this.groceryShopsNumber>this.groceryFacilities.size()) {
				int rd = MatsimRandom.getRandom().nextInt(this.shopFacilities.size());				
				ActivityFacility af = (ActivityFacility) this.shopFacilities.values().toArray()[rd];
				if (!(this.groceryFacilities.contains(af.getId()))) {
					groceryFacilities.add(af.getId());
					//log.info("groceryfacilities = " + this.groceryFacilities);
				}
			}	
			log.info("the number of grocery shops is " + this.groceryFacilities.size());
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
						
			for (ActivityFacility  af:this.shopFacilities.values()) {
				ActivityFacilityImpl afi = (ActivityFacilityImpl)af; 
				Double capacity = afi.getActivityOptions().get("shop").getCapacity();
				SortedSet<OpeningTime> openingTimesWkday = afi.getActivityOptions().get("shop").getOpeningTimes(DayType.wkday);
				SortedSet<OpeningTime> openingTimesSat = afi.getActivityOptions().get("shop").getOpeningTimes(DayType.sat);
				SortedSet<OpeningTime> openingTimesMon = afi.getActivityOptions().get("shop").getOpeningTimes(DayType.mon);
				SortedSet<OpeningTime> openingTimesTue = afi.getActivityOptions().get("shop").getOpeningTimes(DayType.tue);
				SortedSet<OpeningTime> openingTimesWed = afi.getActivityOptions().get("shop").getOpeningTimes(DayType.wed);
				SortedSet<OpeningTime> openingTimesThu = afi.getActivityOptions().get("shop").getOpeningTimes(DayType.thu);
				SortedSet<OpeningTime> openingTimesFri = afi.getActivityOptions().get("shop").getOpeningTimes(DayType.fri);
				if (this.groceryFacilities.contains(af.getId())) {
					
					afi.createActivityOption("shopgrocery");
					afi.getActivityOptions().get("shopgrocery").setCapacity(capacity);
					
					if  (!(openingTimesWkday==null)) {
						for (OpeningTime openingTime: openingTimesWkday){
							afi.getActivityOptions().get("shopgrocery").addOpeningTime(openingTime);
						}
					}
					
					if  (!(openingTimesWkday==null)) {
						for (OpeningTime openingTime: openingTimesSat){
							afi.getActivityOptions().get("shopgrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesMon==null)) {
						for (OpeningTime openingTime: openingTimesMon){
							afi.getActivityOptions().get("shopgrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesTue==null)) {
						for (OpeningTime openingTime: openingTimesTue){
							afi.getActivityOptions().get("shopgrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesWed==null)) {
						for (OpeningTime openingTime: openingTimesWed){
							afi.getActivityOptions().get("shopgrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesThu==null)) {
						for (OpeningTime openingTime: openingTimesThu){
							afi.getActivityOptions().get("shopgrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesFri==null)) {
						for (OpeningTime openingTime: openingTimesFri){
							afi.getActivityOptions().get("shopgrocery").addOpeningTime(openingTime);
						}
					}
					
				}
				else {
					
					afi.createActivityOption("shopnongrocery");
					afi.getActivityOptions().get("shopnongrocery").setCapacity(capacity);
					if  (!(openingTimesWkday==null)) {
						for (OpeningTime openingTime: openingTimesWkday){
							afi.getActivityOptions().get("shopnongrocery").addOpeningTime(openingTime);
						}
					}
					
					if  (!(openingTimesSat==null)) {
						for (OpeningTime openingTime: openingTimesSat){
							afi.getActivityOptions().get("shopnongrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesMon==null)) {
						for (OpeningTime openingTime: openingTimesMon){
							afi.getActivityOptions().get("shopnongrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesTue==null)) {
						for (OpeningTime openingTime: openingTimesTue){
							afi.getActivityOptions().get("shopnongrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesWed==null)) {
						for (OpeningTime openingTime: openingTimesWed){
							afi.getActivityOptions().get("shopnongrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesThu==null)) {
						for (OpeningTime openingTime: openingTimesThu){
							afi.getActivityOptions().get("shopnongrocery").addOpeningTime(openingTime);
						}
					}
					if  (!(openingTimesFri==null)) {
						for (OpeningTime openingTime: openingTimesFri){
							afi.getActivityOptions().get("shopnongrocery").addOpeningTime(openingTime);
						}
					}
				}
				afi.getActivityOptions().remove("shop");
			}
		
		}

		private void assignGroceryAndNonGrocery() {

			int assignedNumberOf_GroceryActs = 0;
			int assignedNumberOf_NonGroceryActs = 0;
			int count = 0;
			int divisor = 1;
			for (Person person : this.scenario.getPopulation().getPersons().values()) {
				PersonImpl pi = (PersonImpl)person;
				// intitially only one plan is available
				if (pi.getPlans().size() > 1) {
					log.error("More than one plan for person: " + pi.getId());
				}
				PlanImpl selectedPlan = (PlanImpl)pi.getSelectedPlan();
				//Double duration = pi.getDesires().getActivityDuration("shop");
				final List<? extends PlanElement> actslegs = selectedPlan.getPlanElements();
				for (int j = 0; j < actslegs.size(); j=j+2) {
					final ActivityImpl act = (ActivityImpl)actslegs.get(j);
					if (act.getType().startsWith("shop")) {
						
						double random = MatsimRandom.getRandom().nextDouble();
						if (random <= 0.66) {
							if (assignedNumberOf_GroceryActs < groceryShare * numberOfShopActs) {
								act.setType("shopgrocery");
								assignFacility(act);
								modifyDesires (act, pi);
								act.setLinkId(null);
								assignedNumberOf_GroceryActs++;
								}
							else {
								act.setType("shopnongrocery");
								assignFacility(act);
								modifyDesires (act, pi);
								act.setLinkId(null);
								assignedNumberOf_NonGroceryActs++;
							}
						}
						else {
							if (assignedNumberOf_NonGroceryActs < (1.0 - groceryShare) * numberOfShopActs) {
								act.setType("shopnongrocery");
								assignFacility(act);
								modifyDesires (act, pi);
								act.setLinkId(null);
								assignedNumberOf_NonGroceryActs++;
							}
							else {
								act.setType("shopgrocery");
								assignFacility(act);
								modifyDesires (act, pi);
								act.setLinkId(null);
								assignedNumberOf_GroceryActs++;
							}
						}
						count = count + 1;
						if (count % divisor == 0 ){
							divisor = divisor*2;
							log.info("reassigned activities # " + count);
						}
					}
					
				}
				
			}
			log.info("Number of shopping activities:\t" + this.numberOfShopActs);
			log.info("Number of grocery shopping acts:\t" + assignedNumberOf_GroceryActs);
			log.info("Number of non-grocery acts:\t" + assignedNumberOf_NonGroceryActs);
			log.info("Share:\t"+ (100.0* assignedNumberOf_GroceryActs / this.numberOfShopActs));
		}

		private void modifyDesires(ActivityImpl act, PersonImpl pi) {
			ModifyDesires desiresModificator = new ModifyDesires (act, pi);
			desiresModificator.run();
		}

		private void assignFacility(ActivityImpl act) {
			AssignFacility assigner = new AssignFacility(act, scenario.getActivityFacilities().getFacilitiesForActivityType(act.getType()));
			assigner.run();
			
		}

}
