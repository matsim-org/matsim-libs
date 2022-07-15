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

package lsp.replanning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.replanning.GenericStrategyManager;

import lsp.LSP;
import lsp.LSPPlan;


class LSPReplannerImpl implements LSPReplanner{

	private LSP lsp;
	private final GenericStrategyManager<LSPPlan, LSP> strategyManager;
	
	LSPReplannerImpl( GenericStrategyManager<LSPPlan, LSP> strategyManager ) {
		this.strategyManager = strategyManager;
	}
	
//	public LSPReplannerImpl() {
//	}
	
	@Override
	public void setEmbeddingContainer( LSP lsp ) {
		this.lsp = lsp;
	}

	@Override
	public void replan(ReplanningEvent event) {
		if(strategyManager != null) {
			strategyManager.run( Collections.singletonList( lsp ), null, event.getIteration(), event.getReplanningContext() );
		}
		lsp.getSelectedPlan().getAssigner().setLSP(lsp);//TODO: Feels wierd, but getting NullPointer because of missing lsp inside the assigner
		//TODO: Do we need to do it for each plan, if it gets selected???
	}

//	@Override
//	public GenericStrategyManager<LSPPlan, LSP> getStrategyManager() {
//		return strategyManager;
//	}

//	@Override
//	public void setStrategyManager(GenericStrategyManager<LSPPlan, LSP> manager) {
//		this.strategyManager = manager;
//	}

}
