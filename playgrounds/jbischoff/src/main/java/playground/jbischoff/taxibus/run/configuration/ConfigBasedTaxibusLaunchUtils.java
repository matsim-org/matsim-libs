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
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.VrpDataImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import com.google.inject.util.Providers;

import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LineDispatcher;
import playground.jbischoff.taxibus.algorithm.optimizer.fifo.Lines.LinesUtils;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusPassengerOrderManager;
import playground.jbischoff.taxibus.algorithm.tubs.datastructure.StateLookupTable;
import playground.jbischoff.taxibus.algorithm.tubs.datastructure.StateSpace;
import playground.jbischoff.taxibus.run.sim.TaxibusQSimProvider;
import playground.jbischoff.taxibus.run.sim.TaxibusServiceRoutingModule;

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
        new VehicleReader(scenario.getNetwork(), vrpData).parse(tbcg.getVehiclesFileUrl(scenario.getConfig().getContext()));
        final TaxibusPassengerOrderManager orderManager;
        final LineDispatcher dispatcher;
        final StateLookupTable lookuptable;
		if (tbcg.getAlgorithmConfig().endsWith("ine")){
			dispatcher = LinesUtils.createLineDispatcher(tbcg.getLinesFileUrl(scenario.getConfig().getContext()).getFile(), tbcg.getZonesXmlFileUrl(scenario.getConfig().getContext()).getFile(),tbcg.getZonesShpFileUrl(scenario.getConfig().getContext()).getFile(),vrpData,tbcg);}
		else  {
			dispatcher = null;}
		if (tbcg.getAlgorithmConfig().equals("stateBased")){
			lookuptable = new StateLookupTable(7.25*3600, 8*3600, 60, 0, controler.getConfig().controler().getOutputDirectory());
			controler.addControlerListener(lookuptable);
		} else {
			lookuptable = null;
		}

		
		if (tbcg.isPrebookTrips()){	
		orderManager = new TaxibusPassengerOrderManager();
		} else {
			orderManager = null;
		}
		if (tbcg.isOtfvis()){
            controler.addOverridingModule(new OTFVisLiveModule());

		}
		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(TaxibusQSimProvider.class));

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				if (dispatcher!=null){
				addEventHandlerBinding().toInstance(dispatcher);
				bind(LineDispatcher.class).toInstance(dispatcher);
				}
				else {
					bind(LineDispatcher.class).toProvider(Providers.of(null));
				}
				if (orderManager!=null){
				addEventHandlerBinding().toInstance(orderManager);
				bind(TaxibusPassengerOrderManager.class).toInstance(orderManager);
				}else {
					bind(TaxibusPassengerOrderManager.class).toProvider(Providers.of(null));
				}
				if (lookuptable!=null){
					bind(StateSpace.class).toInstance(lookuptable);
					}
				else {
					bind(StateSpace.class).toProvider(Providers.of(null));
				}
				
				
				addRoutingModuleBinding("taxibus").toInstance(new TaxibusServiceRoutingModule(controler));
				bind(VrpData.class).toInstance(vrpData);

			}
		});
		
		
		
		
	}


}
