package org.matsim.contrib.carsharing.models;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;

public class ChooseTheCompanyExample implements ChooseTheCompany {
	@Inject private MembershipContainer memberships;

	@Override
	public String pickACompany(Plan plan, Leg leg, double now, String type) {

		Person person = plan.getPerson();
		Id<Person>  personId = person.getId();
		String mode = leg.getMode();
		Set<String> availableCompanies = this.memberships.getPerPersonMemberships().get(personId).getMembershipsPerCSType().get(mode);
		
		int index = MatsimRandom.getRandom().nextInt(availableCompanies.size());
		
		return (String) availableCompanies.toArray()[index];
	}
}
