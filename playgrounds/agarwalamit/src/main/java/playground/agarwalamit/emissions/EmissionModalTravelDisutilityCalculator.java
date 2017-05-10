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

package playground.agarwalamit.emissions;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.WarmEmissionAnalysisModule;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.vsp.airPollution.flatEmissions.EmissionCostModule;

/**
 * @author benjamin
 *
 */
class EmissionModalTravelDisutilityCalculator implements TravelDisutility {

    public static final Logger LOGGER = Logger.getLogger(EmissionModalTravelDisutilityCalculator.class);

    private final TravelDisutility travelDisutility;
    private final TravelTime travelTime;
    private final double marginalUtlOfMoney;
    private final EmissionModule emissionModule;
    private final EmissionCostModule emissionCostModule;
    private final Set<Id<Link>> hotspotLinks;
    private final Vehicles emissionVehicles;
    private final QSimConfigGroup qSimConfigGroup;


    public EmissionModalTravelDisutilityCalculator(TravelDisutility travelDisutility,
                                                   TravelTime travelTime,
                                                   PlanCalcScoreConfigGroup cnScoringGroup,
                                                   EmissionModule emissionModule,
                                                   EmissionCostModule emissionCostModule,
                                                   Set<Id<Link>> hotspotLinks,
                                                   Vehicles vehicles,
                                                   QSimConfigGroup qSimConfigGroup ) {
        this.travelDisutility = travelDisutility;
        this.travelTime = travelTime;
        this.marginalUtlOfMoney = cnScoringGroup.getMarginalUtilityOfMoney();
        this.emissionModule = emissionModule;
        this.emissionCostModule = emissionCostModule;
        this.hotspotLinks = hotspotLinks;
        this.emissionVehicles = vehicles;
        this.qSimConfigGroup = qSimConfigGroup;
    }

    @Override
    public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle v) {

        double travelDisutil = 0.;

        if (this.travelDisutility!=null) {
            travelDisutil = this.travelDisutility.getLinkTravelDisutility(link,time, person, v);
        } else {
            throw new RuntimeException("Travel disutility is null. Aborting...");
        }

        double linkExpectedEmissDiutility = 0.;
        Vehicle emissionVehicle = v;

        if(v==null && person==null) {
            LOGGER.error("Both, person and vehicle are null. Aborting...");
        } else if (v == null){
            switch (this.qSimConfigGroup.getVehiclesSource()){
                case defaultVehicle:
                    emissionVehicle = VehicleUtils.getFactory().createVehicle(Id.createVehicleId("defaultVehicle"), VehicleUtils.getDefaultVehicleType());
                    break;
                case modeVehicleTypesFromVehiclesData:
                    //TODO : this should be fixed in near future when vehicle id generation is moved to PersonPerpareForSim rather than in PopulationAgentSource. Amit May'17
                    // this is more tricky, one has to assume that vehicle Id starts with person id
                    Set<Id<Vehicle>> vehicleIds = this.emissionVehicles.getVehicles()
                                                                       .keySet()
                                                                       .parallelStream()
                                                                       .filter(e -> e.toString().startsWith(person.getId().toString()))
                                                                       .collect(Collectors.toSet());
                    if (vehicleIds.size()==0) {
                        LOGGER.warn("No vehicle id is found for person "+ person.getId()+". Travel disutility for emissions is not estimated in this case.");
                    }
                    else if(vehicleIds.size()==1) emissionVehicle = this.emissionVehicles.getVehicles().get(vehicleIds.iterator().next());
                    else LOGGER.warn("Several vehicles are found for person "+ person.getId()+". Don't know, which vehicle should be used. Travel disutility for emissions is not estimated in this case.");
                    break;
                case fromVehiclesData: // one has to assume that person and vehicle ids are same. Amit May'17
                    emissionVehicle = this.emissionVehicles.getVehicles().get(Id.createVehicleId(person.getId()));
                    break;
            }
        } else if (v!=null) {
            throw new RuntimeException("The vehicle in the travel disutility is not null anymore. This is good, check and update the code.");
        }

        if( emissionVehicle!=null) {
            String mode = emissionVehicle.getType().getId().toString(); // TODO : for old emissionVehicles set up, mode might not be car.

            double linkTravelTime = this.travelTime.getLinkTravelTime(link, time, person, emissionVehicle);

            if(hotspotLinks == null){
                linkExpectedEmissDiutility = calculateExpectedEmissionDisutility(emissionVehicle, link, link.getLength(), linkTravelTime);
            } else {
                if(hotspotLinks.contains(link.getId())) linkExpectedEmissDiutility = calculateExpectedEmissionDisutility(emissionVehicle, link, link.getLength(), linkTravelTime);
                else linkExpectedEmissDiutility = 0.0;
            }
        }

        return travelDisutil + linkExpectedEmissDiutility;
    }

    private double calculateExpectedEmissionDisutility(Vehicle vehicle, Link link, double distance, double linkTravelTime) {
        double linkExpectedEmissionDisutility;

		/* The following is an estimate of the warm emission costs that an agent (depending on her vehicle type and
		the average travel time on that link in the last iteration) would have to pay if chosing that link in the next
		iteration. Cold emission costs are assumed not to change routing; they might change mode choice or
		location choice (not implemented)! */

        WarmEmissionAnalysisModule warmEmissionAnalysisModule = this.emissionModule.getWarmEmissionAnalysisModule();
        Map<WarmPollutant, Double> expectedWarmEmissions = warmEmissionAnalysisModule.checkVehicleInfoAndCalculateWarmEmissions(
                vehicle,
                Integer.parseInt(NetworkUtils.getType(link)),
                link.getFreespeed(),
                distance,
                linkTravelTime
        );
        double expectedEmissionCosts = this.emissionCostModule.calculateWarmEmissionCosts(expectedWarmEmissions);
        linkExpectedEmissionDisutility = this.marginalUtlOfMoney * expectedEmissionCosts ;

        return linkExpectedEmissionDisutility;
    }

    @Override
    public double getLinkMinimumTravelDisutility(Link link) {
        throw new UnsupportedOperationException();
    }
}
