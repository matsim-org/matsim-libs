package org.matsim.contrib.discrete_mode_choice.test_utils;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacility;

public class PlanBuilder {
	private final PopulationFactory factory;
	private final Person person;
	private final Plan plan;

	public PlanBuilder() {
		Config config = ConfigUtils.createConfig();

		Population population = PopulationUtils.createPopulation(config);
		factory = population.getFactory();

		person = factory.createPerson(Id.createPersonId("person"));

		plan = factory.createPlan();
		person.addPlan(plan);
	}

	public PlanBuilder setLicense(String value) {
		PersonUtils.setLicence(person, value);
		return this;
	}

	public PlanBuilder setCarAvailability(String value) {
		PersonUtils.setCarAvail(person, value);
		return this;
	}

	public PlanBuilder addActivity(String type, OptionalTime endTime, OptionalTime duration, Id<Link> linkId,
			Id<ActivityFacility> facilityId) {
		Activity activity = factory.createActivityFromLinkId(type, linkId);
		activity.setFacilityId(facilityId);

		if (endTime.isDefined()) {
			activity.setEndTime(endTime.seconds());
		}

		if (duration.isDefined()) {
			activity.setMaximumDuration(duration.seconds());
		}

		plan.addActivity(activity);
		return this;
	}

	public PlanBuilder addActivityWithLinkId(String type, String linkId) {
		return addActivity(type, OptionalTime.undefined(), OptionalTime.defined(3600.0), Id.createLinkId(linkId), null);
	}

	public PlanBuilder addActivityWithFacilityId(String type, String facilityId) {
		return addActivity(type, OptionalTime.undefined(), OptionalTime.defined(3600.0), null,
				Id.create(facilityId, ActivityFacility.class));
	}

	public PlanBuilder addActivityWithLinkId(String type, double endTime, String linkId) {
		return addActivity(type, OptionalTime.defined(endTime), OptionalTime.undefined(), Id.createLinkId(linkId),
				null);
	}

	public PlanBuilder addActivityWithFacilityId(String type, double endTime, String facilityId) {
		return addActivity(type, OptionalTime.defined(endTime), OptionalTime.undefined(), null,
				Id.create(facilityId, ActivityFacility.class));
	}

	public PlanBuilder addActivityWithEndTime(String type, double endTime) {
		return addActivity(type, OptionalTime.defined(endTime), OptionalTime.undefined(), null, null);
	}

	public PlanBuilder addActivityWithDuration(String type, double duration) {
		return addActivity(type, OptionalTime.undefined(), OptionalTime.defined(duration), null, null);
	}

	public PlanBuilder addLeg(String mode, double traveTime) {
		Leg leg = factory.createLeg(mode);
		leg.setTravelTime(traveTime);
		plan.addLeg(leg);
		return this;
	}

	public PlanBuilder addLeg(String mode) {
		return addLeg(mode, 3600.0);
	}

	public PlanBuilder addLeg() {
		return addLeg(TransportMode.walk);
	}

	public Plan buildPlan() {
		Plan copy = factory.createPlan();
		copy.setPerson(person);

		PopulationUtils.copyFromTo(this.plan, copy);

		return copy;
	}

	public List<DiscreteModeChoiceTrip> buildDiscreteModeChoiceTrips() {
		return new TripListConverter().convert(plan);
	}
}
