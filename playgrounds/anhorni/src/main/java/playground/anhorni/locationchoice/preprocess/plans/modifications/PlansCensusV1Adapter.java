package playground.anhorni.locationchoice.preprocess.plans.modifications;

import java.util.List;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.locationchoice.utils.QuadTreeRing;

import playground.anhorni.locationchoice.preprocess.facilities.FacilityQuadTreeBuilder;

public class PlansCensusV1Adapter {

	private QuadTreeRing<ActivityFacilityImpl> shopTree;
	private QuadTreeRing<ActivityFacilityImpl> leisureTree;
	private QuadTreeRing<ActivityFacilityImpl> homeTree;
	private QuadTreeRing<ActivityFacilityImpl> educationTree;
	private QuadTreeRing<ActivityFacilityImpl> workTree;

	private void init(ActivityFacilitiesImpl facilities) {
		FacilityQuadTreeBuilder builder = new FacilityQuadTreeBuilder();
		leisureTree = builder.builFacQuadTree(
				"leisure", facilities.getFacilitiesForActivityType("leisure"));

		shopTree = builder.builFacQuadTree(
				"shop", facilities.getFacilitiesForActivityType("shop"));

		homeTree = builder.builFacQuadTree(
				"home", facilities.getFacilitiesForActivityType("home"));


		TreeMap<Id, ActivityFacility> educationTreeMap =
			facilities.getFacilitiesForActivityType("education_kindergarten");
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education_primary"));
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education_secondary"));
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education_higher"));
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education_other"));
		educationTreeMap.putAll(facilities.getFacilitiesForActivityType("education"));
		educationTree = builder.builFacQuadTree(
				"education", educationTreeMap);


		TreeMap<Id, ActivityFacility> workTreeMap =
			facilities.getFacilitiesForActivityType("work_sector2");
		workTreeMap.putAll(facilities.getFacilitiesForActivityType("work_sector3"));
		workTreeMap.putAll(facilities.getFacilitiesForActivityType("work"));

		workTree = builder.builFacQuadTree(
				"work", workTreeMap);
	}


	public void adapt(Population plans, ActivityFacilitiesImpl facilities) {

		this.init(facilities);

		for (Person person : plans.getPersons().values()) {
			Plan plan = person.getSelectedPlan();

			((PersonImpl) person).createDesires("adapted desire");

			List<? extends PlanElement> actslegs = plan.getPlanElements();
			for (int j = 0; j < actslegs.size(); j=j+2) {
				final ActivityImpl act = (ActivityImpl)actslegs.get(j);
				this.adaptActivity(act, person);
			}
		}
	}

	private void adaptActivity(ActivityImpl act, Person person) {

		double desiredDuration = 0.0;
		if (act.getType().equals("tta")) {
			desiredDuration = 8.0 * 3600.0;
		}
		else {
			desiredDuration = 3600 * Double.parseDouble(act.getType().substring(1));
		}
		String fullType = "tta";

		if (act.getType().startsWith("h")) {
			fullType = "home";
			act.setFacilityId(this.homeTree.get(act.getCoord().getX(), act.getCoord().getY()).getId());
		}
		else if (act.getType().startsWith("e")){
			fullType = "education";
			act.setFacilityId(this.educationTree.get(act.getCoord().getX(), act.getCoord().getY()).getId());
		}
		else if (act.getType().startsWith("s")){
			fullType = "shop";
			act.setFacilityId(this.shopTree.get(act.getCoord().getX(), act.getCoord().getY()).getId());
		}
		else if (act.getType().startsWith("l")){
			fullType = "leisure";
			act.setFacilityId(this.leisureTree.get(act.getCoord().getX(), act.getCoord().getY()).getId());
		}
		else if (act.getType().startsWith("w")){
			fullType = "work";
			act.setFacilityId(this.workTree.get(act.getCoord().getX(), act.getCoord().getY()).getId());
		}
		if (((PersonImpl) person).getDesires().getActivityDuration(fullType) <= 0.0) {
			((PersonImpl) person).getDesires().putActivityDuration(fullType, desiredDuration);
		}
		act.setType(fullType);
	}
}
