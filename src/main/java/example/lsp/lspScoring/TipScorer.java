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
import org.checkerframework.checker.units.qual.A;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/*package-private*/ class TipScorer implements LSPScorer, LSPSimulationTracker
{
	private final Attributes attributes = new Attributes();

	private final LSP lsp;
	/*package-private*/ TipScorer( LSP lsp ) {
		this.lsp = lsp;
	}
	
	@Override
	public double scoreCurrentPlan(LSP lsp) {
//		double score = 0;
//		for(LSPInfo info : tracker.getAttributes()) {
//			if(info instanceof TipInfo) {
//				Attributes function = info.getAttributes();
//				for(  Map.Entry<String,Object> entry : function.getAsMap().entrySet() ) {
//					if(entry.getKey().equals("TIP IN EUR") && entry.getValue() instanceof Double) {
//						double trinkgeldValue = (Double) entry.getValue();
//						score += trinkgeldValue;
//					}
//				}
//			}
//		}

//		Double tip = (Double) tracker.getAttributes().getAttribute( "TIP IN EUR" );
//		if ( tip != null ){
//			score += tip;
//		}

		return handler.getTip();
	}

	@Override public LSP getEmbeddingContainer(){
		throw new RuntimeException( "not implemented" );
	}

	private final TipEventHandler handler = new TipEventHandler();
//	private final LSPInfo info = new TipInfo();

	@Override
	public Collection<EventHandler> getEventHandlers() {
		return Collections.singletonList( handler );
	}

	@Override
	public void notifyAfterMobsim( AfterMobsimEvent event ) {
		double tip = handler.getTip();
//		LSPInfoFunctionValueImpl<Object> value = LSPInfoFunctionUtils.createInfoFunctionValue( "TIP IN EUR" );
//		value.setValue(tip);
//		info.getAttributes().getAttributes().add(value );
//		this.getAttributes().putAttribute( "TIP IN EUR", tip );
	}


	@Override public void reset(){
	}
	@Override public Attributes getAttributes(){
		return attributes;
	}
}
