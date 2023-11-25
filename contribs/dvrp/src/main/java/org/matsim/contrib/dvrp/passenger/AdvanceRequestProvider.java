package org.matsim.contrib.dvrp.passenger;

import javax.annotation.Nullable;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.mobsim.framework.MobsimAgent;

public interface AdvanceRequestProvider {
  public static AdvanceRequestProvider NONE = (MobsimAgent agent, Leg leg) -> null;

  @Nullable
  PassengerRequest retrieveRequest(MobsimAgent agent, Leg leg);
}
