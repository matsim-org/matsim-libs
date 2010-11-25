package playground.ciarif.retailers.stategies;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PlanImpl;
import playground.ciarif.retailers.RetailerGA.RunRetailerGA;
import playground.ciarif.retailers.data.Consumer;
import playground.ciarif.retailers.data.LinkRetailersImpl;
import playground.ciarif.retailers.data.RetailZone;
import playground.ciarif.retailers.data.RetailZones;
import playground.ciarif.retailers.models.GravityModel;
import playground.ciarif.retailers.utils.Utils;

public class GravityModelRetailerStrategy extends RetailerStrategyImpl
{
  public static final String CONFIG_GROUP = "Retailers";
  public static final String COMPUTE_BETAS = "computeParameters";
  public static final String GENERATIONS = "numberOfGenerations";
  public static final String SIMPLIFY_MODEL = "isSimplifiedGravityModel";
  public static final String POPULATION = "PopulationSize";
  public static final String NAME = "gravityModelRetailerStrategy";
  private Map<Id, ActivityFacilityImpl> shops;
  private RetailZones retailZones;
  private ArrayList<Consumer> consumers;
  private Map<Id, Integer> shops_keys;
  private Map<Id, ActivityFacilityImpl> movedFacilities = new TreeMap();

  public GravityModelRetailerStrategy(Controler controler) {
    super(controler);
    log.info("Controler" + this.controler);
  }

  public Map<Id, ActivityFacilityImpl> moveFacilities(Map<Id, ActivityFacilityImpl> retailerFacilities, TreeMap<Id, LinkRetailersImpl> freeLinks)
  {
    this.retailerFacilities = retailerFacilities;
    log.info("Controler" + this.controler);
    GravityModel gm = new GravityModel(this.controler, retailerFacilities);
    gm.init();
    this.shops = gm.getScenarioShops();
    this.retailZones = gm.getRetailZones();
    DenseDoubleMatrix2D prob_i_j = findProb();
    String computeBetas = this.controler.getConfig().findParam("Retailers", "computeParameters");
    if (computeBetas == null) { log.warn("In config file, param = 'computeParameters' in module = 'Retailers' not defined, this will be interpreted as a 'Yes' by the program");
      computeBetas = "yes";
    }
    double[] b = { 1.0D, -1.0D };
    if (computeBetas.equals("yes")) {
      log.info("Betas of the Gravity Model are explicitly calculated");
      b = computeParameters(prob_i_j);
    }
    double[] b1 = { -b[0], -b[1] };

    gm.setBetas(b1);
    Integer populationSize = Integer.valueOf(Integer.parseInt(this.controler.getConfig().findParam("Retailers", "PopulationSize")));
    if (populationSize == null) log.warn("In config file, param = PopulationSize in module = Retailers not defined, the value '10' will be used as default for this parameter");
    Integer numberGenerations = Integer.valueOf(Integer.parseInt(this.controler.getConfig().findParam("Retailers", "numberOfGenerations")));
    if (numberGenerations == null) log.warn("In config file, param = numberOfGenerations in module = Retailers not defined, the value '100' will be used as default for this parameter");

    RunRetailerGA rrGA = new RunRetailerGA(populationSize, numberGenerations);
    TreeMap first = createInitialLocationsForGA(mergeLinks(freeLinks, retailerFacilities));
    gm.setFirst(first);
    gm.setInitialSolution(first.size());
    ArrayList solution = rrGA.runGA(gm);
    int count = 0;
    for (ActivityFacilityImpl af : this.retailerFacilities.values()) {
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

  private double[] computeParameters(DenseDoubleMatrix2D prob_zone_shop)
  {
    int number_of_consumers = this.consumers.size();
    int number_of_retailer_shops = this.retailerFacilities.size();

    DenseDoubleMatrix1D regressand_matrix = new DenseDoubleMatrix1D(number_of_consumers);
    if (regressand_matrix != null) log.info(" the regressand matrix has been created");
    DenseDoubleMatrix2D variables_matrix = new DenseDoubleMatrix2D(number_of_consumers, 2);
    if (variables_matrix != null) log.info(" the variables matrix has been created");
    log.info("This retailer owns " + number_of_retailer_shops + " shops and " + this.consumers.size() + " consumers ");
    log.info("This scenario has " + this.retailZones.getRetailZones().size() + " zones");
    int cases = 0;
    for (Consumer c : this.consumers) {
      int consumer_index = Integer.parseInt(c.getId().toString());
      int zone_index = Integer.parseInt(c.getRzId().toString());
      ActivityFacilityImpl af = (ActivityFacilityImpl)c.getShoppingFacility();
      double prob = prob_zone_shop.get(zone_index, ((Integer)this.shops_keys.get(af.getId())).intValue());
      regressand_matrix.set(consumer_index, Math.log(prob / prob_zone_shop.viewRow(zone_index).zSum()));
      double dist1 = af.calcDistance(((PlanImpl)c.getPerson().getSelectedPlan()).getFirstActivity().getCoord());
      if (dist1 == 0.0D) {
        dist1 = 10.0D;
        ++cases;
      }
      double sumDist = 0.0D;
      double sumDim = 0.0D;
      double dist2 = 0.0D;
      double dim = 0.0D;
      for (ActivityFacilityImpl aaff : this.shops.values()) {
        dist2 = aaff.calcDistance(((PlanImpl)c.getPerson().getSelectedPlan()).getFirstActivity().getCoord());
        sumDist += dist2;
        dim = ((ActivityOptionImpl)aaff.getActivityOptions().get("shop")).getCapacity().doubleValue();
        sumDim += dim;
      }
      variables_matrix.set(consumer_index, 0, Math.log(dist1 / sumDist / this.shops.size()));
      variables_matrix.set(consumer_index, 1, Math.log(((ActivityOptionImpl)af.getActivityOptions().get("shop")).getCapacity().doubleValue() / sumDim / this.shops.size()));
    }

    log.info("A 'zero distance' has been detected and modified, in " + cases + " cases");

    OLSMultipleLinearRegression olsmr = new OLSMultipleLinearRegression();
    olsmr.newSampleData(regressand_matrix.toArray(), variables_matrix.toArray());

    double[] b = olsmr.estimateRegressionParameters();

    return b;
  }

  private DenseDoubleMatrix2D findProb()
  {
    this.consumers = new ArrayList();
    DenseDoubleMatrix2D prob_i_j = new DenseDoubleMatrix2D(this.retailZones.getRetailZones().values().size(), this.shops.size());

    this.shops_keys = new TreeMap();
    int consumer_count = 0;
    int j = 0;
    log.info("This scenario has " + this.shops.size() + " shops");
    for (ActivityFacilityImpl f : this.shops.values()) {
      this.shops_keys.put(f.getId(), Integer.valueOf(j));
      for (RetailZone rz : this.retailZones.getRetailZones().values())
      {
        double counter = 0.0D;
        double prob = 0.0D;
        ArrayList<Person> persons = rz.getPersons();

        for (Person p : persons) {
          boolean first_shop = true;

          for (PlanElement pe2 : p.getSelectedPlan().getPlanElements())
          {
            if (pe2 instanceof Activity) {
              Activity act = (Activity)pe2;

              if ((act.getType().equals("shop")) && (act.getFacilityId().equals(f.getId()))) {
                if ((first_shop) && (this.retailerFacilities.containsKey(f.getId()))) {
                  Consumer consumer = new Consumer(consumer_count, p, rz.getId());
                  consumer.setShoppingFacility(f);
                  this.consumers.add(consumer);
                  ++consumer_count;
                }
                counter += 1.0D;
                int i = Integer.parseInt(rz.getId().toString());
                prob = counter / persons.size();
                prob_i_j.set(i, j, prob);
                first_shop = false;
              }
            }
          }
        }
      }
      ++j;
    }

    return prob_i_j;
  }

  public Map<Id, ActivityFacilityImpl> getMovedFacilities() {
    log.info("moved Facilities are: " + this.movedFacilities);
    return this.movedFacilities;
  }
}
