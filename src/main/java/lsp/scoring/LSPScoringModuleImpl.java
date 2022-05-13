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

import lsp.LSP;
import lsp.LSPs;
import org.matsim.core.controler.events.ScoringEvent;

class LSPScoringModuleImpl implements LSPScoringModule{

	private final LSPs lsps;
	
	LSPScoringModuleImpl(LSPs lsps) {
		this.lsps = lsps;
	}
		
	@Override
	public void notifyScoring(ScoringEvent event) {
		scoreLSPs(event);
	}

	@Override
	public void scoreLSPs(ScoringEvent arg0) {
		for(LSP lsp : lsps.getLSPs().values()) {
			lsp.scoreSelectedPlan();
		}
	}
}
