/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.vsp.airPollution.exposure;

import java.util.Map;
import java.util.Set;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

/**
 * benjamin, ihab, amit
 */

public class EmissionResponsibilityTollTimeDistanceTravelDisutility implements TravelDisutility {

    private final TravelDisutility randomizedTimeDistanceTravelDisutility;
    private final TravelTime timeCalculator;
    private final double marginalUtlOfMoney;
    private final EmissionModule emissionModule;
    private final EmissionResponsibilityCostModule emissionResponsibilityCostModule;
    private final Set<Id<Link>> hotspotLinks;
    private final double sigma ;

    private final Vehicles emissionVehicles;

    public EmissionResponsibilityTollTimeDistanceTravelDisutility(TravelDisutility randomizedTimeDistanceTravelDisutility,
                                                                  TravelTime timeCalculator,
                                                                  double marginalUtilOfMoney,
                                                                  EmissionModule emissionModule,
                                                                  EmissionResponsibilityCostModule emissionResponsibilityCostModule,
                                                                  double sigma,
                                                                  Set<Id<Link>> hotspotLinks, Vehicles vehicles) {
        this.randomizedTimeDistanceTravelDisutility = randomizedTimeDistanceTravelDisutility;
        this.timeCalculator = timeCalculator;
        this.marginalUtlOfMoney = marginalUtilOfMoney;
        this.emissionModule = emissionModule;
        this.emissionResponsibilityCostModule = emissionResponsibilityCostModule;
        this.sigma = sigma;
        this.hotspotLinks = hotspotLinks;

        this.emissionVehicles = vehicles;
    }

    @Override
    public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle v) {

        double randomizedTimeDistanceDisutilityForLink = this.randomizedTimeDistanceTravelDisutility.getLinkTravelDisutility(link, time, person, v);

        Vehicle emissionVehicle = v;
        if (emissionVehicle == null){
            // TODO [AA] : This looks ugly, is there any better way to get vehicle here, amit Nov 2016.
            // the link travel disutility is asked without information about the vehicle
            if (person == null){
                // additionally, no person is given -> a default vehicle type is used
                Log.warn("No person and no vehicle is given to calculate the link travel disutility. The default vehicle type is used to estimate emission disutility.");
                emissionVehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId("defaultVehicle"), VehicleUtils.getDefaultVehicleType());
            } else {
                // a person is given -> use the vehicle for that person given in emissionModule
                emissionVehicle = this.emissionVehicles.getVehicles().get(Id.createVehicleId(person.getId()));
            }
        }

        double logNormalRnd = 1. ;
        if ( sigma != 0. ) {
            logNormalRnd = (double) person.getCustomAttributes().get("logNormalRnd") ;
        }

        double linkExpectedEmissionDisutility;

        double linkTravelTime = this.timeCalculator.getLinkTravelTime(link, time, person, v);

        if(hotspotLinks == null){
            // pricing applies for all links
            linkExpectedEmissionDisutility = calculateExpectedEmissionDisutility(emissionVehicle, link, link.getLength(), linkTravelTime, time);
        } else {
            // pricing applies for the current link
            if(hotspotLinks.contains(link.getId())) linkExpectedEmissionDisutility = calculateExpectedEmissionDisutility(emissionVehicle, link, link.getLength(), linkTravelTime, time);
                // pricing applies not for the current link
            else linkExpectedEmissionDisutility = 0.0;
        }

		return randomizedTimeDistanceDisutilityForLink + linkExpectedEmissionDisutility * logNormalRnd;
    }

    private double calculateExpectedEmissionDisutility(Vehicle vehicle, Link link, double distance, double linkTravelTime, double time) {
        double linkExpectedEmissionDisutility;

		/* The following is an estimate of the warm emission costs that an agent (depending on her vehicle type and
		the average travel time on that link in the last iteration) would have to pay if chosing that link in the next
		iteration. Cold emission costs are assumed not to change routing; they might change mode choice or
		location choice (not implemented)! */

        WarmEmissionAnalysisModule warmEmissionAnalysisModule = this.emissionModule.getWarmEmissionAnalysisModule();
        Map<WarmPollutant, Double> expectedWarmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
                vehicle,
                Integer.parseInt(NetworkUtils.getType(((Link) link))),
                link.getFreespeed(),
                distance,
                linkTravelTime
        );
        double expectedEmissionCosts = this.emissionResponsibilityCostModule.calculateWarmEmissionCosts(expectedWarmEmissions, link.getId(), time);
        linkExpectedEmissionDisutility = this.marginalUtlOfMoney * expectedEmissionCosts ;

        return linkExpectedEmissionDisutility;
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        throw new UnsupportedOperationException();
    }
}
