/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiverUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.freight.receiver.run.chessboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.CarrierCapabilities.FleetSize;
import org.matsim.freight.receiver.*;
import org.matsim.freight.receiver.collaboration.CollaborationUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.examples.ExamplesUtils;
import org.matsim.vehicles.VehicleType;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

/**
 * Various utilities for building receiver scenarios (for now).
 *
 * @author jwjoubert, wlbean
 */
public class ReceiverChessboardScenario {
    private final static Logger LOG = LogManager.getLogger(ReceiverChessboardScenario.class);


    /**
     * Build the entire chessboard example.
     */
    public static Scenario createChessboardScenario(long seed, int numberOfReceivers, String outputFolder, boolean write) {
        MatsimRandom.reset(seed);

        Config config = setupChessboardConfig(seed, outputFolder);

        Scenario sc = ScenarioUtils.loadScenario(config);

        createChessboardCarriersAndAddToScenario(sc);

        ConfigUtils.addOrGetModule(sc.getConfig(), ReceiverConfigGroup.class).setReceiverReplanningInterval(5);

        /* Create the grand coalition receiver members and allocate orders. */
        createAndAddChessboardReceivers(sc, numberOfReceivers);

        createReceiverOrders(sc);

        /* Let jsprit do its magic and route the given receiver orders. */
//		generateCarrierPlan( sc );
        // needs to be done in iterations startup listener, where it is also done during the iterations.  kai, jan'19

        if (write) {
            writeFreightScenario(sc);
        }

        /* Link the carriers to the receivers. */
        CollaborationUtils.linkReceiverOrdersToCarriers(ReceiverUtils.getReceivers(sc), CarriersUtils.getCarriers(sc));
        CollaborationUtils.createCoalitionWithCarriersAndAddCollaboratingReceivers(sc);
        return sc;
    }


    /**
     * FIXME Need to complete this.
     */
    private static Config setupChessboardConfig(long seed, String outputFolder) {
        URL context = ExamplesUtils.getTestScenarioURL("freight-chessboard-9x9");

        Config config = ConfigUtils.createConfig();

        config.setContext(context);

		config.controller().setOutputDirectory(outputFolder);
        config.controller().setFirstIteration(0);
        config.controller().setLastIteration(ReceiverChessboardParameters.NUM_ITERATIONS);
        config.controller().setMobsim("qsim");
        config.controller().setWriteSnapshotsInterval(ReceiverChessboardParameters.STAT_INTERVAL);
        config.global().setRandomSeed(seed);
        config.network().setInputFile("grid9x9.xml");

		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        return config;
    }

	/**
	 * Write the scenario to the output folder defined in the scenario's config object.
	 */
    public static void writeFreightScenario(Scenario sc) {
        /* Write the necessary bits to file. */
        String outputFolder = sc.getConfig().controller().getOutputDirectory();
		boolean success = new File(outputFolder).mkdirs();
        if (!success) {
            LOG.warn("Could not successfully create '" + outputFolder + "'. Maybe it already exists?");
        }

        new NetworkWriter(sc.getNetwork()).write(outputFolder + "network.xml");
        new ConfigWriter(sc.getConfig()).write(outputFolder + "config.xml");
        CarriersUtils.writeCarriers(CarriersUtils.getCarriers(sc),outputFolder + "carriers.xml");
        new ReceiversWriter(ReceiverUtils.getReceivers(sc)).write(outputFolder + "receivers.xml");

        /* Write the vehicle types. FIXME This will have to change so that vehicle
         * types lie at the Carriers level, and not per Carrier. In this scenario
         * there luckily is only a single Carrier. */
        new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(CarriersUtils.getCarriers(sc))).write(outputFolder + "carrierVehicleTypes.xml");
    }


    /**
     * Creates the product orders for the receiver agents in the simulation. Currently (28/08/2018) all the receivers have the same orders
     * for experiments, but this must be adapted in the future to accept other parameters as inputs to enable different orders per receiver.
     */
    private static void createReceiverOrders(Scenario sc) {
        Carriers carriers = CarriersUtils.getCarriers(sc);
        Receivers receivers = ReceiverUtils.getReceivers(sc);
        Carrier carrierOne = carriers.getCarriers().get(Id.create("Carrier1", Carrier.class));

        /* Try and get the first Carrier vehicle so that we can get its origina link.
         * FIXME We want the carrier's location to rather be an attribute of the
         * Carrier, but currently (Feb 19, JWJ) Carrier is not Attributable.
         */
        Iterator<CarrierVehicle> vehicles = carrierOne.getCarrierCapabilities().getCarrierVehicles().values().iterator();
        if (!vehicles.hasNext()) {
            throw new RuntimeException("Must have vehicles to get origin link!");
        }
        Id<Link> carrierOriginLinkId = vehicles.next().getLinkId();

        /* Create generic product types with a description and required capacity (in kg per item). */
        ProductType productTypeOne = ReceiverUtils.createAndGetProductType(receivers, Id.create("P1", ProductType.class), carrierOriginLinkId);
        productTypeOne.setDescription("Product 1");
        productTypeOne.setRequiredCapacity(1);

        ProductType productTypeTwo = ReceiverUtils.createAndGetProductType(receivers, Id.create("P2", ProductType.class), carrierOriginLinkId);
        productTypeTwo.setDescription("Product 2");
        productTypeTwo.setRequiredCapacity(2);

        for (int r = 1; r < ReceiverUtils.getReceivers(sc).getReceivers().size() + 1; r++) {
            int tw = ReceiverChessboardParameters.TIME_WINDOW_DURATION_IN_HOURS;
            int numDel = ReceiverChessboardParameters.NUM_DELIVERIES;
            String serdur = ReceiverChessboardParameters.SERVICE_TIME;

            /* Create receiver-specific products */
            Receiver receiver = receivers.getReceivers().get(Id.create(Integer.toString(r), Receiver.class));

            ReceiverProduct receiverProductOne = createReceiverProduct(productTypeOne, 1000, 5000);
            receiver.addProduct(receiverProductOne);

            ReceiverProduct receiverProductTwo = createReceiverProduct(productTypeTwo, 500, 2500);
            receiver.addProduct(receiverProductTwo);

            /* Generate and collate orders for the different receiver/order combination. */
            Collection<Order> rOrders = new ArrayList<>();
            {
                Order rOrder1 = createProductOrder(Id.create("Order" + r + "1", Order.class), receiver,
                        receiverProductOne, Time.parseTime(serdur));
                rOrder1.setNumberOfWeeklyDeliveries(numDel);
                rOrders.add(rOrder1);
            }
            {
                Order rOrder2 = createProductOrder(Id.create("Order" + r + "2", Order.class), receiver,
                        receiverProductTwo, Time.parseTime(serdur));
                rOrder2.setNumberOfWeeklyDeliveries(numDel);
                rOrders.add(rOrder2);
            }

            /* Combine product orders into single receiver order for a specific carrier. */
            ReceiverOrder receiverOrder = new ReceiverOrder(receiver.getId(), rOrders, carrierOne.getId());
            ReceiverPlan receiverPlan = ReceiverPlan.Builder.newInstance(receiver, true)
                    .addReceiverOrder(receiverOrder)
//					// setting the time window start time of ALL receivers to 08:00 for time window experiments.
//					.addTimeWindow(TimeWindow.newInstance(6*3600,(6*3600+tw*3600)))
                    // selecting a random time window start time.
                    .addTimeWindow(selectRandomTimeStart(tw))
                    .build();
            receiver.setSelectedPlan(receiverPlan);
            receiver.getAttributes().putAttribute(
                    ReceiverUtils.ATTR_RECEIVER_TW_COST,
                    ReceiverChessboardParameters.TIME_WINDOW_HOURLY_COST);

            /* Convert receiver orders to initial carrier shipment. */
            convertReceiverOrdersToInitialCarrierShipments(carriers, receiverOrder, receiverPlan);
        }

    }


    public static void convertReceiverOrdersToInitialCarrierShipments(Carriers carriers, ReceiverOrder receiverOrder, ReceiverPlan receiverPlan) {
        for (Order order : receiverOrder.getReceiverProductOrders()) {

            CarrierShipment.Builder shpBuilder = CarrierShipment.Builder
                    .newInstance(Id.create(order.getId(), CarrierShipment.class),
                            order.getProduct().getProductType().getOriginLinkId(),
                            order.getReceiver().getLinkId(),
                            (int) (Math.round(order.getDailyOrderQuantity() * order.getProduct().getProductType().getRequiredCapacity())));

            if (receiverPlan.getTimeWindows().size() > 1) {
                LOG.warn("Multiple time windows set. Only the first is used");
            }

            CarrierShipment shipment = shpBuilder.setDeliveryDuration(order.getServiceDuration())
                    .setDeliveryStartingTimeWindow(receiverPlan.getTimeWindows().get(0))
                    .build();
            carriers.getCarriers().get(receiverOrder.getCarrierId()).getShipments().put(shipment.getId(), shipment);
        }
    }


    /**
     * Creates and adds the receivers that are part of the grand coalition. These receivers are allowed to replan
     * their orders as well as decided to join or leave the coalition.
     */
    static void createAndAddChessboardReceivers(Scenario sc, int numberOfReceivers) {
        Network network = sc.getNetwork();

        Receivers receivers = ReceiverUtils.createReceivers();

        receivers.setDescription("Chessboard");

        for (int r = 1; r < numberOfReceivers + 1; r++) {
            Id<Link> receiverLocation = selectRandomLink(network);
            Receiver receiver = ReceiverUtils.newInstance(Id.create(Integer.toString(r), Receiver.class)).setLinkId(receiverLocation);
            receiver.getAttributes().putAttribute(CollaborationUtils.ATTR_GRANDCOALITION_MEMBER, true);
            receiver.getAttributes().putAttribute(CollaborationUtils.ATTR_COLLABORATION_STATUS, true);
            receivers.addReceiver(receiver);
        }

        ReceiverUtils.setReceivers(receivers, sc);
    }


    /**
     * Creates the carrier agents for the simulation.
     */
    private static void createChessboardCarriersAndAddToScenario(Scenario sc) {
        Id<Carrier> carrierId = Id.create("Carrier1", Carrier.class);
        Carrier carrier = CarriersUtils.createCarrier(carrierId);
        Id<Link> carrierLocation = selectRandomLink(sc.getNetwork());

        CarrierCapabilities.Builder capBuilder = CarrierCapabilities.Builder.newInstance();
        CarrierCapabilities carrierCap = capBuilder.setFleetSize(FleetSize.INFINITE).build();
        carrier.setCarrierCapabilities(carrierCap);
        LOG.info("Created a carrier with capabilities.");

        /*
         * Create the carrier vehicle types.
         * TODO This might, potentially, be read from XML file.
         */

        /* Heavy vehicle. */
        CarrierVehicleType.Builder typeBuilderHeavy = CarrierVehicleType.Builder.newInstance(Id.create("heavy", VehicleType.class));
        VehicleType typeHeavy = typeBuilderHeavy
                .setCapacity(14000)
                .setFixCost(2604)
                .setCostPerDistanceUnit(7.34E-3)
                .setCostPerTimeUnit(0.171)
                .build();
        CarrierVehicle.Builder carrierHVehicleBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("heavy"), carrierLocation, typeHeavy);
        CarrierVehicle heavy = carrierHVehicleBuilder
                .setEarliestStart(Time.parseTime("06:00:00"))
                .setLatestEnd(Time.parseTime("18:00:00"))
                .build();

        /* Light vehicle. */
        CarrierVehicleType.Builder typeBuilderLight = CarrierVehicleType.Builder.newInstance(Id.create("light", VehicleType.class));
        VehicleType typeLight = typeBuilderLight
                .setCapacity(3000)
                .setFixCost(1168)
                .setCostPerDistanceUnit(4.22E-3)
                .setCostPerTimeUnit(0.089)
                .build();
        CarrierVehicle.Builder carrierLVehicleBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("light"), carrierLocation, typeLight);
        CarrierVehicle light = carrierLVehicleBuilder
                .setEarliestStart(Time.parseTime("06:00:00"))
                .setLatestEnd(Time.parseTime("18:00:00"))
                .build();

        /* Assign vehicles to carrier. */
        carrier.getCarrierCapabilities().getCarrierVehicles().put(heavy.getId(), heavy);
        carrier.getCarrierCapabilities().getVehicleTypes().add(typeHeavy);
        carrier.getCarrierCapabilities().getCarrierVehicles().put(light.getId(), light);
        carrier.getCarrierCapabilities().getVehicleTypes().add(typeLight);
        LOG.info("Added different vehicle types to the carrier.");

        CarrierVehicleTypes types = new CarrierVehicleTypes();
        types.getVehicleTypes().put(typeLight.getId(), typeLight);
        types.getVehicleTypes().put(typeHeavy.getId(), typeHeavy);

        Carriers carriers = CarriersUtils.addOrGetCarriers(sc);
        carriers.addCarrier(carrier);

    }


    /**
     * Selects a random link in the network.
     */
    @SuppressWarnings("unchecked")
    static Id<Link> selectRandomLink(Network network) {
        Object[] linkIds = network.getLinks().keySet().toArray();
        int sample = MatsimRandom.getRandom().nextInt(linkIds.length);
        Object o = linkIds[sample];
        Id<Link> linkId;
        if (o instanceof Id<?>) {
            linkId = (Id<Link>) o;
            return linkId;
        } else {
            throw new RuntimeException("Oops, cannot find a correct link Id.");
        }
    }


    /**
     * This method assigns a specific product type to a receiver, and allocates that receiver's order
     * policy.
     * <p>
     * TODO This must be made more generic so that multiple policies can be considered. Currently (2018/04
     * this is hard-coded to be a min-max (s,S) reordering policy.
     */
    private static ReceiverProduct createReceiverProduct(ProductType productType, int minLevel, int maxLevel) {
        ReceiverProduct.Builder builder = ReceiverProduct.Builder.newInstance();
        return builder
                .setReorderingPolicy(ReceiverUtils.createSSReorderPolicy(minLevel, maxLevel))
                .setProductType(productType)
                .build();
    }

    /**
     * Create a receiver order for different products.
     */
    private static Order createProductOrder(Id<Order> number, Receiver receiver, ReceiverProduct receiverProduct, double serviceTime) {
        Order.Builder builder = Order.Builder.newInstance(number, receiver, receiverProduct);

        return builder
                .calculateOrderQuantity()
                .setServiceTime(serviceTime)
                .build();
    }

    static TimeWindow selectRandomTimeStart(int tw) {
        int min = 6;
        int max = 18;
//		Random randomTime = new Random();
        Random randomTime = MatsimRandom.getLocalInstance(); // overkill, but easiest to retrofit.  kai, jan'19
        int randomStart = (min +
                randomTime.nextInt(max - tw - min + 1));
        return TimeWindow.newInstance(randomStart * 3600, randomStart * 3600 + tw * 3600);
    }

}
