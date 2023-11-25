package org.matsim.contrib.accessibility;

import org.matsim.facilities.ActivityFacility;

public interface FacilityDataExchangeInterface {

  void setFacilityAccessibilities(
      ActivityFacility measurePoint, Double timeOfDay, String mode, double accessibility);

  void finish();
}
