/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.kai.usecases.assignmentEmulatingQLane;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.qnetsimengine.AssignmentEmulatingQLaneNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.scenario.ScenarioUtils;


/**
 * @author nagel
 *
 */
public class AssignmentEmulatingQLaneMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final Config config = ConfigUtils.loadConfig("../../../matsim/matsim/examples/equil/config.xml");
		
		config.plans().setInputFile("plans2000.xml.gz");
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		
		config.qsim().setNumberOfThreads(1);
		
		config.parallelEventHandling().setNumberOfThreads(1);

		final Scenario scenario = ScenarioUtils.loadScenario(config) ;

		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule( new AbstractModule(){
			@Override public void install() {
				bind( QNetworkFactory.class ).to( AssignmentEmulatingQLaneNetworkFactory.class ) ;
			}
		});
		
		controler.addOverridingModule( new OTFVisLiveModule() );
		
		controler.run();
	}

}
