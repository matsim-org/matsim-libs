/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.jbischoff.taxibus.run.configuration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.taxi.run.TaxiQSimProvider;
import org.matsim.core.controler.*;

import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.*;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusPassengerOrderManager;
import playground.jbischoff.taxibus.run.sim.*;

/**
 * @author jbischoff
 *
 */
public class ConfigBasedTaxibusLaunchUtils {
		private Controler controler;
		
		
		
		public ConfigBasedTaxibusLaunchUtils(Controler controler) {
			this.controler = controler;
			
		}
		
	 
	public  void initiateTaxibusses(){
		//this is done exactly once per simulation
		
		Scenario scenario = controler.getScenario();
		final TaxibusConfigGroup tbcg = (TaxibusConfigGroup) scenario.getConfig().getModule("taxibusConfig");
        final VrpData vrpData = new VrpDataImpl();
        new VehicleReader(scenario.getNetwork(), vrpData).parse(tbcg.getVehiclesFile());

		final LineDispatcher dispatcher = LinesUtils.createLineDispatcher(tbcg.getLinesFile(), tbcg.getZonesXmlFile(), tbcg.getZonesShpFile(),vrpData,tbcg);	
		final TaxibusPassengerOrderManager orderManager = new TaxibusPassengerOrderManager();
		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule());
        controler.addOverridingModule(new DynQSimModule<>(TaxibusQSimProvider.class));

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				addEventHandlerBinding().toInstance(dispatcher);
				addEventHandlerBinding().toInstance(orderManager);
				addRoutingModuleBinding("taxibus").toInstance(new TaxibusServiceRoutingModule(controler));
				bind(TaxibusPassengerOrderManager.class).toInstance(orderManager);
				bind(LineDispatcher.class).toInstance(dispatcher);
				bind(VrpData.class).toInstance(vrpData);

			}
		});
		
		
		
		
	}


}
