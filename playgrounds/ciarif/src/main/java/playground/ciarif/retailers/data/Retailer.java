package playground.ciarif.retailers.data;

import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import playground.ciarif.retailers.stategies.CatchmentAreaRetailerStrategy;
import playground.ciarif.retailers.stategies.CustomersFeedbackStrategy;
import playground.ciarif.retailers.stategies.GravityModelRetailerStrategy;
import playground.ciarif.retailers.stategies.LogitMaxLinkRetailerStrategy;
import playground.ciarif.retailers.stategies.MaxActivitiesRetailerStrategy;
import playground.ciarif.retailers.stategies.MaxLinkRetailerStrategy;
import playground.ciarif.retailers.stategies.MinTravelingCostsRetailerStrategy;
import playground.ciarif.retailers.stategies.RandomRetailerStrategy;
import playground.ciarif.retailers.stategies.RetailerStrategy;

public class Retailer
{
  private final Id id;
  private final Map<Id, ActivityFacilityImpl> facilities = new TreeMap<Id, ActivityFacilityImpl>();
  private static final Logger log = Logger.getLogger(Retailer.class);
  private RetailerStrategy strategy;
  private Map<Id, ActivityFacilityImpl> movedFacilities = new TreeMap<Id, ActivityFacilityImpl>();

  public Retailer(Id id, RetailerStrategy rs) {
    this.id = id;
    this.strategy = rs;
  }

  public final Id getId() {
    return this.id;
  }

  public final boolean addFacility(ActivityFacilityImpl f) {
    if (f == null) return false;
    if (this.facilities.containsKey(f.getId())) return false;
    this.facilities.put(f.getId(), f);

    return true;
  }

  public final boolean addStrategy(Controler controler, String strategyName)
  {
    if (strategyName.contains("randomRetailerStrategy")) {
      this.strategy = new RandomRetailerStrategy();
      //log.info("The retailer " + this.id + " is using a random Stategy");
      return true;
    }
    if (strategyName.contains("maxLinkRetailerStrategy")) {
      this.strategy = new MaxLinkRetailerStrategy(controler);
      return true;
    }
    if (strategyName.contains("logitMaxLinkRetailerStrategy")) {
      this.strategy = new LogitMaxLinkRetailerStrategy(controler);
      return true;
    }
    if (strategyName.contains("catchmentAreaRetailerStrategy")) {
      this.strategy = new CatchmentAreaRetailerStrategy(controler);
      return true;
    }
    if (strategyName.contains("customersFeedbackStrategy")) {
      this.strategy = new CustomersFeedbackStrategy(controler);
      return true;
    }
    if (strategyName.contains("gravityModelRetailerStrategy")) {
      //log.info("Controler =" + controler);
      this.strategy = new GravityModelRetailerStrategy(controler);
      return true;
    }
    if (strategyName.contains("maxActivitiesRetailerStrategy")) {
      this.strategy = new MaxActivitiesRetailerStrategy(controler);
      return true;
    }
    if (strategyName.contains("minTravelingCostsRetailerStrategy")) {
      this.strategy = new MinTravelingCostsRetailerStrategy(controler);
      return true;
    }
    throw new RuntimeException("The strategy has been not added!");
  }

  public final ActivityFacilityImpl getFacility(Id facId) {
    return ((ActivityFacilityImpl)this.facilities.get(facId));
  }

  public final Map<Id, ActivityFacilityImpl> getFacilities() {
    return this.facilities;
  }

  public final void runStrategy(TreeMap<Id, LinkRetailersImpl> links) {
	log.info("available Links are= " + links);
	log.info("A " + this.strategy + " will be used");
    this.movedFacilities = this.strategy.moveFacilities(this.facilities, links);
  }

  public Map<Id, ActivityFacilityImpl> getMovedFacilities() {
    return this.movedFacilities;
  }
}
