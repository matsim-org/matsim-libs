/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import playground.ciarif.flexibletransports.config.FtConfigGroup;

public class FtTravelTimeDistanceCostCalculator implements TravelDisutility {
  protected final TravelTime timeCalculator;
  private final double travelCostFactor;
  private final double marginalUtlOfDistance;

  public FtTravelTimeDistanceCostCalculator(TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup, FtConfigGroup ftConfigGroup)
  {
    this.timeCalculator = timeCalculator;
    this.travelCostFactor = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0D + cnScoringGroup.getPerforming_utils_hr() / 3600.0D);

//    this.marginalUtlOfDistance = (ftConfigGroup.getDistanceCostCar() / 1000.0D * cnScoringGroup.getMarginalUtlOfDistanceCar());
    this.marginalUtlOfDistance = (ftConfigGroup.getDistanceCostCar() / 1000.0D * cnScoringGroup.getMonetaryDistanceCostRateCar() 
    		* cnScoringGroup.getMarginalUtilityOfMoney() );
   // throw new RuntimeException("this is the exact translation but I am not sure what this means maybe check.  kai, dec'10") ;
    
  }

  @Override
	public double getLinkMinimumTravelDisutility(Link link) {
    return 
      (link.getLength() / link.getFreespeed() * this.travelCostFactor - (
      this.marginalUtlOfDistance * link.getLength()));
  }

  @Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
    double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
    return (travelTime * this.travelCostFactor - (this.marginalUtlOfDistance * link.getLength()));
  }

  protected double getTravelCostFactor() {
    return this.travelCostFactor;
  }

  protected double getMarginalUtlOfDistance() {
    return this.marginalUtlOfDistance;
  }

}
