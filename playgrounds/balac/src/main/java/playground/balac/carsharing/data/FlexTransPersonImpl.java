package playground.balac.carsharing.data;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03toWGS84;

public class FlexTransPersonImpl implements Person
{
	private Person delegate ;
	
  private static final Logger log = Logger.getLogger(FlexTransPersonImpl.class);
  private double accessHome;
  private double accessWork;
  private double densityHome;
  private Coord homeCoord;
  private CH1903LV03toWGS84 coordTranformer = new CH1903LV03toWGS84();
  private Coord homeSwissCoord;

  public FlexTransPersonImpl(Person p)
  {
    delegate = PopulationUtils.createPerson(p.getId());
    PersonUtils.setAge(this, PersonUtils.getAge(p));
    PersonUtils.setCarAvail(this, PersonUtils.getCarAvail(p));
    PersonUtils.setLicence(this, PersonUtils.getLicense(p));
    PersonUtils.setSex(this, PersonUtils.getSex(p));
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
          Coord coord = new Coord(0.0D, 0.0D);
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