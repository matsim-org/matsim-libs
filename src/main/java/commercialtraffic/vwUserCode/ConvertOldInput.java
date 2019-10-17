/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package commercialtraffic.vwUserCode;

import commercialtraffic.commercialJob.CommercialJobUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashMap;
import java.util.Map;

public class ConvertOldInput {


    public static void main(String[] args) {

        Carriers oldCarriers = new Carriers();
        CarrierPlanXmlReader carrierReader = new CarrierPlanXmlReader(oldCarriers);

        carrierReader.readFile("C:/Users/Work/tubCloud/VSP_WiMi/VW/commercialTraffic/AP1.1/input_ap1.1/neu_mehrereJobsProAktivität/AP1.1/Carrier/carrier_definition.xml");

        Map<Id<CarrierService>, CarrierService> serviceRegistry = new HashMap<>();
        Map<Id<CarrierService>, Id<Carrier>> service2CarrierId = new HashMap<>();

        for (Carrier carrier : oldCarriers.getCarriers().values()) {
            for (CarrierService service : carrier.getServices().values()) {
                serviceRegistry.put(service.getId(), service);
                service2CarrierId.put(service.getId(), carrier.getId());
            }
            carrier.getServices().clear();
            CarrierUtils.setCarrierMode(carrier, TransportMode.drt);
            CarrierUtils.setJspritIterations(carrier, 50);
            carrier.getAttributes().putAttribute(CommercialJobUtils.CARRIER_MARKET_ATTRIBUTE_NAME, carrier.getId().toString().split("_")[0]);
        }

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        PopulationReader populationReader = new PopulationReader(scenario);
        populationReader.readFile("C:/Users/Work/tubCloud/VSP_WiMi/VW/commercialTraffic/AP1.1/input_ap1.1/neu_mehrereJobsProAktivität/AP1.1/Population/populationWithCTdemand_jobIds.xml.gz");

        for (Person person : scenario.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        Activity activity = (Activity) planElement;
                        if (activity.getAttributes().getAttribute("jobId") != null) {
                            String attValue = String.valueOf(activity.getAttributes().removeAttribute("jobId"));
                            String[] jobIds = attValue.split(";");
                            for (int i = 0; i < jobIds.length; i++) {
                                CarrierService correspondingService = serviceRegistry.get(Id.create(jobIds[i], CarrierService.class));
                                Id<Carrier> carrier = service2CarrierId.get(correspondingService.getId());
                                CommercialJobUtils.addCustomerCommercialJobAttribute(activity,
                                        carrier,
                                        correspondingService.getCapacityDemand(),
                                        correspondingService.getServiceStartTimeWindow().getStart(),
                                        correspondingService.getServiceStartTimeWindow().getEnd(),
                                        correspondingService.getServiceDuration());
                            }
                        }
                    }
                }
            }
        }

        String outputCarrierFile = "C:/Users/Work/tubCloud/VSP_WiMi/VW/commercialTraffic/AP1.1/input_ap1.1/neu_mehrereJobsProAktivität/AP1.1/Carrier/carrier_definition_wOServices_drt.xml";
        new CarrierPlanXmlWriterV2(oldCarriers).write(outputCarrierFile);
        new PopulationWriter(scenario.getPopulation()).write("C:/Users/Work/tubCloud/VSP_WiMi/VW/commercialTraffic/AP1.1/input_ap1.1/neu_mehrereJobsProAktivität/AP1.1/Population/populationWithCTdemand_serviceAtts.xml.gz");

        Carriers outputCarriers = new Carriers();
        new CarrierPlanXmlReader(outputCarriers).readFile(outputCarrierFile);

    }
}
