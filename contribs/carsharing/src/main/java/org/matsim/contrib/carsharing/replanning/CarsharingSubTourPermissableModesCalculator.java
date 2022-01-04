package org.matsim.contrib.carsharing.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;

public class CarsharingSubTourPermissableModesCalculator implements PermissibleModesCalculator {

	private final Scenario scenario;
	private MembershipContainer memberships;

	public CarsharingSubTourPermissableModesCalculator(final Scenario scenario, final String[] availableModes,
			MembershipContainer memberships) {
		this.scenario = scenario;
		this.memberships = memberships;		
	}

	@Override
	public Collection<String> getPermissibleModes(Plan plan) {
		final Person person;
		Id<Person> personId;
		List<String> l = new ArrayList<String>();
		try {
			person = plan.getPerson();
			personId = person.getId();
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("I need a PersonImpl to get car availability");
		}

		if (Boolean.parseBoolean(
				scenario.getConfig().getModules().get("TwoWayCarsharing").getParams().get("useTwoWayCarsharing"))
				&& this.memberships.getPerPersonMemberships().containsKey(personId)) {

			if (this.memberships.getPerPersonMemberships().get(personId).getMembershipsPerCSType()
					.containsKey("twoway")) {
				l.add("twoway");
			}
		}

		return l;
	}

}
