package org.matsim.application.prepare.population;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncodeTypicalDurationsTest {

	private static EncodeTypicalDurations command() {
		EncodeTypicalDurations command = new EncodeTypicalDurations();
		new CommandLine(command).parseArgs("in.xml", "--output", "out.xml", "--simulation-period-in-days", "1.125", "--min-typical-duration", "300");
		return command;
	}

	/**
	 * The first activity runs from midnight to its end, middle ones from their (reconstructed) start to their end, the
	 * last from its start to the end of the simulation period; a too-short middle activity is prolonged to the minimum.
	 */
	@Test
	void encodesTheSelectedPlanOnThePerson() {
		Person person = person(plan());

		command().encodeTypicalDuration(person);

		assertThat(PersonUtils.getTypicalDurations(person)).containsExactly(
			new PersonUtils.TypicalDuration("home_morning", 7 * 3600),
			// starts after the 30 min leg
			new PersonUtils.TypicalDuration("work", 16 * 3600 - (7 * 3600 + 1800)),
			// 2 min, prolonged to the minimum
			new PersonUtils.TypicalDuration("shop_daily", 300),
			// start times are reconstructed before the prolonging, so the evening starts after the shop's original end
			new PersonUtils.TypicalDuration("home_evening", 1.125 * 24 * 3600 - (16 * 3600 + 240 + 600)));

		List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
		Activity shop = activities.get(2);
		assertThat(shop.getEndTime().isUndefined()).isTrue();
		assertThat(shop.getMaximumDuration().seconds()).isEqualTo(300);
		// nothing on the activities any more
		activities.forEach(act -> assertThat(act.getAttributes().getAttribute("typicalDuration")).isNull());
	}

	@Test
	void stayHomeAgentsGetNone() {
		Plan plan = PopulationUtils.createPlan();
		plan.addActivity(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("l")));
		Person person = person(plan);

		command().encodeTypicalDuration(person);

		assertThat(PersonUtils.getTypicalDurations(person)).isNull();
	}

	@Test
	void plansMustShareTheActivitySequence() {
		Person person = person(plan());
		Plan other = PopulationUtils.createPlan();
		other.addActivity(act("home_morning", 7 * 3600));
		other.addLeg(leg());
		other.addActivity(act("home_evening", Double.NaN));
		person.addPlan(other);

		assertThatThrownBy(() -> command().encodeTypicalDuration(person)).hasMessageContaining("different sequences of main activities");
	}

	private static Person person(Plan plan) {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("p"));
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		return person;
	}

	/** home_morning until 7:00, work until 16:00, a 2 min shop, home; 30 min, 2 min and 10 min legs; no start times. */
	private static Plan plan() {
		Plan plan = PopulationUtils.createPlan();
		plan.addActivity(act("home_morning", 7 * 3600));
		plan.addLeg(leg(1800));
		plan.addActivity(act("work", 16 * 3600));
		plan.addLeg(leg(120));
		plan.addActivity(act("shop_daily", 16 * 3600 + 240));
		plan.addLeg(leg(600));
		plan.addActivity(act("home_evening", Double.NaN));
		return plan;
	}

	private static Activity act(String type, double end) {
		Activity act = PopulationUtils.createActivityFromLinkId(type, Id.createLinkId("l"));
		if (!Double.isNaN(end)) {
			act.setEndTime(end);
		}
		return act;
	}

	private static Leg leg(double travelTime) {
		Leg leg = PopulationUtils.createLeg(TransportMode.walk);
		leg.setTravelTime(travelTime);
		return leg;
	}

	private static Leg leg() {
		return leg(0);
	}
}
