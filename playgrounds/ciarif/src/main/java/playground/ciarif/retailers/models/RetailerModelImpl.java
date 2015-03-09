package playground.ciarif.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.Controler;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import playground.ciarif.retailers.data.PersonRetailersImpl;

public class RetailerModelImpl
  implements RetailerModel
{
  protected static final Logger log = Logger.getLogger(RetailerModelImpl.class);
  protected final Map<Id<Person>, Person> persons = new TreeMap<>();
  protected final Map<Id, PersonRetailersImpl> retailersPersons = new TreeMap<Id, PersonRetailersImpl>();
  protected Controler controler;
  protected Map<Id<ActivityFacility>, ActivityFacility> retailerFacilities;
  protected ActivityFacilities controlerFacilities;
  protected Map<Id<ActivityFacility>, ActivityFacility> shops;
  protected TreeMap<Integer, String> first;
  protected ArrayList<Integer> initialSolution = new ArrayList<Integer>();

  @Override
	public double computePotential(ArrayList<Integer> solution)
  {
    return 0.0D;
  }

  protected Map<Id<ActivityFacility>, ActivityFacility> findScenarioShops(Collection<? extends ActivityFacility> controlerFacilities)
  {
    Map<Id<ActivityFacility>, ActivityFacility> shops = new TreeMap<>();
    for (ActivityFacility f : controlerFacilities) {
      if (f.getActivityOptions().entrySet().toString().contains("shopgrocery")) {
        shops.put(f.getId(), f);
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

  @Override
	public ArrayList<Integer> getInitialSolution()
  {
    return this.initialSolution;
  }
}
