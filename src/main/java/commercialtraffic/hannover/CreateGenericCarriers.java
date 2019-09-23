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

package commercialtraffic.hannover;/*
 * created by jbischoff, 20.06.2019
 */

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class CreateGenericCarriers {
    public static void main(String[] args) {
        String folder = "C:\\Users\\Joschka\\Documents\\shared-svn\\projects\\vw_rufbus\\commercialtraffic\\example\\input\\";

        Carriers carriers = new Carriers();
        Carrier carrier1 = CarrierImpl.newInstance(Id.create("carrier1", Carrier.class));
        Carrier carrier2 = CarrierImpl.newInstance(Id.create("carrier2", Carrier.class));
//        Carrier carrier3 = CarrierImpl.newInstance(Id.create("smallParcel_op3", Carrier.class));
//        Carrier carrier4 = CarrierImpl.newInstance(Id.create("smallParcel_op4", Carrier.class));

        carrier1.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
        carrier1.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(1), Id.createLinkId(340834), "dhlhannover"));
        carrier1.getCarrierCapabilities().getVehicleTypes().add(createLightType());


        carrier2.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
        carrier2.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(1), Id.createLinkId(62912), "dpdlehrte"));
        carrier2.getCarrierCapabilities().getVehicleTypes().add(createLightType());


//        carrier3.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
//        carrier3.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(1), Id.createLinkId(340834), "dhlhannover2"));
//        carrier3.getCarrierCapabilities().getVehicleTypes().add(createLightType());
//
//
//        carrier4.getCarrierCapabilities().setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
//        carrier4.getCarrierCapabilities().getCarrierVehicles().add(getLightVehicle(Id.createVehicleId(1), Id.createLinkId(62912), "dpdlehrte2"));
//        carrier4.getCarrierCapabilities().getVehicleTypes().add(createLightType());


        carriers.addCarrier(carrier1);
        carriers.addCarrier(carrier2);
//        carriers.addCarrier(carrier3);
//        carriers.addCarrier(carrier4);

        new CarrierPlanWriter(carriers.getCarriers().values()).write(folder + "carrier_definition.xml");
        new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(folder + "carrier_vehicletypes.xml");


    }


    public static CarrierVehicle getLightVehicle(Id<?> id, Id<Link> homeId, String depot) {
        CarrierVehicle.Builder vBuilder = CarrierVehicle.Builder.newInstance(Id.create(("carrier_" + id.toString() + "_lightVehicle_" + depot), Vehicle.class), homeId);
        vBuilder.setEarliestStart(6 * 60 * 60);
        vBuilder.setLatestEnd(16 * 60 * 60);
        vBuilder.setType(createLightType());
        return vBuilder.build();
    }

    public static VehicleType createLightType() {
        VehicleType type = VehicleUtils.createVehicleType(Id.create("small", VehicleType.class));
        type.getCapacity().setOther(100);
        type.getCostInformation().setFixedCost(80.0);
        type.getCostInformation().setCostsPerMeter(0.00047);
        type.getCostInformation().setCostsPerSecond(0.008);
        return type;
    }
}
