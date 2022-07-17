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

import lsp.controler.LSPSimulationTracker;
import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


/* package-private */ class LogisticsSolutionElementImpl extends LSPDataObject<LogisticsSolutionElement> implements LogisticsSolutionElement {

	private final LSPResource resource;
	private final WaitingShipments incomingShipments;
	private final WaitingShipments outgoingShipments;
	//die beiden nicht im Builder. Die können erst in der Solution als ganzes gesetzt werden
	private LogisticsSolutionElement previousElement;
	private LogisticsSolutionElement nextElement;
	private LogisticsSolution solution;

	LogisticsSolutionElementImpl(LSPUtils.LogisticsSolutionElementBuilder builder) {
		super(builder.id);
		this.resource = builder.resource;
		this.incomingShipments = builder.incomingShipments;
		this.outgoingShipments = builder.outgoingShipments;
		resource.getClientElements().add(this);
	}

	@Override
	public void connectWithNextElement(LogisticsSolutionElement element) {
		this.nextElement = element;
		((LogisticsSolutionElementImpl) element).previousElement = this;
	}

	@Override
	public LSPResource getResource() {
		return resource;
	}

	@Override
	public WaitingShipments getIncomingShipments() {
		return incomingShipments;
	}

	@Override
	public WaitingShipments getOutgoingShipments() {
		return outgoingShipments;
	}


	@Override
	public void setEmbeddingContainer(LogisticsSolution solution) {
		this.solution = solution;
	}

	@Override
	public LogisticsSolutionElement getPreviousElement() {
		return previousElement;
	}

	@Override
	public LogisticsSolutionElement getNextElement() {
		return nextElement;
	}

}
