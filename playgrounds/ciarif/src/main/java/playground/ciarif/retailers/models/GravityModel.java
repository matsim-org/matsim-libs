package playground.ciarif.retailers.models;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import playground.ciarif.retailers.data.PersonRetailersImpl;
import playground.ciarif.retailers.data.RetailZone;
import playground.ciarif.retailers.data.RetailZones;

public class GravityModel extends RetailerModelImpl
{
  private static final Logger log = Logger.getLogger(GravityModel.class);
  public static final String CONFIG_GROUP = "GravityModel";
  public static final String CONFIG_ZONES = "zones";
  public static final String SIMPLIFY_MODEL = "isSimplifiedGravityModel";
  private boolean isSimplifiedGravityModel = false;
  private double[] betas;
  private final RetailZones retailZones = new RetailZones();
  private final Map<Id, ActivityFacilityImpl> retailerFacilities;
  private int counter = 0;
  private int operationCount = 0;
  private int nextCounterMsg = 1;
  private ArrayList<Integer> incumbentSolution;
  private ArrayList<Double> incumbentFitness = new ArrayList();

  public GravityModel(Controler controler, Map<Id, ActivityFacilityImpl> retailerFacilities)
  {
    this.controler = controler;
    log.info("Controler" + this.controler);
    this.retailerFacilities = retailerFacilities;
    this.controlerFacilities = controler.getFacilities();
    this.shops = findScenarioShops(this.controlerFacilities.getFacilities().values());

    for (Person p : controler.getPopulation().getPersons().values()) {
      PersonImpl pi = (PersonImpl)p;
      this.persons.put(pi.getId(), pi);
    }
  }

  public void init()
  {
    int number_of_zones = 0;
    int n = (int)Double.parseDouble(this.controler.getConfig().findParam("GravityModel", "zones"));
    number_of_zones = (int)Math.pow(n, 2.0D);
    if (number_of_zones == 0) throw new RuntimeException("In config file, param = zones in module = GravityModel not defined!");
    createZonesFromPersonsShops(n);

    Boolean isSimplifiedGravityModel = Boolean.valueOf(Boolean.parseBoolean(this.controler.getConfig().findParam("GravityModel", "isSimplifiedGravityModel")));
    if (isSimplifiedGravityModel == null) log.warn("In config file, param = isSimplifiedGravityModel in module = GravityModel not defined, the value 'true' will be used as default for this parameter");

    Gbl.printMemoryUsage();

    for (PersonImpl pi : this.persons.values()) {
      PersonRetailersImpl pr = new PersonRetailersImpl(pi);
      this.retailersPersons.put(pr.getId(), pr);
    }
  }

  private ArrayList<Double> computeNormalPotential(ArrayList<Integer> solution)
  {
    ArrayList newFitness = new ArrayList();
    double global_likelihood = 0.0D;
    int a = 0;

    for (ActivityFacility c : this.retailerFacilities.values())
    {
      String linkId = (String)this.first.get(solution.get(a));
      Coord coord = ((Link)this.controler.getNetwork().getLinks().get(new IdImpl(linkId))).getCoord();

      double loc_likelihood = 0.0D;

      for (PersonRetailersImpl pr : this.retailersPersons.values())
      {
        double pers_sum_potential = 0.0D;
        double pers_potential = 0.0D;
        double pers_likelihood = 0.0D;

        ActivityFacility firstFacility = (ActivityFacility)this.controlerFacilities.getFacilities().get(((PlanImpl)pr.getSelectedPlan()).getFirstActivity().getFacilityId());
        double dist1 = ((ActivityFacilityImpl)firstFacility).calcDistance(coord);
        if (dist1 == 0.0D) {
          dist1 = 10.0D;
        }
        pers_potential = Math.pow(dist1, this.betas[0]) + Math.pow(((ActivityOption)c.getActivityOptions().get("shop")).getCapacity().doubleValue(), this.betas[1]);

        if (pr.getGlobalShopsUtility() == 0.0D) {
          processPerson();

          for (ActivityFacility s : this.shops.values()) {
            double dist = 0.0D;
            int count = 0;

            for (ActivityFacility af : this.retailerFacilities.values()) {
              if (af.equals(s)) {
                int index = count;
                Coord coord1 = ((Link)this.controler.getNetwork().getLinks().get(new IdImpl((String)this.first.get(solution.get(index))))).getCoord();

                dist = ((ActivityFacilityImpl)firstFacility).calcDistance(coord1);
                if (dist == 0.0D) {
                  dist = 10.0D;
                }
              }
              else if (CoordUtils.calcDistance(s.getCoord(), ((PlanImpl)pr.getSelectedPlan()).getFirstActivity().getCoord()) == 0.0D) {
                dist = 10.0D;
              }
              else
              {
                dist = CoordUtils.calcDistance(s.getCoord(), ((PlanImpl)pr.getSelectedPlan()).getFirstActivity().getCoord());
              }
              ++count;
            }

            double potential = Math.pow(dist, this.betas[0]) + Math.pow(((ActivityOption)s.getActivityOptions().get("shop")).getCapacity().doubleValue(), this.betas[1]);

            pers_sum_potential += potential;
          }
          pr.setGlobalShopsUtility(pers_sum_potential);
        }
        pers_likelihood = pers_potential / pr.getGlobalShopsUtility();
        loc_likelihood += pers_likelihood;
      }

      global_likelihood += loc_likelihood;
      newFitness.add(a, Double.valueOf(loc_likelihood));

      ++a;
    }
    return newFitness;
  }

  public double computePotential(ArrayList<Integer> solution)
  {
    double fitness = 0.0D;
    if (this.isSimplifiedGravityModel)
    {
      if (this.initialSolution.equals(solution)) {
        this.incumbentFitness = computeNormalPotential(solution);
      }
      else {
        this.incumbentFitness = computeIncrementalPotential(solution, this.incumbentSolution, this.incumbentFitness);
      }
      this.incumbentSolution = solution;
    }
    else
    {
      this.incumbentFitness = computeNormalPotential(solution);
    }

    for (Object fit : this.incumbentFitness.toArray()) {
      fitness += ((Double)fit).doubleValue();
    }

    return fitness;
  }

  private ArrayList<Double> computeIncrementalPotential(ArrayList<Integer> solution, ArrayList<Integer> incumbentSolution, ArrayList<Double> incumbentFitness)
  {
    int a = 0;
    ArrayList newFitness = new ArrayList();

    for (ActivityFacility c : this.retailerFacilities.values())
    {
      String linkId = (String)this.first.get(solution.get(a));
      Coord coord = ((Link)this.controler.getNetwork().getLinks().get(new IdImpl(linkId))).getCoord();

      double loc_likelihood = 0.0D;
      if (!(((Integer)solution.get(a)).equals(incumbentSolution.get(a))))
      {
        for (PersonRetailersImpl pr : this.retailersPersons.values())
        {
          double pers_sum_potential = 0.0D;
          double pers_potential = 0.0D;
          double pers_likelihood = 0.0D;

          ActivityFacility firstFacility = (ActivityFacility)this.controlerFacilities.getFacilities().get(((PlanImpl)pr.getSelectedPlan()).getFirstActivity().getFacilityId());
          double dist1 = ((ActivityFacilityImpl)firstFacility).calcDistance(coord);
          if (dist1 == 0.0D) {
            dist1 = 10.0D;
          }
          pers_potential = Math.pow(dist1, this.betas[0]) + Math.pow(((ActivityOption)c.getActivityOptions().get("shop")).getCapacity().doubleValue(), this.betas[1]);

          if (pr.getGlobalShopsUtility() == 0.0D) {
            processPerson();

            for (ActivityFacility s : this.shops.values()) {
              double dist = 0.0D;
              int count = 0;

              for (ActivityFacility af : this.retailerFacilities.values())
              {
                this.operationCount += 1;
                if (af.equals(s)) {
                  int index = count;
                  Coord coord1 = ((Link)this.controler.getNetwork().getLinks().get(new IdImpl((String)this.first.get(solution.get(index))))).getCoord();
                  dist = ((ActivityFacilityImpl)firstFacility).calcDistance(coord1);
                  if (dist == 0.0D) {
                    dist = 10.0D;
                  }
                }
                else if (CoordUtils.calcDistance(s.getCoord(), ((PlanImpl)pr.getSelectedPlan()).getFirstActivity().getCoord()) == 0.0D) {
                  dist = 10.0D;
                }
                else
                {
                  dist = CoordUtils.calcDistance(s.getCoord(), ((PlanImpl)pr.getSelectedPlan()).getFirstActivity().getCoord());
                }
                ++count;
              }

              double potential = Math.pow(dist, this.betas[0]) + Math.pow(((ActivityOption)s.getActivityOptions().get("shop")).getCapacity().doubleValue(), this.betas[1]);

              pers_sum_potential += potential;
            }
            pr.setGlobalShopsUtility(pers_sum_potential);
          }
          pers_likelihood = pers_potential / pr.getGlobalShopsUtility();
          loc_likelihood += pers_likelihood;
        }
        newFitness.add(a, Double.valueOf(loc_likelihood));
      }
      else {
        newFitness.add(a, (Double)incumbentFitness.get(a));
      }
      ++a;
    }

    return newFitness;
  }

  private void createZonesFromPersonsShops(int n) {
    log.info("Zones are created");
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);
    for (PersonImpl p : this.persons.values()) {
      if (((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getX() < minx) minx = ((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getX();
      if (((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getY() < miny) miny = ((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getY();
      if (((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getX() > maxx) maxx = ((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getX();
      if (((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getY() <= maxy) continue; maxy = ((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getY();
    }
    for (ActivityFacility shop : this.shops.values()) {
      if (shop.getCoord().getX() < minx) minx = shop.getCoord().getX();
      if (shop.getCoord().getY() < miny) miny = shop.getCoord().getY();
      if (shop.getCoord().getX() > maxx) maxx = shop.getCoord().getX();
      if (shop.getCoord().getY() <= maxy) continue; maxy = shop.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    log.info("Min x = " + minx);
    log.info("Min y = " + miny);
    log.info("Max x = " + maxx);
    log.info("Max y = " + maxy);

    double x_width = (maxx - minx) / n;
    double y_width = (maxy - miny) / n;
    int a = 0;
    int i = 0;

    while (i < n) {
      int j = 0;
      while (j < n) {
        Coord c;
        Id id = new IdImpl(a);
        double x1 = minx + i * x_width;
        double x2 = x1 + x_width;
        double y1 = miny + j * y_width;
        double y2 = y1 + y_width;
        RetailZone rz = new RetailZone(id, Double.valueOf(x1), Double.valueOf(y1), Double.valueOf(x2), Double.valueOf(y2));
        for (PersonImpl p : this.persons.values()) {
          c = ((ActivityFacility)this.controlerFacilities.getFacilities().get(((PlanImpl)p.getSelectedPlan()).getFirstActivity().getFacilityId())).getCoord();
          if ((c.getX() < x2) && (c.getX() >= x1) && (c.getY() < y2) && (c.getY() >= y1)) {
            rz.addPersonToQuadTree(c, p);
          }
        }
        for (ActivityFacility af : this.shops.values()) {
          c = af.getCoord();
          if ((((c.getX() < x2) ? 1 : 0) & ((c.getX() >= x1) ? 1 : 0) & ((c.getY() < y2) ? 1 : 0) & ((c.getY() >= y1) ? 1 : 0)) != 0) {
            rz.addShopToQuadTree(c, af);
          }
        }
        this.retailZones.addRetailZone(rz);
        ++a;
        ++j;
      }
      ++i;
    }
  }

  private void createZonesFromFacilities(int n)
  {
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);

    for (ActivityFacility af : this.controlerFacilities.getFacilities().values()) {
      if (af.getCoord().getX() < minx) minx = af.getCoord().getX();
      if (af.getCoord().getY() < miny) miny = af.getCoord().getY();
      if (af.getCoord().getX() > maxx) maxx = af.getCoord().getX();
      if (af.getCoord().getY() <= maxy) continue; maxy = af.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    log.info("Min x = " + minx);
    log.info("Min y = " + miny);
    log.info("Max x = " + maxx);
    log.info("Max y = " + maxy);

    double x_width = (maxx - minx) / n;
    double y_width = (maxy - miny) / n;
    int a = 0;
    int i = 0;

    while (i < n) {
      int j = 0;
      while (j < n) {
        Coord c;
        Id id = new IdImpl(a);
        double x1 = minx + i * x_width;
        double x2 = x1 + x_width;
        double y1 = miny + j * y_width;
        double y2 = y1 + y_width;
        RetailZone rz = new RetailZone(id, Double.valueOf(x1), Double.valueOf(y1), Double.valueOf(x2), Double.valueOf(y2));
        for (Person p : this.persons.values())
        {
          c = ((ActivityFacility)this.controlerFacilities.getFacilities().get(((PlanImpl)p.getSelectedPlan()).getFirstActivity().getFacilityId())).getCoord();
          if ((c.getX() < x2) && (c.getX() >= x1) && (c.getY() < y2) && (c.getY() >= y1)) {
            rz.addPersonToQuadTree(c, p);
          }
        }
        for (ActivityFacility af : this.shops.values()) {
          c = af.getCoord();
          if ((((c.getX() < x2) ? 1 : 0) & ((c.getX() >= x1) ? 1 : 0) & ((c.getY() < y2) ? 1 : 0) & ((c.getY() >= y1) ? 1 : 0)) != 0) {
            rz.addShopToQuadTree(c, af);
          }
        }
        this.retailZones.addRetailZone(rz);
        ++a;
        ++j;
      }
      ++i;
    }
  }

  private void createZonesFromPersonsActivitiesShops(int n) {
    log.info("Zones are created");
    double minx = (1.0D / 0.0D);
    double miny = (1.0D / 0.0D);
    double maxx = (-1.0D / 0.0D);
    double maxy = (-1.0D / 0.0D);
    for (Person p : this.persons.values()) {
      if (((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getX() < minx) minx = ((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getX();
      if (((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getY() < miny) miny = ((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getY();
      if (((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getX() > maxx) maxx = ((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getX();
      if (((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getY() <= maxy) continue; maxy = ((PlanImpl)p.getSelectedPlan()).getFirstActivity().getCoord().getY();
    }
    for (ActivityFacility shop : this.shops.values()) {
      if (shop.getCoord().getX() < minx) minx = shop.getCoord().getX();
      if (shop.getCoord().getY() < miny) miny = shop.getCoord().getY();
      if (shop.getCoord().getX() > maxx) maxx = shop.getCoord().getX();
      if (shop.getCoord().getY() <= maxy) continue; maxy = shop.getCoord().getY();
    }
    minx -= 1.0D; miny -= 1.0D; maxx += 1.0D; maxy += 1.0D;
    log.info("Min x = " + minx);
    log.info("Min y = " + miny);
    log.info("Max x = " + maxx);
    log.info("Max y = " + maxy);

    double x_width = (maxx - minx) / n;
    double y_width = (maxy - miny) / n;
    int a = 0;
    int i = 0;

    while (i < n) {
      int j = 0;
      while (j < n) {
        Coord c;
        Id id = new IdImpl(a);
        double x1 = minx + i * x_width;
        double x2 = x1 + x_width;
        double y1 = miny + j * y_width;
        double y2 = y1 + y_width;
        RetailZone rz = new RetailZone(id, Double.valueOf(x1), Double.valueOf(y1), Double.valueOf(x2), Double.valueOf(y2));
        for (Person p : this.persons.values()) {
          for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
            if (pe instanceof Activity) {
              Activity act = (Activity)pe;
              if ((act.getType().equals("work")) || (act.getType().equals("home"))) continue; act.getType().equals("education");
            }

          }

          c = ((ActivityFacility)this.controlerFacilities.getFacilities().get(((PlanImpl)p.getSelectedPlan()).getFirstActivity().getFacilityId())).getCoord();
          if ((c.getX() < x2) && (c.getX() >= x1) && (c.getY() < y2) && (c.getY() >= y1)) {
            rz.addPersonToQuadTree(c, p);
          }
        }
        for (ActivityFacility af : this.shops.values()) {
          c = af.getCoord();
          if ((((c.getX() < x2) ? 1 : 0) & ((c.getX() >= x1) ? 1 : 0) & ((c.getY() < y2) ? 1 : 0) & ((c.getY() >= y1) ? 1 : 0)) != 0) {
            rz.addShopToQuadTree(c, af);
          }
        }
        this.retailZones.addRetailZone(rz);
        ++a;
        ++j;
      }
      ++i;
    }
  }

  public void processPerson()
  {
    this.counter += 1;
    if (this.counter == this.nextCounterMsg) {
      this.nextCounterMsg *= 2;
      printEventsCount();
    }
  }

  private void printEventsCount() {
    log.info(" Person # " + this.counter + " have been processed");
    Gbl.printMemoryUsage();
  }

  public Map<Id, ActivityFacilityImpl> getScenarioShops() {
    return this.shops; }

  public RetailZones getRetailZones() {
    return this.retailZones; }

  public boolean setBetas(double[] betas) {
    this.betas = betas;
    log.info("Betas = " + betas[0] + " " + betas[1]);
    return true;
  }
}