package playground.balac.carsharing.preprocess.membership.strategies;

import java.io.IOException;
import java.util.ArrayList;

import playground.balac.carsharing.preprocess.membership.FacilitiesPortfolio;
import playground.balac.carsharing.preprocess.membership.SupplySideModel;

public abstract interface LocationStrategy
{
  public abstract ArrayList<Integer> findOptimalLocations(FacilitiesPortfolio paramFacilitiesPortfolio, SupplySideModel paramSupplySideModel) throws IOException;
}