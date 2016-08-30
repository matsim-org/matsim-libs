package org.matsim.contrib.carsharing.models;

import java.util.Set;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;

public class ChooseTheCompanyExample implements ChooseTheCompany {
	
	Set<String> availableCompanies;
	
	@Override
	public String pickACompany(Plan plan, Leg leg) {

		int index = MatsimRandom.getRandom().nextInt(this.availableCompanies.size());
		
		return (String) availableCompanies.toArray()[index];
	}

	@Override
	public void setCompanies(Set<String> companyNames) {

		this.availableCompanies = companyNames;
	}

}
