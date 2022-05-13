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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

import lsp.shipment.LSPShipment;
import lsp.controler.LSPSimulationTracker;


/**
 * A LogisticsSolution can be seen as a representative of a
 * transport chain. It consists of several chain links that implement the interface
 * {@link LogisticsSolutionElement}. The latter is more a logical than a physical entity.
 * Physical entities, in turn, are housed inside classes that implement the interface
 * {@link LSPResource}. This introduction of an intermediate layer allows physical Resources
 * to be used by several {@link LogisticsSolution}s and thus transport chains.
 */
public interface LogisticsSolution {

	Id<LogisticsSolution> getId();
	
	void setLSP(LSP lsp);
	
	LSP getLSP();
	
	Collection<LogisticsSolutionElement> getSolutionElements();
	
	Collection<LSPShipment> getShipments();
	
	void assignShipment(LSPShipment shipment);
	
	Collection<LSPInfo> getInfos();
	
    Collection <EventHandler> getEventHandlers();
        
    void addSimulationTracker(LSPSimulationTracker tracker);
    
    Collection<LSPSimulationTracker> getSimulationTrackers();
    
    void setEventsManager(EventsManager eventsManager);
}
