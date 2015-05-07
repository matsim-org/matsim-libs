/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerDefaultCoreListenersModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.core.controler;

import org.matsim.core.controler.corelisteners.DumpDataAtEnd;
import org.matsim.core.controler.corelisteners.DumpDataAtEndImpl;
import org.matsim.core.controler.corelisteners.EventsHandling;
import org.matsim.core.controler.corelisteners.EventsHandlingImpl;
import org.matsim.core.controler.corelisteners.PlansDumping;
import org.matsim.core.controler.corelisteners.PlansDumpingImpl;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.core.controler.corelisteners.PlansReplanningImpl;
import org.matsim.core.controler.corelisteners.PlansScoring;
import org.matsim.core.controler.corelisteners.PlansScoringImpl;

/**
 * Defines the default core listeners.
 * In 99% of the use cases, this should be left as is.
 * Most of the process can be configured using more elemental elements:
 * StrategyManager, ScoringFunction, EventHandlers.
 *
 * @author thibautd
 */
public class ControlerDefaultCoreListenersModule extends AbstractModule {

	@Override
	public void install() {
		bind( PlansScoring.class ).to( PlansScoringImpl.class );
		bind( PlansReplanning.class ).to( PlansReplanningImpl.class );
		bind( PlansDumping.class ).to( PlansDumpingImpl.class );
		bind( EventsHandling.class ).to( EventsHandlingImpl.class );
		bind( DumpDataAtEnd.class ).to( DumpDataAtEndImpl.class );
	}
}

