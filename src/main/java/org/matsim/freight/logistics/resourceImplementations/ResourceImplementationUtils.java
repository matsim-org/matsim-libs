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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierVehicle;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.LSPPlan;
import org.matsim.freight.logistics.LSPResource;
import org.matsim.freight.logistics.shipment.LSPShipment;
import org.matsim.freight.logistics.shipment.ShipmentPlanElement;
import org.matsim.freight.logistics.shipment.ShipmentUtils;
import org.matsim.vehicles.VehicleType;

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
      for (LSPShipment shipment : lsp.getShipments()) {
        final String str1 = "Shipment: " + shipment;
        System.out.println(str1);
        writer.write(str1 + "\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void printResults_shipmentPlan(String outputDir, LSP lsp) {
    System.out.println("Writing out shipmentPlan for LSP");
    LSPPlan lspPlan = lsp.getSelectedPlan();
    try (BufferedWriter writer =
        IOUtils.getBufferedWriter(outputDir + "/" + lsp.getId().toString() + "_schedules.tsv")) {
      final String str0 = "LSP: " + lsp.getId();
      System.out.println(str0);
      writer.write(str0 + "\n");
      for (LSPShipment shipment : lsp.getShipments()) {
        ArrayList<ShipmentPlanElement> elementList =
            new ArrayList<>(
                ShipmentUtils.getOrCreateShipmentPlan(lsp.getSelectedPlan(), shipment.getId())
                    .getPlanElements()
                    .values());
        elementList.sort(ShipmentUtils.createShipmentPlanElementComparator());
        writeShipmentWithPlanElements(writer, shipment, elementList);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void writeShipmentWithPlanElements(
      BufferedWriter writer, LSPShipment shipment, ArrayList<ShipmentPlanElement> elementList)
      throws IOException {
    final String str1 = "Shipment: " + shipment;
    System.out.println(str1);
    writer.write(str1 + "\n");
    for (ShipmentPlanElement element : elementList) {
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
      for (LSPShipment shipment : lsp.getShipments()) {
        ArrayList<ShipmentPlanElement> elementList =
            new ArrayList<>(shipment.getShipmentLog().getPlanElements().values());
        elementList.sort(ShipmentUtils.createShipmentPlanElementComparator());
        writeShipmentWithPlanElements(writer, shipment, elementList);
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
        log.warn(
            "Requested attribute "
                + CARRIER_TYPE_ATTR
                + " does not exists. Will return "
                + CARRIER_TYPE.undefined);
        return CARRIER_TYPE.undefined;
      } else {
        return CARRIER_TYPE.valueOf(result);
      }
    }
  }

  public static void setCarrierType(Carrier carrier, CARRIER_TYPE carrierType) {
    carrier.getAttributes().putAttribute(CARRIER_TYPE_ATTR, carrierType);
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
}
