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

package lsp.scoring;

import com.google.inject.Inject;
import lsp.LSP;
import lsp.LSPUtils;
import lsp.LSPs;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.ScoringEvent;

public class LSPScoringModuleImpl implements LSPScoringModule{

	private final Scenario scenario;
	@Inject LSPScoringModuleImpl( Scenario scenario ) {
		this.scenario = scenario;
	}
	
//	LSPScoringModuleImpl(LSPs lsps) {
//		this.lsps = lsps;
//	}
		
	@Override
	public void notifyScoring(ScoringEvent event) {
		scoreLSPs(event);
	}

	@Override
	public void scoreLSPs(ScoringEvent arg0) {
		LSPs lsps = LSPUtils.getLSPs( scenario );
		for(LSP lsp : lsps.getLSPs().values()) {
			lsp.scoreSelectedPlan();
		}
	}
}
