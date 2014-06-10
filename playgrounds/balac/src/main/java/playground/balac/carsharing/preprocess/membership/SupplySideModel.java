package playground.balac.carsharing.preprocess.membership;

import java.util.ArrayList;

import playground.balac.carsharing.data.FlexTransPersonImpl;

public abstract interface SupplySideModel
{
  public abstract double computePotential(ArrayList<Integer> paramArrayList);

  public abstract int calcMembership(FlexTransPersonImpl paramFlexTransPersonImpl);
}