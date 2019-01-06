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

package org.matsim.codeexamples.mobsim.ownMobsimAgent;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfigGroup;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author nagel
 *
 */
public class RunAddAgentSourceExample{

	public static void main(String[] args){
		Config config = ConfigUtils.loadConfig( "scenarios/equil/example5-config.xml" );
		config.qsim().setEndTime( 25 * 60 * 60 );
		config.controler().setLastIteration( 0 );
		config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		Scenario scenario = ScenarioUtils.loadScenario( config );

		final Controler controler = new Controler( scenario );

		{
			QSimComponentsConfigGroup cconfig = ConfigUtils.addOrGetModule( config, QSimComponentsConfigGroup.class );
			cconfig.getActiveComponents().add( "newAgentSource" );

			controler.addOverridingQSimModule( new AbstractQSimModule(){
				@Override
				protected void configureQSim(){
					this.addQSimComponentBinding( "newAgentSource" ).to( MyAgentSource.class );
				}
			} );
		}

		controler.run();
	}

	private static class MyAgentSource implements AgentSource{
		@Inject private QSim qsim;

		@Override
		public void insertAgentsIntoMobsim(){
			// insert traveler agent:
			final MobsimAgent ag = new MyMobsimAgent( qsim.getScenario(), qsim.getSimTimer() );
			qsim.insertAgentIntoMobsim( ag );

			// insert vehicle:
			final Vehicle vehicle = VehicleUtils.getFactory().createVehicle( Id.create( ag.getId(), Vehicle.class ), VehicleUtils.getDefaultVehicleType() );
			QVehicleImpl qVeh = new QVehicleImpl( vehicle );
			qsim.addParkedVehicle( qVeh, ag.getCurrentLinkId() );

		}
	}

}
