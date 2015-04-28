/* *********************************************************************** *
, * project: org.matsim.*												   *
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
package playground.kai.usecases.janus;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.janusproject.kernel.agent.Kernels;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author nagel
 *
 */
public class Main {

	public static void main(String[] args) {
		final Config config = ConfigUtils.createConfig() ;
		
		config.network().setInputFile("examples/equil/network.xml"); 
		
		config.qsim().setEndTime(36.*3600.);
		
		config.controler().setLastIteration(0);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config) ;
		
		final Controler ctrl = new Controler(scenario) ;
		ctrl.setOverwriteFiles(true);

		ctrl.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new MobsimFactory() {
							@Override
							public Mobsim createMobsim(final Scenario sc, final EventsManager eventsManager) {
								final QSim qsim = (QSim) QSimUtils.createDefaultQSim(sc, eventsManager);

								qsim.addAgentSource(new AgentSource() {
									@Override
									public void insertAgentsIntoMobsim() {
										Logger.getLogger(this.getClass()).warn("here");

										final MobsimDriverAgent ag = new MyMobsimAgent(sc.getNetwork());

										// insert vehicle:
										final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(ag.getPlannedVehicleId(), VehicleUtils.getDefaultVehicleType());
										qsim.createAndParkVehicleOnLink(vehicle, ag.getCurrentLinkId());

										// insert traveler agent:
										qsim.insertAgentIntoMobsim(ag);
									}
								});

								return qsim;
							}
						}.createMobsim(ctrl.getScenario(), ctrl.getEvents());
					}
				});
			}
		});

		ctrl.addControlerListener(new ShutdownListener(){
			@Override
			public void notifyShutdown(ShutdownEvent event) {
				Kernels.killAll(); 
			}
		});
		
		ctrl.run() ;
		
	}

	
}
