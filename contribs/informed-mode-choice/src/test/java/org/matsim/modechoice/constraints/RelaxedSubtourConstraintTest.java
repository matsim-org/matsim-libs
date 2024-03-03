package org.matsim.modechoice.constraints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.modechoice.EstimatorContext;
import org.matsim.modechoice.PlanModel;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class RelaxedSubtourConstraintTest {

	private final PopulationFactory f = PopulationUtils.getFactory();

	private RelaxedSubtourConstraint constraint;

	@BeforeEach
	public void setUp() throws Exception {

		SubtourModeChoiceConfigGroup config = new SubtourModeChoiceConfigGroup();

		config.setChainBasedModes(new String[]{"bike", "car"});

		constraint = new RelaxedSubtourConstraint(config);
	}

	@Test
	void cycle() {

		Person person = f.createPerson(Id.createPersonId(0));

		Plan plan = f.createPlan();
		plan.setPerson(person);

		// In this plan, a person would need two bikes available at station 1 and 2
		plan.addActivity(f.createActivityFromLinkId("home", Id.createLinkId(0)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("station1", Id.createLinkId(1)));
		plan.addLeg(f.createLeg("pt"));

		plan.addActivity(f.createActivityFromLinkId("station2", Id.createLinkId(2)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("work", Id.createLinkId(3)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("station2", Id.createLinkId(2)));
		plan.addLeg(f.createLeg("pt"));

		plan.addActivity(f.createActivityFromLinkId("station1", Id.createLinkId(1)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("home", Id.createLinkId(0)));

		int[] context = constraint.getContext(new EstimatorContext(person, null), PlanModel.newInstance(plan));

		System.out.println(Arrays.toString(context));

		assert constraint.isValid(context, new String[]{"bike", "pt", "bike", "bike", "pt", "bike"});

		assert constraint.isValid(context, new String[]{"car", "pt", "bike", "bike", "pt", "car"});

		assert !constraint.isValid(context, new String[]{"walk", "pt", "bike", "bike", "pt", "car"});


	}

	@Test
	void open() {

		Person person = f.createPerson(Id.createPersonId(0));

		Plan plan = f.createPlan();
		plan.setPerson(person);

		plan.addActivity(f.createActivityFromLinkId("home", Id.createLinkId(0)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("work", Id.createLinkId(1)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("leisure", Id.createLinkId(2)));

		int[] context = constraint.getContext(new EstimatorContext(person, null), PlanModel.newInstance(plan));

		System.out.println(Arrays.toString(context));

		assert constraint.isValid(context, new String[]{"bike", "bike"});
		assert constraint.isValid(context, new String[]{"walk", "pt"});
		assert !constraint.isValid(context, new String[]{"walk", "car"});

	}

	@Test
	void openEnd() {

		Person person = f.createPerson(Id.createPersonId(0));

		Plan plan = f.createPlan();
		plan.setPerson(person);

		plan.addActivity(f.createActivityFromLinkId("home", Id.createLinkId(0)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("work", Id.createLinkId(1)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("home", Id.createLinkId(0)));
		plan.addLeg(f.createLeg("bike"));

		plan.addActivity(f.createActivityFromLinkId("leisure", Id.createLinkId(2)));

		int[] context = constraint.getContext(new EstimatorContext(person, null), PlanModel.newInstance(plan));

		System.out.println(Arrays.toString(context));

		assert constraint.isValid(context, new String[]{"bike", "bike", "bike"});

		assert constraint.isValid(context, new String[]{"bike", "bike", "car"});

		assert constraint.isValid(context, new String[]{"walk", "pt", "car"});

		assert !constraint.isValid(context, new String[]{"car", "bike", "walk"});

	}

}
