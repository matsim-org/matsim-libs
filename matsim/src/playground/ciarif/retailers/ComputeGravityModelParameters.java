package playground.ciarif.retailers;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import java.util.ArrayList;
//import org.apache.commons.math.stat.regression.OLSMultipleLinearRegression;
import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

public class ComputeGravityModelParameters
{
  private static final Logger log = Logger.getLogger(ComputeGravityModelParameters.class);

  public double[] computeParameters(Controler controler, DenseDoubleMatrix2D prob_zone_shop, ArrayList<Consumer> consumers, ArrayList<ActivityFacility> shops)
  {
    DenseDoubleMatrix1D prob_pers_shop = new DenseDoubleMatrix1D(consumers.size() * prob_zone_shop.columns());
    DenseDoubleMatrix1D regressand_matrix = new DenseDoubleMatrix1D(consumers.size() * prob_zone_shop.columns());
    DenseDoubleMatrix2D variables_matrix = new DenseDoubleMatrix2D(consumers.size() * prob_zone_shop.columns(), 2);
    DenseDoubleMatrix1D dist_pers_shop = new DenseDoubleMatrix1D(consumers.size() * prob_zone_shop.columns());
    DenseDoubleMatrix1D dim_shop = new DenseDoubleMatrix1D(consumers.size() * prob_zone_shop.columns());
    log.info(" The matrix prob_zone_shop has dimensions = " + prob_zone_shop.rows() + "," + prob_zone_shop.columns());
    for (Consumer c : consumers) {
      double prob_sum = 0.0D;
      double dist_sum = 0.0D;
      double dim_sum = 0.0D;
      for (int i = 0; i <= prob_zone_shop.columns() - 1; ++i)
      {
        double prob = prob_zone_shop.get(Integer.parseInt(c.getRzId().toString()), i);
        double dist = ((ActivityFacility)shops.get(i)).getActivityOption("shop").getFacility().calcDistance(c.getPerson().getSelectedPlan().getFirstActivity().getCoord());

        if (dist == 0.0D) {
          dist = 10.0D;
          log.info("A 'zero distance' has been detected and modified, case number " + (Integer.parseInt(c.getId().toString()) * prob_zone_shop.columns()) + i);
        }
        double dimension = ((ActivityFacility)shops.get(i)).getActivityOption("shop").getCapacity().doubleValue();
        prob_pers_shop.set(Integer.parseInt(c.getId().toString()) * prob_zone_shop.columns() + i, prob);
        prob_sum += prob;
        dist_pers_shop.set(Integer.parseInt(c.getId().toString()) * prob_zone_shop.columns() + i, dist);
        dist_sum += dist;
        dim_shop.set(Integer.parseInt(c.getId().toString()) * prob_zone_shop.columns() + i, dimension);
        dim_sum += dimension;
      }
      double avg_prob_pers_shop = prob_sum / prob_zone_shop.columns();
      double avg_dist_pers_shop = dist_sum / prob_zone_shop.columns();
      double avg_dim_shop = dim_sum / prob_zone_shop.columns();

      for (int j = 0; j <= prob_zone_shop.columns() - 1; ++j) {
        int k = Integer.parseInt(c.getId().toString()) * prob_zone_shop.columns();
        regressand_matrix.set(k + j, prob_pers_shop.get(k + j) / avg_prob_pers_shop);

        variables_matrix.set(k + j, 0, Math.log(dist_pers_shop.get(k + j) / avg_dist_pers_shop));
        variables_matrix.set(k + j, 1, Math.log(dim_shop.get(k + j) / avg_dim_shop));
      }
    }
//    OLSMultipleLinearRegression olsmr = new OLSMultipleLinearRegression();
//    olsmr.newSampleData(regressand_matrix.toArray(), variables_matrix.toArray());
    double[] b = {-0.6, -0.4};
//    double[] b = olsmr.estimateRegressionParameters();
    log.info("Betas = " + b[0] + " " + b[1]);

    return b;
  }
}
