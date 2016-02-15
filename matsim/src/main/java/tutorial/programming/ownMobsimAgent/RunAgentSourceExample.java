/* *********************************************************************** *
 * project: kai
 * Main.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package tutorial.programming.ownMobsimAgent;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import com.google.inject.Provider;

/**
 * @author nagel
 *
 */
public class RunAgentSourceExample {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/tutorial/config/example5-config.xml" ) ;
		config.qsim().setEndTime(25 * 60 * 60);
		config.controler().setLastIteration(0);
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		scenario.getPopulation().getPersons().clear();

		final Controler controler = new Controler( scenario );
		controler.addOverridingModule(new AbstractModule() {
			@Override public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Inject Scenario scenario ;
					@Inject EventsManager events ;
					@Override public Mobsim get() {
						final QSim qsim = QSimUtils.createDefaultQSim( scenario, events );
						qsim.addAgentSource(new AgentSource() {
							@Override
							public void insertAgentsIntoMobsim() {
								// insert traveler agent:
								final MobsimAgent ag = new MyMobsimAgent(qsim.getScenario(), qsim.getSimTimer());
								qsim.insertAgentIntoMobsim(ag);

								// insert vehicle:
								final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(ag.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType());
								final Id<Link> linkId4VehicleInsertion = Id.createLinkId(1);
								qsim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
							}
						});
						return qsim;
					}
				});
			}
		});
		controler.run();
	}

}
