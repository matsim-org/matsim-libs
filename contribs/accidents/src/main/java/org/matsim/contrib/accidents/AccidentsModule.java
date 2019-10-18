/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.accidents;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Singleton;

/**
* @author ikaddoura
*/

public final class AccidentsModule extends AbstractModule {

	@Override
	public void install() {
		AccidentsConfigGroup accidentsConfigGroup = ConfigUtils.addOrGetModule( getConfig() , AccidentsConfigGroup.class );

		if ( accidentsConfigGroup.isEnableAccidentsModule()) {
			
			this.bind(AccidentsContext.class).in( Singleton.class ) ;
			
			this.bind(AnalysisEventHandler.class).in( Singleton.class ) ;
			this.addEventHandlerBinding().to(AnalysisEventHandler.class) ;
			
			this.addControlerListenerBinding().to(AccidentControlerListener.class);
		}
				
	}

}

