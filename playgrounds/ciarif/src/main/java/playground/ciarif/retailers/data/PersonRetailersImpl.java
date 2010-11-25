package playground.ciarif.retailers.data;

import java.util.List;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.Desires;
import org.matsim.utils.customize.Customizable;
import playground.ciarif.retailers.RetailersLocationListener;

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

    if (p.getDesires().getActivityDuration("shop") > 0.0D) {
      this.desires.putActivityDuration("shop", Double.valueOf(p.getDesires().getActivityDuration("shop")).toString());
    }
    if (p.getDesires().getActivityDuration("work") > 0.0D) {
      this.desires.putActivityDuration("work", Double.valueOf(p.getDesires().getActivityDuration("work")).toString());
    }
    if (p.getDesires().getActivityDuration("home") > 0.0D) {
      this.desires.putActivityDuration("home", Double.valueOf(p.getDesires().getActivityDuration("home")).toString());
    }

    if (p.getDesires().getActivityDuration("education") > 0.0D) {
      this.desires.putActivityDuration("education", Double.valueOf(p.getDesires().getActivityDuration("education")).toString());
    }
    if (p.getDesires().getActivityDuration("leisure") > 0.0D)
      this.desires.putActivityDuration("leisure", Double.valueOf(p.getDesires().getActivityDuration("leisure")).toString());
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
