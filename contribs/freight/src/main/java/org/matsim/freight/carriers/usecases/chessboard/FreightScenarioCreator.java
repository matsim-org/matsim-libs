/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.usecases.chessboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * Creates chessboard freight scenario.
 *
 * @author stefan
 *
 */
final class FreightScenarioCreator {

    static int agentCounter = 1;
    static final Random random = new Random(Long.MAX_VALUE);

    public static void main(String[] args) {

        Config config = new Config();
        config.addCoreModules();
        Scenario scenario = ScenarioUtils.createScenario(config);
        new MatsimNetworkReader(scenario.getNetwork()).readFile("input/usecases/chessboard/network/grid9x9.xml");



        //carriers
        Carriers carriers = new Carriers();

        for(int i=1;i<10;i++){
            Id<Link> homeId = Id.createLinkId("i("+i+",9)R");
            Carrier carrier = CarriersUtils.createCarrier(Id.create(agentCounter,Carrier.class ) );
            createFleet(homeId, carrier);
            createCustomers(carrier,scenario.getNetwork());
            agentCounter++;
            carriers.addCarrier(carrier);

            Id<Link> homeIdR = Id.createLinkId("i("+i+",0)");
            Carrier carrier_ = CarriersUtils.createCarrier(Id.create(agentCounter,Carrier.class ) );
            createFleet(homeIdR, carrier_);
            createCustomers(carrier_,scenario.getNetwork());
            agentCounter++;
            carriers.addCarrier(carrier_);
        }

        for(int i=1;i<10;i++){
            Id<Link> homeId = Id.createLinkId("j(0,"+i+")R");
            Carrier carrier = CarriersUtils.createCarrier(Id.create(agentCounter,Carrier.class ) );
            createFleet(homeId, carrier);
            createCustomers(carrier,scenario.getNetwork());
            agentCounter++;
            carriers.addCarrier(carrier);

            Id<Link> homeIdR = Id.createLinkId("j(9,"+i+")");
            Carrier carrier_ = CarriersUtils.createCarrier(Id.create(agentCounter,Carrier.class ) );
            createFleet(homeIdR, carrier_);
            createCustomers(carrier_,scenario.getNetwork());
            agentCounter++;
            carriers.addCarrier(carrier_);
        }

		CarriersUtils.writeCarriers(carriers, "input/usecases/chessboard/freight/multipleCarriers_withoutTW_withDepots_withoutPlan.xml");
    }

    private static void createCustomers(Carrier carrier, Network network) {
        List<Id<Link>> innerCityLinks = createInnerCityLinks(network);
        List<Id<Link>> outerCityLinks = createOuterCityLinks(network);

        for(int i=0;i<20;i++){
            CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(Id.create((i + 1),CarrierService.class), drawLocationLinkId(innerCityLinks, outerCityLinks));
            serviceBuilder.setCapacityDemand(1);
            serviceBuilder.setServiceDuration(5*60);
			serviceBuilder.setServiceStartingTimeWindow(TimeWindow.newInstance(6*60*60, 15*60*60));
			CarrierService carrierService = serviceBuilder.build();
            CarriersUtils.addService(carrier, carrierService);
        }
    }

    private static Id<Link> drawLocationLinkId(List<Id<Link>> innerCityLinks, List<Id<Link>> outerCityLinks) {
        double probInner = 0.5;
        double randomFigure = random.nextDouble();
        if(randomFigure <= probInner){
            int randomIndex = random.nextInt(innerCityLinks.size());
            return innerCityLinks.get(randomIndex);
        }
        else{
            int randomIndex = random.nextInt(outerCityLinks.size());
            return outerCityLinks.get(randomIndex);
        }
    }

    private static List<Id<Link>> createOuterCityLinks(Network network) {
        List<Id<Link>> inner = new InnerOuterCityScenarioCreator().getInnerCityLinks();
        List<Id<Link>> outer = new ArrayList<>();
        for(Id<Link> id : network.getLinks().keySet()){
            if(!inner.contains(id)){
                outer.add(id);
            }
        }
        return outer;
    }

    private static List<Id<Link>> createInnerCityLinks(Network network) {
        List<Id<Link>> inner = new InnerOuterCityScenarioCreator().getInnerCityLinks();
        List<Id<Link>> innerCityLinkIds = new ArrayList<>();
        for(Id<Link> id : inner){
            if(network.getLinks().containsKey(id)){
                innerCityLinkIds.add(id);
            }
        }
        return innerCityLinkIds;
    }

    private static void createFleet(Id<Link> homeId, Carrier carrier) {
        Id<Link> oppositeId = getOpposite(homeId);

        //light
        CarrierVehicle carrierVehicle_lightA = createLightVehicle(carrier.getId(), homeId, "a");
        CarriersUtils.addCarrierVehicle(carrier, carrierVehicle_lightA);
        CarrierVehicle carrierVehicle_lightB = createLightVehicle(carrier.getId(), oppositeId, "b");
        CarriersUtils.addCarrierVehicle(carrier, carrierVehicle_lightB);

        //heavy
        CarrierVehicle carrierVehicle_heavyA = createHeavyVehicle(carrier.getId(), homeId, "a");
        CarriersUtils.addCarrierVehicle(carrier, carrierVehicle_heavyA);
        CarrierVehicle carrierVehicle_heavyB = createHeavyVehicle(carrier.getId(), oppositeId, "b");
        CarriersUtils.addCarrierVehicle(carrier, carrierVehicle_heavyB);

        carrier.getCarrierCapabilities().setFleetSize(FleetSize.INFINITE);
    }

    public static Id<Link> getOpposite(Id<Link> homeId) {
        if(homeId.toString().startsWith("i")){
            String opposite = "i(" + homeId.toString().substring(2,4);
            if(homeId.toString().charAt(4) == '0') opposite += "9)R";
            else opposite += "0)";
            return Id.createLinkId(opposite);
        }
        else{
            String opposite = "j(";
            if(homeId.toString().charAt(2) == '0') {
                opposite += "9," + homeId.toString().substring(4,6);
            }
            else opposite += "0," + homeId.toString().substring(4,6) + "R";
            return Id.createLinkId(opposite);
        }
    }

    private static CarrierVehicle createLightVehicle(Id<?> id, Id<Link> homeId, String depot) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create(("carrier_"+id.toString()+"_lightVehicle_" + depot) ,Vehicle.class), homeId, createLightType() );
        vBuilder.setEarliestStart(6*60*60);
        vBuilder.setLatestEnd(16*60*60);
//        vBuilder.setType(createLightType());
        return vBuilder.build();
    }

    private static VehicleType createLightType() {
	    VehicleType typeBuilder = VehicleUtils.getFactory().createVehicleType( Id.create( "small", VehicleType.class ) );
        typeBuilder.getCapacity().setWeightInTons( 6. ) ;
        typeBuilder.getCostInformation().setFixedCost(80.0 );
        typeBuilder.getCostInformation().setCostsPerMeter( 0.00047 );
        typeBuilder.getCostInformation().setCostsPerSecond( 0.008 );
        return typeBuilder ;
    }

    private static CarrierVehicle createHeavyVehicle(Id<?> id, Id<Link> homeId, String depot) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create("carrier_" + id.toString() + "_heavyVehicle_" + depot, Vehicle.class), homeId, createHeavyType() );
        vBuilder.setEarliestStart(6*60*60);
        vBuilder.setLatestEnd(16*60*60);
//        vBuilder.setType(createHeavyType());
        return vBuilder.build();
    }

    private static VehicleType createHeavyType() {
	    VehicleType typeBuilder = VehicleUtils.getFactory().createVehicleType( Id.create( "heavy", VehicleType.class ) );
	    typeBuilder.getCapacity().setWeightInTons( 25 ) ;
	    typeBuilder.getCostInformation().setFixedCost( 130.0 ) ;
	    typeBuilder.getCostInformation().setCostsPerMeter( 0.00077 ) ;
	    typeBuilder.getCostInformation().setCostsPerSecond( 0.008 ) ;
	    return typeBuilder ;
    }



}
