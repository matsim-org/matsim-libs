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
import lsp.LSPInfo;
import lsp.scoring.LSPScorer;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.Map;

/*package-private*/ class TipScorer implements LSPScorer {

	private final TipSimulationTracker tracker;

	/*package-private*/ TipScorer(LSP lsp, TipSimulationTracker tracker) {
		this.tracker = tracker;
	}
	
	@Override
	public double scoreCurrentPlan(LSP lsp) {
		double score = 0;
		for(LSPInfo info : tracker.getInfos()) {
			if(info instanceof TipInfo) {
				Attributes function = info.getAttributes();
					for(  Map.Entry value : function.getAsMap().entrySet() ) {
						if(value.getKey().equals("TIP IN EUR") && value.getValue() instanceof Double) {
							double trinkgeldValue = (Double) value.getValue();
							score += trinkgeldValue;
						}
					}
			}
		}
		return score;
	}

	@Override
	public void setLSP(LSP lsp) {
		// TODO Auto-generated method stub
		
	}

		
}
