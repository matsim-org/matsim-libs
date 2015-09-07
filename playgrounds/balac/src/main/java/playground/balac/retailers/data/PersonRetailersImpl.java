package playground.balac.retailers.data;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;

import playground.balac.retailers.RetailersLocationListener;

public class PersonRetailersImpl implements Person
{
	private final Person delegate ;
	
  private static final Logger log = Logger.getLogger(RetailersLocationListener.class);
  private Customizable customizableDelegate;
  private Id workRetailZone;
  private Id homeRetailZone;
  private Id educationRetailZone;
  private double globalShopsUtility = 0.0D;
  private List<? extends Plan> plans = null;

  public PersonRetailersImpl(PersonImpl p)
  {
	  delegate = PersonImpl.createPerson(p.getId());
    this.plans = p.getPlans();
    addPlan(p.getSelectedPlan());
    setSelectedPlan(p.getSelectedPlan());
  }
   

  public void setGlobalShopsUtility(double average)
  {
    this.globalShopsUtility = average;
  }

  public double getGlobalShopsUtility() {
    return this.globalShopsUtility;
  }


public List<? extends Plan> getPlans() {
		return delegate.getPlans();
}


public boolean addPlan(Plan p) {
		return delegate.addPlan(p);
}


public boolean removePlan(Plan p) {
		return delegate.removePlan(p);
}


public Plan getSelectedPlan() {
		return delegate.getSelectedPlan();
}


public void setSelectedPlan(Plan selectedPlan) {
		delegate.setSelectedPlan(selectedPlan);
}


public Plan createCopyOfSelectedPlanAndMakeSelected() {
		return delegate.createCopyOfSelectedPlanAndMakeSelected();
}


public Id<Person> getId() {
		return delegate.getId();
}


public Map<String, Object> getCustomAttributes() {
		return delegate.getCustomAttributes();
}
}
