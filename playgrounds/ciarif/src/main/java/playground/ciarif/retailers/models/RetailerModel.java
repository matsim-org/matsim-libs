package playground.ciarif.retailers.models;

import java.util.ArrayList;

public abstract interface RetailerModel
{
  public abstract double computePotential(ArrayList<Integer> paramArrayList);

  public abstract ArrayList<Integer> getInitialSolution();
}
