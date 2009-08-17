package playground.ciarif.retailers.data;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.Desires;
import org.matsim.utils.customize.Customizable;

public class PersonRetailersImpl extends PersonImpl {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Customizable customizableDelegate;
	private Id workRetailZone;
	private Id homeRetailZone;
	private Id educationRetailZone;
	private double globalShopsUtility = 0;
	private List<PlanImpl> plans = null;
	
	public PersonRetailersImpl(PersonImpl p) {
		super(p.getId());
		this.plans=p.getPlans();
		this.addPlan(p.getSelectedPlan());
		this.setSelectedPlan(p.getSelectedPlan());
		//this.createDesires(p.getDesires().toString());
		
	}
	public void setGlobalShopsUtility(double average) {
		this.globalShopsUtility = average;
	}
	
	 public double getGlobalShopsUtility () {
		 return this.globalShopsUtility;
	 }
}
