package playground.balac.carsharing.preprocess.membership;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.MatsimRandom;

import org.matsim.core.population.PersonUtils;
import playground.balac.carsharing.data.FlexTransPersonImpl;


public class MembershipModel
  implements SupplySideModel
{
  static final double B_Yes_Access_Home = 0.163D;
  static final double B_Yes_Access_Work = 0.0563D;
  static final double B_Yes_Density = 0.0D;
  static final double B_Yes_31_45 = 0.436D;
  static final double B_yes_CAR_NEV = 1.14D;
  static final double B_yes_CAR_SOM = 2.56D;
  static final double B_yes_MALE = 0.197D;
  static final double KONST_NO = 5.23D;
  static final double B_No_18_30 = 0.791D;
  static final double B_No_60 = 0.43D;
  private static final Logger log = Logger.getLogger(MembershipModel.class);
  private ArrayList<Integer> initialSolution;

  public final int calcMembership(FlexTransPersonImpl pi)
  {
    double[] utils = new double[2];
    utils[0] = calcYesUtil(pi);
    utils[1] = calcNoUtil(pi);
    double[] probs = calcLogitProbability(utils);
    double r = MatsimRandom.getRandom().nextDouble();

    double prob_sum = 0.0D;
    for (int i = 0; i < probs.length; i++) {
      prob_sum += probs[i];
      if (r < prob_sum) return i;
    }
    return -1;
  }

  private final double[] calcLogitProbability(double[] utils)
  {
    double exp_sum = 0.0D;
    for (int i = 0; i < utils.length; i++) exp_sum += Math.exp(utils[i]);
    double[] probs = new double[utils.length];
    for (int i = 0; i < utils.length; i++) probs[i] = (Math.exp(utils[i]) / exp_sum);
    return probs;
  }
  protected final double calcNoUtil(FlexTransPersonImpl pi) {
    double util = 0.0D;
    util += 5.23D * 0.979;
    if (((PersonUtils.getAge(pi) <= 30 ? 1 : 0) & (PersonUtils.getAge(pi) >= 18 ? 1 : 0)) != 0) util += 0.791D;
    if (PersonUtils.getAge(pi) >= 60) util += 0.43D;

    return util;
  }

  protected final double calcYesUtil(FlexTransPersonImpl pi)
  {
    double util = 0.0D;
    util += 0.163D * pi.getAccessHome();
    util += 0.0563D * pi.getAccessWork();
    util += 0.0D * pi.getDensityHome();

    if (((PersonUtils.getAge(pi) <= 45 ? 1 : 0) & (PersonUtils.getAge(pi) >= 31 ? 1 : 0)) != 0) util += 0.436D;
    if (PersonUtils.getCarAvail(pi).equals( "never")) util += 1.14D;
    if (PersonUtils.getCarAvail(pi).equals( "sometimes")) util += 2.56D;
    if (PersonUtils.getSex(pi).equals( "m")) util += 0.197D;

    return util;
  }

  public double computePotential(ArrayList<Integer> paramArrayList)
  {
    return 0.0D;
  }
}