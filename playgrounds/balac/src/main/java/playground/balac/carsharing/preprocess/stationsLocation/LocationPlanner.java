package playground.balac.carsharing.preprocess.stationsLocation;

import java.io.IOException;

public abstract interface LocationPlanner
{
  public abstract void runStrategy() throws IOException;
}