/* *********************************************************************** *
 * project: kai
 * KaiControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package tutorial.programming.simpleadaptivesignalengine;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

public class RunSimpleAdaptiveSignalExample {

	public static void main(String[] args) {
		
		final Controler controler = new Controler( "examples/config/daganzo-config.xml" ) ;
		
		controler.getConfig().controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists ) ;
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(SimpleAdaptiveSignal.class).asEagerSingleton();
				addMobsimListenerBinding().to(SimpleAdaptiveSignal.class);
				addEventHandlerBinding().to(SimpleAdaptiveSignal.class);
			}
		});
		controler.run();
	
	}

}
