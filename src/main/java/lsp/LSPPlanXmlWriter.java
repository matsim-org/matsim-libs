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

package lsp;

import lsp.shipment.LSPShipment;
import lsp.shipment.ShipmentPlanElement;
import lsp.usecase.TransshipmentHub;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.attributable.AttributesXmlWriterDelegate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static lsp.LSPConstants.*;


/**
 * Writes out resources, shipments and plans for each LSP in an XML-file including header for validating against
 * respective XSD and setting up according writer.
 * Uses variables defined in LSPConstants-class for the elements and attributes within the XML.
 * @author nrichter (Niclas Richter)
 */
public class LSPPlanXmlWriter extends MatsimXmlWriter {

	private static final  Logger logger = LogManager.getLogger(LSPPlanXmlWriter.class);

	private final Collection<LSP> lsPs;
	private final AttributesXmlWriterDelegate attributesWriter = new AttributesXmlWriterDelegate();

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
			this.startLSPs(this.writer);
			for (LSP lsp : lsPs) {
				this.startLSP(lsp, this.writer);
				this.writeResources(lsp, this.writer);
				this.writeShipments(lsp, this.writer);
				this.writePlans(lsp, this.writer);
				this.endLSP(this.writer);
			}
			this.endLSPs(this.writer);
			this.writeEndTag(LSPConstants.LSPS_DEFINITIONS);
			this.close();
			logger.info("done");
		} catch ( IOException e) {
			e.printStackTrace();
			logger.error(e);
			System.exit(1);
		}
	}

	private void writeRootElement() throws UncheckedIOException, IOException {
		List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
		atts.add(createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "lspsDefinitions_v1.xsd"));
		this.writeStartTag(LSPConstants.LSPS_DEFINITIONS, atts);
		this.writer.write(NL);
	}

	private void startLSPs(BufferedWriter writer) throws IOException {
		writer.write("\n\t<" + LSPS + ">\n");
	}

	private void startLSP(LSP lsp, BufferedWriter writer)
			throws IOException {
		writer.write("\t\t<" + LSP + " " + ID + "=\"" + lsp.getId() + "\">\n");
	}

	private void writeResources(LSP lsp, BufferedWriter writer )throws IOException {
		if (lsp.getResources().isEmpty()) return;
		writer.write("\t\t\t<" + RESOURCES + ">\n");
		for (LSPResource resource : lsp.getResources()) {
			if (resource instanceof TransshipmentHub hub) {
				writer.write("\t\t\t\t<" + HUB + " ");
				writer.write(ID + "=\"" + hub.getId() + "\" ");
				writer.write(LOCATION + "=\"" + hub.getStartLinkId() + "\" ");
				writer.write(FIXED_COST + "=\"" + hub.getAttributes().getAttribute(FIXED_COST) + "\">\n");
				writer.write("\t\t\t\t\t<" + SCHEDULER + " ");
				writer.write(CAPACITY_NEED_FIXED + "=\"" + hub.getCapacityNeedFixed() +  "\" ");
				writer.write(CAPACITY_NEED_LINEAR + "=\"" + hub.getCapacityNeedLinear() +  "\"/>\n");
				writer.write("\t\t\t\t</" + HUB + ">\n");
			}
			if (resource instanceof LSPCarrierResource carrierResource) {
				writer.write("\t\t\t\t<" + CARRIER + " ");
				writer.write(ID + "=\"" + carrierResource.getId() + "\"/>\n");
			}
		}
		writer.write("\t\t\t</" + RESOURCES + ">\n\n");
	}

	private void writeShipments(LSP lsp, BufferedWriter writer )throws IOException {
		if(lsp.getShipments().isEmpty()) return;
		writer.write("\t\t\t<" + SHIPMENTS + ">\n");
		for (LSPShipment shipment: lsp.getShipments()) {
			writer.write("\t\t\t\t<" + SHIPMENT + " ");
			writer.write(ID + "=\"" + shipment.getId() + "\" ");
			writer.write(FROM + "=\"" + shipment.getFrom() + "\" ");
			writer.write(TO + "=\"" + shipment.getTo() + "\" ");
			writer.write(SIZE + "=\"" + shipment.getSize() + "\" ");
			writer.write(START_PICKUP + "=\"" + shipment.getPickupTimeWindow().getStart() + "\" ");
			writer.write(END_PICKUP + "=\"" + shipment.getPickupTimeWindow().getEnd() + "\" ");
			writer.write(START_DELIVERY + "=\"" + shipment.getDeliveryTimeWindow().getStart() + "\" ");
			writer.write(END_DELIVERY + "=\"" + shipment.getDeliveryTimeWindow().getEnd() + "\" ");
			writer.write(PICKUP_SERVICE_TIME + "=\"" + shipment.getPickupServiceTime() + "\" ");
			writer.write(DELIVERY_SERVICE_TIME + "=\"" + shipment.getDeliveryServiceTime());
			if (shipment.getAttributes().isEmpty()) {
				writer.write("\"/>\n");
			} else {
				writer.write("\">\n");
				this.attributesWriter.writeAttributes("\t\t\t\t\t", writer, shipment.getAttributes());
				writer.write("\t\t\t\t</" + SHIPMENT + ">\n");
			}
		}
		writer.write("\t\t\t</" + SHIPMENTS + ">\n\n");
	}

	private void writePlans(LSP lsp, BufferedWriter writer) throws IOException {
		if (lsp.getPlans().isEmpty()) return;
		writer.write("\t\t\t<" + LSP_PLANS + ">\n");

		for (LSPPlan plan : lsp.getPlans()) {
			writer.write("\t\t\t\t<" + PLAN);
			if (plan.getScore() != null) {
				writer.write(" " + SCORE + "=\"" + plan.getScore() + "\"");
			}
			if (lsp.getSelectedPlan() != null) {
				if (plan == lsp.getSelectedPlan()) {
					writer.write(" selected=\"true\"");
				} else {
					writer.write(" selected=\"false\"");
				}
			} else {
				writer.write(" selected=\"false\"");
			}
			writer.write(">\n");

			writer.write("\t\t\t\t\t<" + LOGISTIC_CHAINS + ">\n");
			for (LogisticChain logisticChain : plan.getLogisticChains()) {
				writer.write("\t\t\t\t\t\t<" + LOGISTIC_CHAIN + " " + ID + "=\"" + logisticChain.getId() + "\">\n");
				for (LogisticChainElement chainElement : logisticChain.getLogisticChainElements()) {
					writer.write("\t\t\t\t\t\t\t<" + LOGISTIC_CHAIN_ELEMENT + " " + ID + "=\"" + chainElement.getId() + "\" ");
					writer.write(RESOURCE_ID + "=\"" + chainElement.getResource().getId() + "\"/>\n");
				}
				writer.write("\t\t\t\t\t\t</" + LOGISTIC_CHAIN + ">\n");
				writer.write("\t\t\t\t\t</" + LOGISTIC_CHAINS + ">\n");
				writer.write("\t\t\t\t\t<" + SHIPMENT_PLANS + ">\n");
				for (LSPShipment shipment : logisticChain.getShipments()) {
					writer.write("\t\t\t\t\t\t<" + SHIPMENT_PLAN + " " +  SHIPMENT_ID + "=\"" + shipment.getId() + "\">\n");
					for (var elementId : shipment.getShipmentPlan().getPlanElements().keySet()) {
						writer.write("\t\t\t\t\t\t\t<" + ELEMENT + " " + ID + "=\"" + elementId.toString() + "\" ");
						ShipmentPlanElement element = shipment.getShipmentPlan().getPlanElements().get(elementId);
						writer.write(TYPE + "=\"" + element.getElementType() + "\" ");
						writer.write(START_TIME + "=\"" + element.getStartTime() + "\" ");
						writer.write(END_TIME + "=\"" + element.getEndTime() + "\" ");
						writer.write(RESOURCE_ID + "=\"" + element.getResourceId() + "\"/>\n");
					}
					writer.write("\t\t\t\t\t\t</" + SHIPMENT_PLAN + ">\n");
				}
				writer.write("\t\t\t\t\t</" + SHIPMENT_PLANS + ">\n");
			}
			writer.write("\t\t\t\t</"+ PLAN + ">\n");
		}
		writer.write("\t\t\t</" + LSP_PLANS + ">\n\n");
	}

	private void endLSP(BufferedWriter writer) throws IOException {
		writer.write("\t\t</" + LSP + ">\n");
	}

	private void endLSPs(BufferedWriter writer) throws IOException {
		writer.write("\t</" + LSPS + ">\n");
	}

}