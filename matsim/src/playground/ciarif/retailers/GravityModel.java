package playground.ciarif.retailers;

import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Coord;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.population.PersonImpl;

public class GravityModel
{
  private static final Logger log = Logger.getLogger(GravityModel.class);
  private double[] betas;
  private Controler controler;
  private ArrayList<ActivityFacility> retailers_shops = new ArrayList();
  private ArrayList<ActivityFacility> shops;
  private ArrayList<Consumer> consumers;

  public GravityModel(Controler controler, double[] b, ArrayList<ActivityFacility> retailers_shops, ArrayList<Consumer> consumers, ArrayList<ActivityFacility> shops)
  {
    this.betas = b;
    this.controler = controler;
    this.retailers_shops = retailers_shops;
    this.shops = shops;
    this.consumers = consumers;
  }

  public double computePotential(ArrayList<Integer> solution)
  {
    double global_likelihood = 0;
    int a = 0;
    for (ActivityFacility c : this.retailers_shops) {
      Coord coord = this.controler.getNetwork().getLink(((Integer)solution.get(a)).toString()).getCoord();
      ++a;
      double loc_likelihood = 0.0D;
      for (PersonImpl p : this.controler.getPopulation().getPersons().values()) {
        double pers_sum_potential = 0.0D;
        double pers_potential = 0.0D;
        double pers_likelihood = 0.0D;
        double dist1 = 0.0D;

        if (p.getSelectedPlan().getFirstActivity().getFacility().calcDistance(coord) == 0.0D) dist1 = 10.0D;
        else {
          dist1 = p.getSelectedPlan().getFirstActivity().getFacility().calcDistance(coord);
        }
        pers_potential = Math.pow(dist1, this.betas[0]) + Math.pow(c.getActivityOption("shop").getCapacity().doubleValue(), this.betas[1]);

        for (ActivityFacility s : this.shops) {
          double dist = 0.0D;

          if (this.retailers_shops.contains(s))
          {
            int index = this.retailers_shops.indexOf(s);
            Coord coord1 = this.controler.getNetwork().getLink(((Integer)solution.get(index)).toString()).getCoord();

            if (p.getSelectedPlan().getFirstActivity().getFacility().calcDistance(coord1) == 0.0D) dist = 10.0D;
            else {
              dist = p.getSelectedPlan().getFirstActivity().getFacility().calcDistance(coord1);
            }

          }
          else if (s.calcDistance(p.getSelectedPlan().getFirstActivity().getCoord()) == 0.0D) { dist = 10.0D; } else {
            dist = s.calcDistance(p.getSelectedPlan().getFirstActivity().getCoord());
          }

          double potential = Math.pow(dist, this.betas[0]) + Math.pow(s.getActivityOption("shop").getCapacity().doubleValue(), this.betas[1]);

          pers_sum_potential += potential;
        }

        pers_likelihood = pers_potential / pers_sum_potential;

        loc_likelihood += pers_likelihood;
      }

      global_likelihood += loc_likelihood;
    }

    return global_likelihood;
  }
}
