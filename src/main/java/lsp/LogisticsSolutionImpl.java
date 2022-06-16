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
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import java.util.ArrayList;
import java.util.Collection;

/* package-private */ class LogisticsSolutionImpl extends LSPDataObject<LogisticsSolution> implements LogisticsSolution {
	private static final Logger log = Logger.getLogger( LogisticsSolutionImpl.class );

	private final Id<LogisticsSolution> id;
	private LSP lsp;
	private final Collection<LogisticsSolutionElement> solutionElements;
	private final Collection<LSPShipment> shipments;

	LogisticsSolutionImpl( LSPUtils.LogisticsSolutionBuilder builder ){
		this.id = builder.id;
		this.solutionElements = builder.elements;
		for(LogisticsSolutionElement element : this.solutionElements) {
			element.setEmbeddingContainer(this );
		}
		this.shipments = new ArrayList<>();
	}
	
	
	@Override
	public Id<LogisticsSolution> getId() {
		return id;
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
		return  solutionElements;
	}

	@Override
	public Collection<LSPShipment> getShipments() {
		return shipments;
	}

	@Override
	public void assignShipment(LSPShipment shipment) {
		shipments.add(shipment);	
	}
	
}
