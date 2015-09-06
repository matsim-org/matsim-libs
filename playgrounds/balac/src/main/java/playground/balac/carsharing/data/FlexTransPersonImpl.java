package playground.balac.carsharing.data;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

public class FlexTransPersonImpl extends PersonImpl
{
  private static final Logger log = Logger.getLogger(FlexTransPersonImpl.class);
  private double accessHome;
  private double accessWork;
  private double densityHome;
  private Coord homeCoord;
  private CH1903LV03toWGS84 coordTranformer = new CH1903LV03toWGS84();
  private Coord homeSwissCoord;

  public FlexTransPersonImpl(Person p)
  {
    super(p.getId());
    setAge(this, PersonImpl.getAge(p));
    setCarAvail(this, PersonImpl.getCarAvail(p));
    setLicence(this, PersonImpl.getLicense(p));
    setSex(this, PersonImpl.getSex(p));
    for (Plan plan : p.getPlans()) {
        addPlan(plan);
    }

    setSelectedPlan(p.getSelectedPlan());
    setHomeCoord();
    setHomeSwissCoord();
  }

  private void setHomeSwissCoord()
  {
    this.homeSwissCoord = this.coordTranformer.transform(this.homeCoord);
  }

  public double getAccessHome()
  {
    return this.accessHome;
  }

  public void setAccessHome(double accessHome) {
    this.accessHome = accessHome;
  }

  public double getAccessWork() {
    return this.accessWork;
  }

  public void setAccessWork(double accessWork) {
    this.accessWork = accessWork;
  }

  public double getDensityHome() {
    return this.densityHome;
  }

  public void setDensityHome(double densityHome) {
    this.densityHome = densityHome;
  }

  public void setHomeCoord() {
    for (PlanElement pe : getSelectedPlan().getPlanElements())
    {
      if ((pe instanceof Activity))
      {
        Activity act = (Activity)pe;

        if (act.getType().equals("home")) {
          this.homeCoord = act.getCoord();
          return;
        }
        if (act.getType().equals("tta")) {
          CoordImpl coord = new CoordImpl(0.0D, 0.0D);
          this.homeCoord = coord;
        }
      }
    }
  }

  public Coord getHomeCoord()
  {
    return this.homeCoord;
  }

  public Coord getHomeSwissCoord() {
    return this.homeSwissCoord;
  }
}