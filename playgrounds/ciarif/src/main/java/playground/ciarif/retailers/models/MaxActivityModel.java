package playground.ciarif.retailers.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.data.PersonPrimaryActivity;
import playground.ciarif.retailers.data.PersonRetailersImpl;
import playground.ciarif.retailers.utils.Utils;

public class MaxActivityModel extends RetailerModelImpl
{
  private static final Logger log = Logger.getLogger(MaxActivityModel.class);

  private TreeMap<Id, LinkRetailersImpl> availableLinks = new TreeMap<Id, LinkRetailersImpl>();

  public MaxActivityModel(Controler controler, Map<Id, ActivityFacilityImpl> retailerFacilities)
  {
    this.controler = controler;
    this.retailerFacilities = retailerFacilities;
    this.controlerFacilities = this.controler.getFacilities();
    this.shops = findScenarioShops(this.controlerFacilities.getFacilities().values());

    for (Person p : controler.getPopulation().getPersons().values()) {
      PersonImpl pi = (PersonImpl)p;
      this.persons.put(pi.getId(), pi);
    }
  }

  public void init(TreeMap<Integer, String> first)
  {
    this.first = first;

    setInitialSolution(this.first.size());
    log.info("Initial solution = " + getInitialSolution());
    findScenarioShops(this.controlerFacilities.getFacilities().values());
    Gbl.printMemoryUsage();
    for (PersonImpl pi : this.persons.values()) {
      PersonRetailersImpl pr = new PersonRetailersImpl(pi);
      this.retailersPersons.put(pr.getId(), pr);
    }
    Utils.setPersonPrimaryActivityQuadTree(Utils.createPersonPrimaryActivityQuadTree(this.controler));
    Utils.setShopsQuadTree(Utils.createShopsQuadTree(this.controler));

    for (Integer i = Integer.valueOf(0); i.intValue() < first.size(); i = Integer.valueOf(i.intValue() + 1)) {
      String linkId = (String)this.first.get(i);

      LinkRetailersImpl link = new LinkRetailersImpl((Link)this.controler.getNetwork().getLinks().get(new IdImpl(linkId)), (NetworkImpl)this.controler.getNetwork(), Double.valueOf(0.0D), Double.valueOf(0.0D));
      Collection<PersonPrimaryActivity> primaryActivities = Utils.getPersonPrimaryActivityQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), 1000.0D);
      Collection<ActivityFacility> shops = Utils.getShopsQuadTree().get(link.getCoord().getX(), link.getCoord().getY(), 1000.0D);

      int globalShopsCapacity = 0;
      for (ActivityFacility shop : shops) {
        globalShopsCapacity = (int)(globalShopsCapacity + ((ActivityOption)shop.getActivityOptions().get("shopgrocery")).getCapacity().doubleValue());
      }
      log.info("primaryActivities = " + primaryActivities.size());
      log.info("globalShopsCapacity = " + globalShopsCapacity);
      link.setPotentialCustomers(primaryActivities.size() - globalShopsCapacity);
      this.availableLinks.put(link.getId(), link);
    }
  }

  public double computePotential(ArrayList<Integer> solution)
  {
    Double Fitness = Double.valueOf(0.0D);
    for (int s = 0; s < this.retailerFacilities.size(); ++s) {
      String linkId = (String)this.first.get(solution.get(s));
      Fitness = Double.valueOf(Fitness.doubleValue() + ((LinkRetailersImpl)this.availableLinks.get(new IdImpl(linkId))).getPotentialCustomers());
    }

    return Fitness.doubleValue();
  }

  public Map<Id, ActivityFacilityImpl> getScenarioShops() {
    return this.shops;
  }
}
