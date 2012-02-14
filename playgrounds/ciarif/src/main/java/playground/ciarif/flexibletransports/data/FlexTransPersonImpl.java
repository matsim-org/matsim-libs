package playground.ciarif.flexibletransports.data;

import org.matsim.core.population.PersonImpl;

public class FlexTransPersonImpl extends PersonImpl {
	
	private double accessHome;
	private double accessWork;
	private double densityHome;
	
	public FlexTransPersonImpl (PersonImpl p) {
		super(p.getId());
		this.setAge(p.getAge());
		this.setCarAvail(p.getCarAvail());
		this.setLicence(p.getLicense());
		this.setSex(p.getSex());
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
