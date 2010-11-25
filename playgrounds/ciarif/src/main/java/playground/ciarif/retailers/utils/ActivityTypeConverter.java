package playground.ciarif.retailers.utils;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonImpl;
import org.matsim.population.Desires;
import playground.ciarif.retailers.data.PersonRetailersImpl;
import playground.ciarif.retailers.data.Retailer;
import playground.ciarif.retailers.data.Retailers;

public class ActivityTypeConverter
{
  private static final Logger log = Logger.getLogger(ActivityTypeConverter.class);
  private Controler controler;
  private Map<Id, ActivityFacility> retailersShops;
  private double groceryCapacity;

  public ActivityTypeConverter(Controler controler)
  {
    this.controler = controler;
  }

  public void run(Retailers retailers) {
    findRetailersShops(retailers);

    reduceFacilityCapacity();
  }

  private void reduceFacilityCapacity()
  {
    for (ActivityFacility af : this.controler.getFacilities().getFacilities().values()) {
      ActivityFacilityImpl afi = (ActivityFacilityImpl)af;
      if ((!(af.getActivityOptions().containsKey("shop"))) || 
        (this.retailersShops.containsKey(af.getId())))
        continue;
      Double rd = Double.valueOf(MatsimRandom.getRandom().nextDouble());
      if (rd.doubleValue() <= 1.0D) {
        ActivityOptionImpl ao = (ActivityOptionImpl)afi.getActivityOptions().get("shop");
        ao.setCapacity(1.0);
      }
    }
  }

  private void findRetailersShops(Retailers retailers)
  {
    Map retailersShops = new TreeMap();
    for (Retailer r : retailers.getRetailers().values()) {
      retailersShops.putAll(r.getFacilities());
    }
    this.retailersShops = retailersShops;
  }

  private void convertPlansActivities() {
    int count = 0;
    while (count <= this.groceryCapacity * 0.6D)
      for (Person p : this.controler.getPopulation().getPersons().values()) {
        PersonImpl pi = (PersonImpl)p;
        Double rd = Double.valueOf(MatsimRandom.getRandom().nextDouble());

        if (rd.doubleValue() < 0.5D) {
          log.info("modifying the plan of person " + pi.getId());
          ++count;

          PersonRetailersImpl pri = new PersonRetailersImpl(pi);

          pri.modifyDesires("shop_grocery", Double.valueOf(pri.getDesires().getActivityDuration("shop")));
          pri.getDesires().removeActivityDuration("shop");
          pri.getDesires().getActivityDuration("shop_grocery");
          for (PlanElement pe : pri.getSelectedPlan().getPlanElements())
          {
            if (pe instanceof Activity) {
              Activity act = (Activity)pe;
              if (act.getType().equals("shop")) {
                act.setType("shop_grocery");
                ActivityFacility af = (ActivityFacility)this.controler.getFacilities().getFacilities().get(act.getFacilityId());
                if (!(af.getActivityOptions().containsKey("shop_grocery"))) {
                  ActivityFacilityImpl afi = (ActivityFacilityImpl)af;
                  ActivityOptionImpl ao = (ActivityOptionImpl)afi.getActivityOptions().get("shop");
                  afi.createActivityOption("shop_grocery");
                  ((ActivityOptionImpl)afi.getActivityOptions().get("shop_grocery")).setCapacity(ao.getCapacity());
                  ((ActivityOptionImpl)afi.getActivityOptions().get("shop_grocery")).setOpeningTimes(ao.getOpeningTimes());
                  this.groceryCapacity += ao.getCapacity().doubleValue();
                }
              }
            }
          }
        }
      }
  }

  private void convertFacilitiesActivityOptions()
  {
    double groceryCapacity = 0.0D;
    for (ActivityFacility af : this.controler.getFacilities().getFacilities().values()) {
      ActivityFacilityImpl afi = (ActivityFacilityImpl)af;
      if ((!(af.getActivityOptions().containsKey("shop"))) || 
        (!(this.retailersShops.containsKey(af.getId())))) continue;
      ActivityOptionImpl ao = (ActivityOptionImpl)afi.getActivityOptions().get("shop");
      afi.createActivityOption("shop_grocery");
      ((ActivityOptionImpl)afi.getActivityOptions().get("shop_grocery")).setCapacity(ao.getCapacity());
      ((ActivityOptionImpl)afi.getActivityOptions().get("shop_grocery")).setOpeningTimes(ao.getOpeningTimes());
      groceryCapacity += ao.getCapacity().doubleValue();
    }

    this.groceryCapacity = groceryCapacity;
    log.info("groceryCapacity = " + this.groceryCapacity);
  }
}
