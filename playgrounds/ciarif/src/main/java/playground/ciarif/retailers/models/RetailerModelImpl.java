package playground.ciarif.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.population.PersonImpl;
import playground.ciarif.retailers.data.PersonRetailersImpl;

public class RetailerModelImpl
  implements RetailerModel
{
  protected static final Logger log = Logger.getLogger(RetailerModelImpl.class);
  protected final Map<Id, PersonImpl> persons = new TreeMap<Id, PersonImpl>();
  protected final Map<Id, PersonRetailersImpl> retailersPersons = new TreeMap<Id, PersonRetailersImpl>();
  protected Controler controler;
  protected Map<Id, ActivityFacilityImpl> retailerFacilities;
  protected ActivityFacilities controlerFacilities;
  protected Map<Id, ActivityFacilityImpl> shops;
  protected TreeMap<Integer, String> first;
  protected ArrayList<Integer> initialSolution = new ArrayList<Integer>();

  public double computePotential(ArrayList<Integer> solution)
  {
    return 0.0D;
  }

  protected Map<Id, ActivityFacilityImpl> findScenarioShops(Collection<? extends ActivityFacility> controlerFacilities)
  {
    Map<Id, ActivityFacilityImpl> shops = new TreeMap<Id, ActivityFacilityImpl>();
    for (ActivityFacility f : controlerFacilities) {
      if (f.getActivityOptions().entrySet().toString().contains("shopgrocery")) {
        shops.put(f.getId(), (ActivityFacilityImpl)f);
      }
    }
    return shops;
  }

  public boolean setFirst(TreeMap<Integer, String> first) {
    this.first = first;

    return true;
  }

  public void setInitialSolution(int size) {
    for (int i = 0; i < size; ++i)
      this.initialSolution.add(Integer.valueOf(i));
  }

  public ArrayList<Integer> getInitialSolution()
  {
    return this.initialSolution;
  }
}
