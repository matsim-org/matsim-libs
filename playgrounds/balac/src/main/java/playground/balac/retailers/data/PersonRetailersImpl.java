package playground.balac.retailers.data;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.utils.customize.Customizable;

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
    this.desires = new MyDesires(p.getDesires());
    createDesires(p.getDesires().toString());
    
    for (String actType:p.getDesires().getActivityDurations().keySet()) {
    	Double actDur = p.getDesires().getActivityDuration(actType);
    	this.desires.putActivityDuration(actType, actDur);
    }
  }
   

  public void setGlobalShopsUtility(double average)
  {
    this.globalShopsUtility = average;
  }

  public double getGlobalShopsUtility() {
    return this.globalShopsUtility;
  }

  public void modifyDesires(String actType, Double durs) {
    desires.putActivityDuration(actType, durs);
  }
}
