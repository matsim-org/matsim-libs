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

package lsp;

import lsp.shipment.LSPShipment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

/* package-private */ class LogisticsSolutionImpl extends LSPDataObject<LogisticsSolution> implements LogisticsSolution {
	private static final Logger log = LogManager.getLogger(LogisticsSolutionImpl.class);

	private final Collection<LogisticsSolutionElement> solutionElements;
	private final Collection<LSPShipment> shipments;
	private LSP lsp;

	LogisticsSolutionImpl(LSPUtils.LogisticsSolutionBuilder builder) {
		super(builder.id);
		this.solutionElements = builder.elements;
		for (LogisticsSolutionElement element : this.solutionElements) {
			element.setEmbeddingContainer(this);
		}
		this.shipments = new ArrayList<>();
	}

	@Override
	public void setLSP( LSP lsp ) {
		this.lsp = lsp;
	}

	@Override
	public LSP getLSP() {
		return lsp;
	}

	@Override
	public Collection<LogisticsSolutionElement> getSolutionElements() {
		return solutionElements;
	}

	@Override
	public Collection<LSPShipment> getShipments() {
		return shipments;
	}

	@Override
	public void assignShipment(LSPShipment shipment) {
		shipments.add(shipment);
	}

	@Override public String toString() {
		StringBuilder strb = new StringBuilder();
		strb.append("LogisticsSolutionImpl{")
				.append("[No of SolutionsElements=").append(solutionElements.size()).append("] \n");
		if (!solutionElements.isEmpty()){
			strb.append("{SolutionElements=");
			for  (LogisticsSolutionElement solutionElement : solutionElements) {
				strb.append("\n [" + solutionElement.toString() + "]");
			}
			strb.append("}");
		}
		strb.append("[No of Shipments=").append(shipments.size()).append("] \n");
		if (!shipments.isEmpty()){
			strb.append("{ShipmentIds=");
			for (LSPShipment shipment : shipments) {
				strb.append("[" + shipment.getId().toString() + "]");
			}
			strb.append("}");
		}
		strb.append('}');
		return strb.toString();
	}

}
