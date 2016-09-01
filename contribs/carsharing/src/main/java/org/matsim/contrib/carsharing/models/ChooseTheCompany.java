package org.matsim.contrib.carsharing.models;

import java.util.Set;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;

public interface ChooseTheCompany {
	
	public String pickACompany(Plan plan, Leg leg);
	//public void setCompanies(Set<String> companyNames);

}
