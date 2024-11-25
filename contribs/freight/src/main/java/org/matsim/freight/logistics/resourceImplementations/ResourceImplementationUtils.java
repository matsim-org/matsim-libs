/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2022 by the members listed in the COPYING,        *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.freight.logistics.resourceImplementations;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.LSPResourceScheduler;
import org.matsim.freight.logistics.LogisticChainElement;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;
import org.matsim.vehicles.VehicleType;

@SuppressWarnings("ClassEscapesDefinedScope")
public class ResourceImplementationUtils {

  private static final Logger log = LogManager.getLogger(ResourceImplementationUtils.class);
  private static final String CARRIER_TYPE_ATTR = "carrierType";

  public static SimpleForwardLogisticChainScheduler
      createDefaultSimpleForwardLogisticChainScheduler(List<LSPResource> resources) {
    return new SimpleForwardLogisticChainScheduler(resources);
  }

  public static SingleLogisticChainShipmentAssigner createSingleLogisticChainShipmentAssigner() {
    return new SingleLogisticChainShipmentAssigner();
  }

  /**
   * Collects all the vehicleTyps from the different Vehicle of the carrier. This is needed since we
   * do not use carrier.getCarrierCapabilities().getVehicleTypes() any more as second field to safe
   * vehicleTypes ... TODO: Maybe move to CarriersUtils in MATSim-libs / freight contrib.
   *
   * <p>KMT/Jul22
   *
   * @param carrier the carrier
   * @return Collection of VehicleTypes
   */
  public static Collection<VehicleType> getVehicleTypeCollection(Carrier carrier) {
    Set<VehicleType> vehicleTypeCollection = new HashSet<>();
    for (CarrierVehicle carrierVehicle :
        carrier.getCarrierCapabilities().getCarrierVehicles().values()) {
      vehicleTypeCollection.add(carrierVehicle.getType());
    }
    return vehicleTypeCollection;
  }

  public static void printShipmentsOfLSP(String outputDir, LSP lsp) {
    System.out.println("Writing out shipments of LSP");
    try (BufferedWriter writer =
        IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_shipments.tsv")) {
      final String str0 = "LSP: " + lsp.getId();
      System.out.println(str0);
      writer.write(str0 + "\n");
      for (LspShipment lspShipment : lsp.getLspShipments()) {
        final String str1 = "Shipment: " + lspShipment;
        System.out.println(str1);
        writer.write(str1 + "\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printResults_shipmentPlan(String outputDir, LSP lsp) {
    System.out.println("Writing out shipmentPlan for LSP");
    try (BufferedWriter writer =
        IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_schedules.tsv")) {
      final String str0 = "LSP: " + lsp.getId();
      System.out.println(str0);
      writer.write(str0 + "\n");
      for (LspShipment shipment : lsp.getLspShipments()) {
        ArrayList<LspShipmentPlanElement> elementList =
            new ArrayList<>(
                LspShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId())
                    .getPlanElements()
                    .values());
        elementList.sort(LspShipmentUtils.createShipmentPlanElementComparator());
        writeShipmentWithPlanElements(writer, shipment, elementList);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void writeShipmentWithPlanElements(
          BufferedWriter writer, LspShipment lspShipment, ArrayList<LspShipmentPlanElement> elementList)
      throws IOException {
    final String str1 = "Shipment: " + lspShipment;
    System.out.println(str1);
    writer.write(str1 + "\n");
    for (LspShipmentPlanElement element : elementList) {
      final String str2 =
          element.getLogisticChainElement().getId()
              + "\t\t"
              + element.getResourceId()
              + "\t\t"
              + element.getElementType()
              + "\t\t"
              + element.getStartTime()
              + "\t\t"
              + element.getEndTime();
      System.out.println(str2);
      writer.write(str2 + "\n");
    }
    System.out.println();
    writer.write("\n");
  }

  /**
   * Prints out the log of the shipment - this is not the shipment's plan Maybe the log will get
   * removed soon. kmt sep/oct'22
   *
   * @param outputDir path, defining the location for the results
   * @param lsp the LSP, for which the results should be written out.
   */
  public static void printResults_shipmentLog(String outputDir, LSP lsp) {
    System.out.println("Writing out shipmentLog for LSP");
    try (BufferedWriter writer =
        IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_shipmentLogs.tsv")) {
      final String str0 = "LSP: " + lsp.getId();
      System.out.println(str0);
      writer.write(str0 + "\n");
      for (LspShipment lspShipment : lsp.getLspShipments()) {
        ArrayList<LspShipmentPlanElement> elementList =
            new ArrayList<>(lspShipment.getShipmentLog().getPlanElements().values());
        elementList.sort(LspShipmentUtils.createShipmentPlanElementComparator());
        writeShipmentWithPlanElements(writer, lspShipment, elementList);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printScores(String outputDir, LSP lsp) {
    System.out.println("Writing out scores for LSP");
    try (BufferedWriter writer =
        IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_scores.tsv")) {
      final String str0 = "LSP: " + lsp.getId();
      System.out.println(str0);
      writer.write(str0 + "\n");
      final String str1 =
          "The LSP `` "
              + lsp.getId()
              + " ´´ has the following number of plans: "
              + lsp.getPlans().size()
              + "\n The scores are: ";
      System.out.println(str1);
      writer.write(str1 + "\n");
      for (LSPPlan plan : lsp.getPlans()) {
        final String str2 = "Score: " + plan.getScore().toString();
        System.out.println(str2);
        writer.write(str2 + "\n");
      }
      final String str3 = "The selected plan has the score: " + lsp.getSelectedPlan().getScore();
      System.out.println(str3);
      writer.write(str3 + "\n");
      System.out.println("###");
      writer.write("### \n");

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static CARRIER_TYPE getCarrierType(Carrier carrier) {
    if (carrier.getAttributes().getAttribute(CARRIER_TYPE_ATTR)
        instanceof CARRIER_TYPE carrierType) {
      return carrierType;
    } else {
      String result = (String) carrier.getAttributes().getAttribute(CARRIER_TYPE_ATTR);
      if (result == null) {
        log.warn("Requested attribute " + CARRIER_TYPE_ATTR + " does not exists. Will return {}", CARRIER_TYPE.undefined);
        return CARRIER_TYPE.undefined;
      } else {
        return CARRIER_TYPE.valueOf(result);
      }
    }
  }

  public static void setCarrierType(Carrier carrier, CARRIER_TYPE carrierType) {
    carrier.getAttributes().putAttribute(CARRIER_TYPE_ATTR, carrierType);
  }

  /**
   * Utils method to create a DistributionCarrierScheduler
   *  TODO: In the future, the scheduler should get the scenario via injection. This here is only a dirty workaround. KMT'Aug'24
   *
   * @param scenario the scenario
   */
  public static DistributionCarrierScheduler createDefaultDistributionCarrierScheduler(Scenario scenario) {
    return new DistributionCarrierScheduler(scenario);
  }

  /**
   * Utils method to create a CollectionCarrierScheduler
   * TODO: In the future, the scheduler should get the scenario via injection. This here is only a dirty workaround. KMT'Aug'24
   *
   * @param scenario the scenario
   */
  public static CollectionCarrierScheduler createDefaultCollectionCarrierScheduler(Scenario scenario) {
    return new CollectionCarrierScheduler(scenario);
  }

  /**
   * Utils method to create a MainRunCarrierScheduler
   * TODO: In the future, the scheduler should get the scenario via injection. This here is only a dirty workaround. KMT'Aug'24
   *
   * @param scenario the scenario
   */
  public static MainRunCarrierScheduler createDefaultMainRunCarrierScheduler(Scenario scenario) {
    return new MainRunCarrierScheduler(scenario);
  }

  public enum VehicleReturn {
    returnToFromLink,
    endAtToLink
  }

  public enum CARRIER_TYPE {
    collectionCarrier,
    mainRunCarrier,
    distributionCarrier,
    undefined
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static class DistributionCarrierResourceBuilder {

    final Id<LSPResource> id;
    final ArrayList<LogisticChainElement> clientElements;
    final Carrier carrier;
    Id<Link> locationLinkId;
    DistributionCarrierScheduler distributionHandler;

    private DistributionCarrierResourceBuilder(Carrier carrier) {
      this.id = Id.create(carrier.getId().toString(), LSPResource.class);
      setCarrierType(carrier, CARRIER_TYPE.distributionCarrier);
      this.carrier = carrier;
      this.clientElements = new ArrayList<>();
    }

    public static DistributionCarrierResourceBuilder newInstance(Carrier carrier) {
      return new DistributionCarrierResourceBuilder(carrier);
    }

    public DistributionCarrierResourceBuilder setLocationLinkId(Id<Link> locationLinkId) {
      this.locationLinkId = locationLinkId;
      return this;
    }

    public DistributionCarrierResourceBuilder setDistributionScheduler(
        DistributionCarrierScheduler distributionCarrierScheduler) {
      this.distributionHandler = distributionCarrierScheduler;
      return this;
    }

    public DistributionCarrierResource build() {
      return new DistributionCarrierResource(this);
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static class CollectionCarrierResourceBuilder {

    final Id<LSPResource> id;
    final ArrayList<LogisticChainElement> clientElements;
    final Carrier carrier;
    Id<Link> locationLinkId;
    CollectionCarrierScheduler collectionScheduler;

    private CollectionCarrierResourceBuilder(Carrier carrier) {
      this.id = Id.create(carrier.getId().toString(), LSPResource.class);
      setCarrierType(carrier, CARRIER_TYPE.collectionCarrier);
      this.carrier = carrier;
      this.clientElements = new ArrayList<>();
    }

    public static CollectionCarrierResourceBuilder newInstance(Carrier carrier) {
      return new CollectionCarrierResourceBuilder(carrier);
    }

    public CollectionCarrierResourceBuilder setLocationLinkId(Id<Link> locationLinkId) {
      this.locationLinkId = locationLinkId;
      return this;
    }

    public CollectionCarrierResourceBuilder setCollectionScheduler(
        CollectionCarrierScheduler collectionCarrierScheduler) {
      this.collectionScheduler = collectionCarrierScheduler;
      return this;
    }

    public CollectionCarrierResource build() {
      return new CollectionCarrierResource(this);
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static class MainRunCarrierResourceBuilder {

    private final Id<LSPResource> id;
    private final ArrayList<LogisticChainElement> clientElements;
    private Carrier carrier;
    private Id<Link> fromLinkId;
    private Id<Link> toLinkId;
    private MainRunCarrierScheduler mainRunScheduler;
    private VehicleReturn vehicleReturn;

    private MainRunCarrierResourceBuilder(Carrier carrier) {
      this.id = Id.create(carrier.getId().toString(), LSPResource.class);
      setCarrierType(carrier, CARRIER_TYPE.mainRunCarrier);
      this.carrier = carrier;
      this.clientElements = new ArrayList<>();
    }

    public static MainRunCarrierResourceBuilder newInstance(Carrier carrier) {
      return new MainRunCarrierResourceBuilder(carrier);
    }

    public MainRunCarrierResourceBuilder setMainRunCarrierScheduler(
        MainRunCarrierScheduler mainRunScheduler) {
      this.mainRunScheduler = mainRunScheduler;
      return this;
    }

    public MainRunCarrierResource build() {
      return new MainRunCarrierResource(this);
    }

    Id<LSPResource> getId() {
      return id;
    }

    Carrier getCarrier() {
      return carrier;
    }

    public MainRunCarrierResourceBuilder setCarrier(Carrier carrier) {
      setCarrierType(carrier, CARRIER_TYPE.mainRunCarrier);
      this.carrier = carrier;
      return this;
    }

    Id<Link> getFromLinkId() {
      return fromLinkId;
    }

    // --- Getter ---

    public MainRunCarrierResourceBuilder setFromLinkId(Id<Link> fromLinkId) {
      this.fromLinkId = fromLinkId;
      return this;
    }

    Id<Link> getToLinkId() {
      return toLinkId;
    }

    public MainRunCarrierResourceBuilder setToLinkId(Id<Link> toLinkId) {
      this.toLinkId = toLinkId;
      return this;
    }

    ArrayList<LogisticChainElement> getClientElements() {
      return clientElements;
    }

    MainRunCarrierScheduler getMainRunScheduler() {
      return mainRunScheduler;
    }

    VehicleReturn getVehicleReturn() {
      return vehicleReturn;
    }

    public MainRunCarrierResourceBuilder setVehicleReturn(VehicleReturn vehicleReturn) {
      this.vehicleReturn = vehicleReturn;
      return this;
    }
  }

  @SuppressWarnings("ClassEscapesDefinedScope")
  public static class TranshipmentHubSchedulerBuilder {
    private double capacityNeedLinear;
    private double capacityNeedFixed;

    private TranshipmentHubSchedulerBuilder() {}

    public static TranshipmentHubSchedulerBuilder newInstance() {
      return new TranshipmentHubSchedulerBuilder();
    }

    public TransshipmentHubScheduler build() {
      return new TransshipmentHubScheduler(this);
    }

    double getCapacityNeedLinear() {
      return capacityNeedLinear;
    }

    public TranshipmentHubSchedulerBuilder setCapacityNeedLinear(double capacityNeedLinear) {
      this.capacityNeedLinear = capacityNeedLinear;
      return this;
    }

    // --- Getters ---

    double getCapacityNeedFixed() {
      return capacityNeedFixed;
    }

    public TranshipmentHubSchedulerBuilder setCapacityNeedFixed(double capacityNeedFixed) {
      this.capacityNeedFixed = capacityNeedFixed;
      return this;
    }
  }

  public static final class TransshipmentHubBuilder {

    private final Id<LSPResource> id;
    private final Id<Link> locationLinkId;
    private final ArrayList<LogisticChainElement> clientElements;
    private final Scenario scenario;
    private TransshipmentHubScheduler transshipmentHubScheduler;

    private TransshipmentHubBuilder(
        Id<LSPResource> id, Id<Link> locationLinkId, Scenario scenario) {
      this.id = id;
      this.clientElements = new ArrayList<>();
      this.locationLinkId = locationLinkId;
      this.scenario = scenario;
    }

    public static TransshipmentHubBuilder newInstance(
        Id<LSPResource> id, Id<Link> locationLinkId, Scenario scenario) {
      return new TransshipmentHubBuilder(id, locationLinkId, scenario);
    }

    public TransshipmentHubResource build() {
      return new TransshipmentHubResource(this, scenario);
    }

    Id<LSPResource> getId() {
      return id;
    }

    // --- Getters ---

    Id<Link> getLocationLinkId() {
      return locationLinkId;
    }

    TransshipmentHubScheduler getTransshipmentHubScheduler() {
      return transshipmentHubScheduler;
    }

    public TransshipmentHubBuilder setTransshipmentHubScheduler(
        LSPResourceScheduler TranshipmentHubScheduler) {
      this.transshipmentHubScheduler = (TransshipmentHubScheduler) TranshipmentHubScheduler;
      return this;
    }

    ArrayList<LogisticChainElement> getClientElements() {
      return clientElements;
    }
  }
}
