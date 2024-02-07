package org.matsim.application.prepare.population;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacility;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class FixSubtourModesTest {

	private FixSubtourModes algo = (FixSubtourModes) new FixSubtourModes().withArgs("--input=tmp", "--output=tmp");
	private PopulationFactory fact = PopulationUtils.getFactory();

	@Test
	void unclosed() {

		Person person = fact.createPerson(Id.create("1000", Person.class));
		final Plan plan = fact.createPlan();

		plan.addActivity(fact.createActivityFromActivityFacilityId("home", Id.create(0, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("car"));

		plan.addActivity(fact.createActivityFromActivityFacilityId("work", Id.create(1, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("pt"));

		plan.addActivity(fact.createActivityFromActivityFacilityId("leisure", Id.create(2, ActivityFacility.class)));


		Collection<TripStructureUtils.Subtour> tours = TripStructureUtils.getSubtours(plan);

		assertThat(tours.stream().anyMatch(st -> algo.fixSubtour(person, st)))
				.isTrue();

	}

	@Test
	void complex() {

		Person person = fact.createPerson(Id.create("1000", Person.class));
		final Plan plan = fact.createPlan();

		plan.addActivity(fact.createActivityFromActivityFacilityId("home", Id.create(0, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("car"));
		plan.addActivity(fact.createActivityFromActivityFacilityId("work", Id.create(1, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("car"));
		plan.addActivity(fact.createActivityFromActivityFacilityId("home", Id.create(0, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("pt"));
		plan.addActivity(fact.createActivityFromActivityFacilityId("leisure", Id.create(2, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("car"));
		plan.addActivity(fact.createActivityFromActivityFacilityId("shop", Id.create(3, ActivityFacility.class)));


		Collection<TripStructureUtils.Subtour> tours = TripStructureUtils.getSubtours(plan);

		assertThat(tours.stream().anyMatch(st -> algo.fixSubtour(person, st)))
				.isTrue();

	}


	@Test
	void correct() {

		Person person = fact.createPerson(Id.create("1000", Person.class));
		final Plan plan = fact.createPlan();

		plan.addActivity(fact.createActivityFromActivityFacilityId("home", Id.create(0, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("car"));
		plan.addActivity(fact.createActivityFromActivityFacilityId("work", Id.create(1, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("car"));
		plan.addActivity(fact.createActivityFromActivityFacilityId("home", Id.create(0, ActivityFacility.class)));

		Collection<TripStructureUtils.Subtour> tours = TripStructureUtils.getSubtours(plan);

		assertThat(tours.stream().noneMatch(st -> algo.fixSubtour(person, st)))
				.isTrue();

		plan.addLeg(fact.createLeg("pt"));
		plan.addActivity(fact.createActivityFromActivityFacilityId("shop", Id.create(2, ActivityFacility.class)));

		plan.addLeg(fact.createLeg("ride"));
		plan.addActivity(fact.createActivityFromActivityFacilityId("home", Id.create(3, ActivityFacility.class)));

		tours = TripStructureUtils.getSubtours(plan);

		assertThat(tours.stream().noneMatch(st -> algo.fixSubtour(person, st)))
				.isTrue();

	}

}