package playground.ciarif.flexibletransports.data;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonUtils;

public class FlexTransPersonImpl extends PersonImpl {
	
	private double accessHome;
	private double accessWork;
	private double densityHome;
	
	public FlexTransPersonImpl (Person p) {
		super(p.getId());
		PersonUtils.setAge(this, PersonUtils.getAge(p));
		PersonUtils.setCarAvail(this, PersonUtils.getCarAvail(p));
		PersonUtils.setLicence(this, PersonUtils.getLicense(p));
		PersonUtils.setSex(this, PersonUtils.getSex(p));
		this.plans = p.getPlans();
	    addPlan(p.getSelectedPlan());
	    setSelectedPlan(p.getSelectedPlan());		
	}
	
	public double getAccessHome() {
		return accessHome;
	}

	public void setAccessHome(double accessHome) {
		this.accessHome = accessHome;
	}

	public double getAccessWork() {
		return accessWork;
	}

	public void setAccessWork(double accessWork) {
		this.accessWork = accessWork;
	}

	public double getDensityHome() {
		return densityHome;
	}

	public void setDensityHome(double densityHome) {
		this.densityHome = densityHome;
	}

	

}
