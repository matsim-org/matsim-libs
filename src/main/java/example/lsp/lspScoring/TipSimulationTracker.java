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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.contrib.freight.events.LSPServiceEndEvent;
import org.matsim.contrib.freight.events.eventhandler.LSPServiceEndEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;

import lsp.controler.LSPSimulationTracker;
import org.matsim.utils.objectattributes.attributable.Attributes;

/*package-private*/ class TipSimulationTracker implements LSPSimulationTracker{

	private final Attributes attributes = new Attributes();

	private final TipEventHandler handler = new TipEventHandler();
//	private final LSPInfo info = new TipInfo();

	@Override
	public Collection<EventHandler> getEventHandlers() {
		List<EventHandler> handlers = new ArrayList<>();
		handlers.add(handler);
		return handlers;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		double tip = handler.getTip();
//		LSPInfoFunctionValueImpl<Object> value = LSPInfoFunctionUtils.createInfoFunctionValue( "TIP IN EUR" );
//		value.setValue(tip);
//		info.getAttributes().getAttributes().add(value );
		this.getAttributes().putAttribute( "TIP IN EUR", tip );
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

	@Override public Attributes getAttributes(){
		return attributes;
	}
}
