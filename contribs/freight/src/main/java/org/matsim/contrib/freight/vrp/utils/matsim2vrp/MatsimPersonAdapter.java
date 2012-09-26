package org.matsim.contrib.freight.vrp.utils.matsim2vrp;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.vrp.basics.Driver;

public class MatsimPersonAdapter implements Person {

	private Driver driver;

	public MatsimPersonAdapter(Driver driver) {
		super();
		this.driver = driver;
	}

	@Override
	public Id getId() {
		return null;
	}

	public Driver getDriver() {
		return driver;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<? extends Plan> getPlans() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setId(Id id) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean addPlan(Plan p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Plan getSelectedPlan() {
		// TODO Auto-generated method stub
		return null;
	}

}
