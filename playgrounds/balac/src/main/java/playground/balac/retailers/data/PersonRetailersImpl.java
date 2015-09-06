package playground.balac.retailers.data;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;

import playground.balac.retailers.RetailersLocationListener;

public class PersonRetailersImpl extends PersonImpl
{
  private static final Logger log = Logger.getLogger(RetailersLocationListener.class);
  private Customizable customizableDelegate;
  private Id workRetailZone;
  private Id homeRetailZone;
  private Id educationRetailZone;
  private double globalShopsUtility = 0.0D;
  private List<? extends Plan> plans = null;

  public PersonRetailersImpl(PersonImpl p)
  {
    super(p.getId());
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
}
