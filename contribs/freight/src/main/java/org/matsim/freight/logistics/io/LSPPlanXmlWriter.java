/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.freight.logistics.io;

import static org.matsim.freight.logistics.LSPConstants.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.freight.logistics.*;
import org.matsim.freight.logistics.LSP;
import org.matsim.freight.logistics.resourceImplementations.TransshipmentHubResource;
import org.matsim.freight.logistics.shipment.LspShipment;
import org.matsim.freight.logistics.shipment.LspShipmentPlanElement;
import org.matsim.freight.logistics.shipment.LspShipmentUtils;

/**
 * Writes out resources, shipments and plans for each LSP in an XML-file including header for
 * validating against respective XSD and setting up according writer. Uses variables defined in
 * LSPConstants-class for the elements and attributes within the XML.
 *
 * @author nrichter (Niclas Richter)
 */
public class LSPPlanXmlWriter extends MatsimXmlWriter {

  private static final Logger logger = LogManager.getLogger(LSPPlanXmlWriter.class);

  private final Collection<LSP> lsPs;

  public LSPPlanXmlWriter(LSPs lsPs) {
    super();
    this.lsPs = lsPs.getLSPs().values();
  }

  public void write(String filename) {
    logger.info(Gbl.aboutToWrite("lsps", filename));
    try {
      this.openFile(filename);
      this.writeXmlHead();
      this.writeRootElement();
      for (LSP lsp : lsPs) {
        this.startLSP(lsp);
        this.writeResources(lsp);
        this.writeShipments(lsp);
        this.writePlans(lsp, this.writer);
        this.writeEndTag(LSP);
      }
      this.writeEndTag(LSPConstants.LSPS);
      this.close();
      logger.info("done");
    } catch (IOException e) {
      e.printStackTrace();
      logger.error(e);
      System.exit(1);
    }
  }

  private void writeRootElement() throws IOException {
    List<Tuple<String, String>> atts = new ArrayList<>();
    atts.add(createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
    atts.add(createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
    atts.add(
        createTuple(
            "xsi:schemaLocation",
            MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "lspsDefinitions_v1.xsd"));
    this.writeStartTag(LSPConstants.LSPS, atts);
    this.writer.write(NL);
  }

  private void startLSP(LSP lsp) {
    this.writeStartTag(LSP, List.of(createTuple(ID, lsp.getId().toString())));
  }

  private void writeResources(LSP lsp) {
    if (lsp.getResources().isEmpty()) return;
    this.writeStartTag(RESOURCES, null);
    for (LSPResource resource : lsp.getResources()) {
      if (resource instanceof TransshipmentHubResource hub) {
        List<Tuple<String, String>> tupleList = new ArrayList<>();
        tupleList.add(new Tuple<>(ID, hub.getId().toString()));
        tupleList.add(new Tuple<>(LOCATION, hub.getStartLinkId().toString()));
        if (hub.getAttributes().getAttribute(FIXED_COST) != null) {
          tupleList.add(
              new Tuple<>(FIXED_COST, hub.getAttributes().getAttribute(FIXED_COST).toString()));
        }
        this.writeStartTag(HUB, tupleList);
        this.writeStartTag(
            SCHEDULER,
            List.of(
                createTuple(CAPACITY_NEED_FIXED, hub.getCapacityNeedFixed()),
                createTuple(CAPACITY_NEED_LINEAR, hub.getCapacityNeedLinear())),
            true);
        this.writeEndTag(HUB);
      }
      if (resource instanceof LSPCarrierResource carrierResource) {
        this.writeStartTag(
            CARRIER, List.of(createTuple(ID, carrierResource.getId().toString())), true);
      }
    }
    this.writeEndTag(RESOURCES);
  }

  private void writeShipments(LSP lsp) {
    if (lsp.getLspShipments().isEmpty()) return;
    this.writeStartTag(SHIPMENTS, null);
    for (LspShipment lspShipment : lsp.getLspShipments()) {
      this.writeStartTag(
          SHIPMENT,
          List.of(
              createTuple(ID, lspShipment.getId().toString()),
              createTuple(FROM, lspShipment.getFrom().toString()),
              createTuple(TO, lspShipment.getTo().toString()),
              createTuple(SIZE, lspShipment.getSize()),
              createTuple(START_PICKUP, lspShipment.getPickupTimeWindow().getStart()),
              createTuple(END_PICKUP, lspShipment.getPickupTimeWindow().getEnd()),
              createTuple(START_DELIVERY, lspShipment.getDeliveryTimeWindow().getStart()),
              createTuple(END_DELIVERY, lspShipment.getDeliveryTimeWindow().getEnd()),
              createTuple(PICKUP_SERVICE_TIME, lspShipment.getPickupServiceTime()),
              createTuple(DELIVERY_SERVICE_TIME, lspShipment.getDeliveryServiceTime())),
          true);
    }
    this.writeEndTag(SHIPMENTS);
  }

  private void writePlans(LSP lsp, BufferedWriter writer) {
    if (lsp.getPlans().isEmpty()) return;
    this.writeStartTag(LSP_PLANS, null);

    for (LSPPlan plan : lsp.getPlans()) {
      if (plan.getScore() != null && lsp.getSelectedPlan() != null) {
        if (plan == lsp.getSelectedPlan()) {
          this.writeStartTag(
              LSP_PLAN,
              List.of(createTuple(SCORE, plan.getScore()), createTuple(SELECTED, "true")));
        } else {
          this.writeStartTag(
              LSP_PLAN,
              List.of(createTuple(SCORE, plan.getScore()), createTuple(SELECTED, "false")));
        }
      } else {
        this.writeStartTag(LSP_PLAN, List.of(createTuple(SELECTED, "false")));
      }

      this.writeStartTag(LOGISTIC_CHAINS, null);
      for (LogisticChain chain : plan.getLogisticChains()) {
        writeStartTag(LOGISTIC_CHAIN, List.of(createTuple(ID, chain.getId().toString())));
        for (LogisticChainElement chainElement : chain.getLogisticChainElements()) {
          this.writeStartTag(
              LOGISTIC_CHAIN_ELEMENT,
              List.of(
                  createTuple(ID, chainElement.getId().toString()),
                  createTuple(RESOURCE_ID, chainElement.getResource().getId().toString())),
              true);
        }
        writeEndTag(LOGISTIC_CHAIN);
      }
      writeEndTag(LOGISTIC_CHAINS);

      writeStartTag(SHIPMENT_PLANS, null);
      for (LogisticChain chain : plan.getLogisticChains()) {
        for (Id<LspShipment> shipmentId : chain.getLspShipmentIds()) {
          if (chain.getLspShipmentIds().contains(shipmentId)) {
            this.writeStartTag(
                SHIPMENT_PLAN,
                List.of(
                    createTuple(SHIPMENT_ID, shipmentId.toString()),
                    createTuple(CHAIN_ID, chain.getId().toString())));
          }
          LspShipment lspShipment = LSPUtils.findLspShipment(lsp, shipmentId);
          assert lspShipment != null;
          final Map<Id<LspShipmentPlanElement>, LspShipmentPlanElement> planElements =
              LspShipmentUtils.getOrCreateShipmentPlan(plan, lspShipment.getId()).getPlanElements();
          for (Id<LspShipmentPlanElement> elementId : planElements.keySet()) {
            LspShipmentPlanElement element = planElements.get(elementId);
            this.writeStartTag(
                ELEMENT,
                List.of(
                    createTuple(ID, elementId.toString()),
                    createTuple(TYPE, element.getElementType()),
                    createTuple(START_TIME, element.getStartTime()),
                    createTuple(END_TIME, element.getEndTime()),
                    createTuple(RESOURCE_ID, element.getResourceId().toString())),
                true);
          }
          writeEndTag(SHIPMENT_PLAN);
        }
      }
      writeEndTag(SHIPMENT_PLANS);
      writeEndTag(LSP_PLAN);
    }
    this.writeEndTag(LSP_PLANS);
  }
}
