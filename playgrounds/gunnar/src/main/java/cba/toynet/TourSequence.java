package cba.toynet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TourSequence {

	// -------------------- CONSTANTS --------------------

	static enum Type {

		work_car, work_car_other1_car, work_car_other1_pt, work_car_other2_car,

		work_pt, work_pt_other1_car, work_pt_other1_pt, work_pt_other2_car

		// two new alternatives (the policy measure option)
		// work_car_other2_pt, work_pt_other2_pt
	}

	final Type type;

	// -------------------- CONSTRUCTION --------------------

	TourSequence(final Type type) {
		this.type = type;
	}

	// -------------------- IMPLEMENTATION --------------------

	static final String homeLoc = "home"; // "1_2";
	static final String dest1 = "dest1"; // "3_2";
	static final String dest2 = "dest2"; // "5_4";

	private void addHomeAct(final Plan plan, final String homeActType, final Double endTime_s,
			final Scenario scenario) {
		final Activity home = scenario.getPopulation().getFactory().createActivityFromLinkId(homeActType,
				Id.createLinkId(homeLoc));
		if (endTime_s != null) {
			home.setEndTime(endTime_s);
		}
		plan.addActivity(home);
	}

	private void addTour(final Plan plan, final String actType, final String mode, final String dest,
			final double actEndTime_s, final Scenario scenario) {
		plan.addLeg(scenario.getPopulation().getFactory().createLeg(mode));

		final Activity act = scenario.getPopulation().getFactory().createActivityFromLinkId(actType,
				Id.createLinkId(dest));
		act.setEndTime(actEndTime_s);
		plan.addActivity(act);

		plan.addLeg(scenario.getPopulation().getFactory().createLeg(mode));
	}

	Plan asPlan(final Scenario scenario, final Person person) {

		final Plan result = scenario.getPopulation().getFactory().createPlan();
		result.setPerson(person);

		this.addHomeAct(result, "home", 6.0 * 3600, scenario);

		if (Type.work_car.equals(this.type)) {
			this.addTour(result, "work", "car", dest1, 16 * 3600, scenario);
		} else if (Type.work_car_other1_car.equals(this.type)) {
			this.addTour(result, "work", "car", dest1, 16 * 3600, scenario);
			this.addHomeAct(result, "home2", 17.0 * 3600, scenario);
			this.addTour(result, "other", "car", dest1, 18 * 3600, scenario);
		} else if (Type.work_car_other1_pt.equals(this.type)) {
			this.addTour(result, "work", "car", dest1, 16 * 3600, scenario);
			this.addHomeAct(result, "home2", 17.0 * 3600, scenario);
			this.addTour(result, "other", "pt", dest1, 18 * 3600, scenario);
		} else if (Type.work_car_other2_car.equals(this.type)) {
			this.addTour(result, "work", "car", dest1, 16 * 3600, scenario);
			this.addHomeAct(result, "home2", 17.0 * 3600, scenario);
			this.addTour(result, "other", "car", dest2, 18 * 3600, scenario);
		} else if (Type.work_pt.equals(this.type)) {
			this.addTour(result, "work", "pt", dest1, 16 * 3600, scenario);
		} else if (Type.work_pt_other1_car.equals(this.type)) {
			this.addTour(result, "work", "pt", dest1, 16 * 3600, scenario);
			this.addHomeAct(result, "home2", 17.0 * 3600, scenario);
			this.addTour(result, "other", "car", dest1, 18 * 3600, scenario);
		} else if (Type.work_pt_other1_pt.equals(this.type)) {
			this.addTour(result, "work", "pt", dest1, 16 * 3600, scenario);
			this.addHomeAct(result, "home2", 17.0 * 3600, scenario);
			this.addTour(result, "other", "pt", dest1, 18 * 3600, scenario);
		} else if (Type.work_pt_other2_car.equals(this.type)) {
			this.addTour(result, "work", "pt", dest1, 16 * 3600, scenario);
			this.addHomeAct(result, "home2", 17.0 * 3600, scenario);
			this.addTour(result, "other", "car", dest2, 18 * 3600, scenario);
		}
		// else if (Type.work_car_other2_pt.equals(this.type)) {
		// // new, with policy alternative
		// this.addTour(result, "work", "car", dest1, 16 * 3600, scenario);
		// this.addHomeAct(result, "home2", 17.0 * 3600, scenario);
		// this.addTour(result, "other", "pt", dest2, 18 * 3600, scenario);
		// } else if (Type.work_pt_other2_pt.equals(this.type)) {
		// // new, with policy alternative
		// this.addTour(result, "work", "pt", dest1, 16 * 3600, scenario);
		// this.addHomeAct(result, "home2", 17.0 * 3600, scenario);
		// this.addTour(result, "other", "pt", dest2, 18 * 3600, scenario);
		// }
		else {
			throw new RuntimeException("unknown type: " + this.type);
		}

		this.addHomeAct(result, "home", null, scenario);

		return result;
	}

}
