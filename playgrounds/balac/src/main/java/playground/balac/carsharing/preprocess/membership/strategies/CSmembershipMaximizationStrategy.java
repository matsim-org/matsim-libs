package playground.balac.carsharing.preprocess.membership.strategies;

import java.io.IOException;
import java.util.ArrayList;

import org.jfree.util.Log;
import org.matsim.core.scenario.MutableScenario;

import playground.balac.carsharing.preprocess.membership.FacilitiesPortfolio;
import playground.balac.carsharing.preprocess.membership.MembershipAssigner;
import playground.balac.carsharing.preprocess.membership.SupplySideModel;
import playground.balac.carsharing.preprocess.membership.algos.RunMembershipGA;


public class CSmembershipMaximizationStrategy extends CSstationsLocationStrategy
{
  private MembershipAssigner membershipAssigner;
  private MutableScenario scenario;

  public CSmembershipMaximizationStrategy(MutableScenario scenario)
  {
    this.scenario = scenario;
  }

  public ArrayList<Integer> findOptimalLocations(FacilitiesPortfolio facilitiesPortfolio, SupplySideModel model) throws IOException
  {
    Log.info("stations = " + facilitiesPortfolio);

    this.membershipAssigner = new MembershipAssigner(this.scenario, facilitiesPortfolio, model);
    Integer populationSize = Integer.valueOf(80);
    Integer numberGenerations = Integer.valueOf(500);
    RunMembershipGA rmGA = new RunMembershipGA(populationSize, numberGenerations);
    ArrayList solution = rmGA.runGA(this.membershipAssigner);
    return solution;
  }
}