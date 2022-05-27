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
import java.util.Map;


/* package-private */ class LogisticsSolutionElementImpl implements LogisticsSolutionElement {

	private final Attributes attributes = new Attributes();
	private final Id<LogisticsSolutionElement>id;
	//die beiden nicht im Builder. Die können erst in der Solution als ganzes gesetzt werden
	private LogisticsSolutionElement previousElement;
	private LogisticsSolutionElement nextElement;
	private final LSPResource resource;
	private final WaitingShipments incomingShipments;
	private final WaitingShipments outgoingShipments;
	private LogisticsSolution solution;
	private final Collection<LSPInfo> infos;
	private final Collection<LSPSimulationTracker> trackers;
	private final Collection<EventHandler> handlers;
//	private EventsManager eventsManager;

	LogisticsSolutionElementImpl( LSPUtils.LogisticsSolutionElementBuilder builder ){
		this.id = builder.id;
		this.resource = builder.resource;
		this.incomingShipments = builder.incomingShipments;
		this.outgoingShipments = builder.outgoingShipments;
		resource.getClientElements().add(this);
		this.handlers = new ArrayList<>();
		this.infos = new ArrayList<>();
		this.trackers = new ArrayList<>();
	}
	
	@Override
	public Id<LogisticsSolutionElement> getId() {
		return id;
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

//	@Override
//	//KMT, KN: Never Used? -- Wäre wenn eher eh was für eine Utils-klasse. (ggf. mit anderem Namen). Frage: gedoppelt mit Scheduler?
//	public void schedulingOfResourceCompleted() {
//		for( ShipmentWithTime tuple : outgoingShipments.getSortedShipments()){
//			nextElement.getIncomingShipments().addShipment(tuple.getTime(), tuple.getShipment());
//		}
//	}

	@Override
	public void setEmbeddingContainer( LogisticsSolution solution ) {
		this.solution = solution;
	}

	@Override
	public LogisticsSolution getEmbeddingContainer() {
		return solution;
	}

	@Override
	public LogisticsSolutionElement getPreviousElement() {
		return previousElement;
	}

	@Override
	public LogisticsSolutionElement getNextElement() {
		return nextElement;
	}

	@Override
	public void addSimulationTracker( LSPSimulationTracker tracker ) {
		trackers.add(tracker);

		// can't say if this hierarchical design is useful or confusing. kai, may'22
		// yy should maybe check for overwriting?  However, did also not check in original design. kai, may'22
		for( Map.Entry<String, Object> entry : tracker.getAttributes().getAsMap().entrySet() ){
			attributes.putAttribute( entry.getKey(), entry.getValue() );
		}

		handlers.addAll(tracker.getEventHandlers());
	}

	public Collection<EventHandler> getEventHandlers(){
		return handlers;
	}

	@Override
	public Collection<LSPSimulationTracker> getSimulationTrackers() {
		return trackers;
	}
	@Override public Attributes getAttributes(){
		return attributes;
	}

//	@Override
//	public void setEventsManager(EventsManager eventsManager) {
//		this.eventsManager = eventsManager;
//	}
	

}
