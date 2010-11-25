package playground.ciarif.retailers.stategies;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.network.NetworkImpl;
import playground.ciarif.retailers.RetailerGA.RunRetailerGA;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.models.MaxActivityModel;
import playground.ciarif.retailers.utils.Utils;

public class MaxActivitiesRetailerStrategy extends RetailerStrategyImpl
{
  public static final String CONFIG_GROUP = "Retailers";
  public static final String NAME = "maxActivitiesRetailerStrategy";
  public static final String GENERATIONS = "numberOfGenerations";
  public static final String POPULATION = "PopulationSize";
  private Map<Id, ActivityFacilityImpl> shops;
  private Map<Id, ActivityFacilityImpl> retailerFacilities;
  private Map<Id, ActivityFacilityImpl> movedFacilities = new TreeMap();

  public MaxActivitiesRetailerStrategy(Controler controler)
  {
    super(controler);
  }

  public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> retailerFacilities, TreeMap<Id, LinkRetailersImpl> freeLinks)
  {
    this.retailerFacilities = retailerFacilities;
    MaxActivityModel mam = new MaxActivityModel(this.controler, retailerFacilities);
    TreeMap first = createInitialLocationsForGA(mergeLinks(freeLinks, retailerFacilities));
    Log.info("first = " + first);
    mam.init(first);
    this.shops = mam.getScenarioShops();
    Integer populationSize = Integer.valueOf(Integer.parseInt(this.controler.getConfig().findParam("Retailers", "PopulationSize")));
    if (populationSize == null) log.warn("In config file, param = PopulationSize in module = Retailers not defined, the value '10' will be used as default for this parameter");
    Integer numberGenerations = Integer.valueOf(Integer.parseInt(this.controler.getConfig().findParam("Retailers", "numberOfGenerations")));
    if (numberGenerations == null) log.warn("In config file, param = numberOfGenerations in module = Retailers not defined, the value '100' will be used as default for this parameter");
    RunRetailerGA rrGA = new RunRetailerGA(populationSize, numberGenerations);
    ArrayList solution = rrGA.runGA(mam);
    int count = 0;
    for (ActivityFacilityImpl af : this.retailerFacilities.values())
    {
      if (first.get(solution.get(count)) != af.getLinkId().toString()) {
        Utils.moveFacility(af, (Link)this.controler.getNetwork().getLinks().get(new IdImpl((String)first.get(solution.get(count)))));
        log.info("The facility " + af.getId() + " has been moved");
        this.movedFacilities.put(af.getId(), af);
        log.info("Link Id after = " + af.getLinkId());

        log.info("Count= " + count);
      }
      ++count;
    }
    return this.movedFacilities;
  }
}
