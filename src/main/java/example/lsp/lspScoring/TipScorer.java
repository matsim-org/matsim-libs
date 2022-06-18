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
import org.apache.log4j.Logger;
import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceEndEventHandler;

import java.util.Random;

/*package-private*/ class TipScorer implements LSPScorer, LSPSimulationTracker<LSP>, LSPServiceEndEventHandler
{
	private static final Logger log = Logger.getLogger( TipScorer.class );

	private final Random tipRandom;
	private double tipSum;

	/*package-private*/ TipScorer() {
		tipRandom = new Random(1);
		tipSum = 0;
	}
	
	@Override public double computeScoreForCurrentPlan() {
		return tipSum;
	}

	@Override public void setEmbeddingContainer( LSP pointer ){
		// backpointer not needed here, therefor not memorizing it.  kai, jun'22
	}

	@Override public void handleEvent( LSPServiceEndEvent event ) {
		double tip = tipRandom.nextDouble() * 5;
		log.warn("tipSum=" + tipSum + "; tip=" + tip);
		tipSum += tip;
	}

	@Override public void reset( int iteration ){
		tipSum = 0.;
	}

}
