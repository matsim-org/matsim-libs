package playground.ciarif.retailers.data;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.Desires;
import org.matsim.utils.customize.Customizable;

import playground.ciarif.retailers.RetailersLocationListener;

public class PersonRetailersImpl extends PersonImpl {
	
	private final static Logger log = Logger.getLogger(RetailersLocationListener.class);
	
	private static final long serialVersionUID = 1L;
	private Customizable customizableDelegate;
	private Id workRetailZone;
	private Id homeRetailZone;
	private Id educationRetailZone;
	private double globalShopsUtility = 0;
	private List<? extends Plan> plans = null;
	//private Desires desires = new Desires("");
	
	public PersonRetailersImpl(PersonImpl p) {
		super(p.getId());
		this.plans=p.getPlans();
		this.addPlan(p.getSelectedPlan());
		this.setSelectedPlan(p.getSelectedPlan());
		this.createDesires(p.getDesires().toString());
		//TODO fc: there must be a better solution than hard coding all the possible activities...
		//TODO check this >0 it might be that is not completely appropriate
		if ((Double)p.getDesires().getActivityDuration("shop")> 0){
			this.desires.putActivityDuration("shop", ((Double)p.getDesires().getActivityDuration("shop")).toString());
		}
		if((Double)p.getDesires().getActivityDuration("work")>0) {
			this.desires.putActivityDuration("work", ((Double)p.getDesires().getActivityDuration("work")).toString());
		}
		if ((Double)p.getDesires().getActivityDuration("home")> 0) {
			this.desires.putActivityDuration("home", ((Double)p.getDesires().getActivityDuration("home")).toString());
		}
		
		if ((Double)p.getDesires().getActivityDuration("education")> 0) {
			this.desires.putActivityDuration("education", ((Double)p.getDesires().getActivityDuration("education")).toString());
		}
		if ((Double)p.getDesires().getActivityDuration("leisure")> 0) {
			this.desires.putActivityDuration("leisure", ((Double)p.getDesires().getActivityDuration("leisure")).toString());		
		}
	}

	public void setGlobalShopsUtility(double average) {
		this.globalShopsUtility = average;
	}
	
	 public double getGlobalShopsUtility () {
		 return this.globalShopsUtility;
	 }
}
