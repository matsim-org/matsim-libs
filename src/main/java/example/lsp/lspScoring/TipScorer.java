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

package example.lsp.lspScoring;

import lsp.LSP;
import lsp.controler.LSPSimulationTracker;
import lsp.scoring.LSPScorer;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceEndEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;

/*package-private*/ class TipScorer implements LSPScorer, LSPSimulationTracker<LSP>, LSPServiceEndEventHandler
{

	private final Random tipRandom;
	private double tipSum;
	private final Collection<EventHandler> eventHandlers = new ArrayList<>();

	/*package-private*/ TipScorer() {
		tipRandom = new Random(1);
		tipSum = 0;
	}
	
	@Override
	public double scoreCurrentPlan(LSP lsp) {
		return tipSum;
	}

	@Override public LSP getEmbeddingContainer(){
		throw new RuntimeException( "not implemented" );
	}

	@Override public Collection<EventHandler> getEventHandlers() {
		return this.eventHandlers;
	}

	@Override
	public void handleEvent( LSPServiceEndEvent event ) {
		double tip = tipRandom.nextDouble() * 5;
		tipSum += tip;
	}

	@Override public void notifyAfterMobsim( AfterMobsimEvent event ) {
	}


	@Override public void reset(){
		tipSum = 0.;
	}
	@Override public Attributes getAttributes(){
		return null;
	}


}
