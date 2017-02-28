/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.ikaddoura.analysis.detailedPersonTripAnalysis;

import org.matsim.core.controler.AbstractModule;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.BasicPersonTripAnalysisHandler;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.handler.NoiseAnalysisHandler;

public class PersonTripAnalysisModule extends AbstractModule {
	
	@Override
	public void install() {
		
		this.bind(BasicPersonTripAnalysisHandler.class).asEagerSingleton();
		this.bind(NoiseAnalysisHandler.class).asEagerSingleton();
		
		this.addEventHandlerBinding().to(BasicPersonTripAnalysisHandler.class);
		this.addEventHandlerBinding().to(NoiseAnalysisHandler.class);
		
		this.addControlerListenerBinding().to(AnalysisControlerListener.class);
	}
}
