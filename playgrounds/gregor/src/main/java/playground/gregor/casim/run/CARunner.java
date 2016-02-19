/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.gregor.casim.run;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioUtils;
import playground.gregor.casim.proto.CALinkInfos.CALinInfos;
import playground.gregor.casim.simulation.CAMobsimFactory;
import playground.gregor.casim.simulation.physics.AbstractCANetwork;
import playground.gregor.casim.simulation.physics.CASingleLaneNetworkFactory;
import playground.gregor.sim2d_v4.debugger.eventsbaseddebugger.QSimDensityDrawer;

import java.io.FileInputStream;
import java.io.IOException;

public class CARunner implements IterationStartsListener {

	private MatsimServices controller;
	private QSimDensityDrawer qSimDrawer;

	public static void main(String[] args) {
		if (args.length != 3) {
			printUsage();
			System.exit(-1);
		}
		String qsimConf = args[0];
		Config c = ConfigUtils.loadConfig(qsimConf);

		c.controler().setOutputDirectory("/Users/laemmel/devel/nyc/output2/");
		
		c.controler().setWriteEventsInterval(1);
		c.controler().setMobsim("casim");
		Scenario sc = ScenarioUtils.loadScenario(c);
		
		
		CALinInfos infos = null;
		try {
			FileInputStream str = new FileInputStream("/Users/laemmel/devel/nyc/gct_vicinity/ca_link_infos");
			infos = CALinInfos.parseFrom(str);
			str.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		sc.addScenarioElement("CALinkInfos", infos);
		
		
		// sc.addScenarioElement(Sim2DScenario.ELEMENT_NAME, sim2dsc);

		// c.qsim().setEndTime(120);
		 c.qsim().setEndTime(30*3600);
		// c.qsim().setEndTime(41*60);//+30*60);

		final Controler controller = new Controler(sc);


		controller.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);


		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addRoutingModuleBinding("car").toProvider(CARoutingModule.class);
			}
		});

		final CAMobsimFactory factory = new CAMobsimFactory();
		if (args[1].equals("false")) {
			factory.setCANetworkFactory(new CASingleLaneNetworkFactory());
		}
		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				if (getConfig().controler().getMobsim().equals("casim")) {
					bind(Mobsim.class).toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return factory.createMobsim(controller.getScenario(), controller.getEvents());
						}
					});
				}
			}
		});

		controller.addControlerListener(new IterationStartsListener() {
			
			@Override
			public void notifyIterationStarts(IterationStartsEvent event) {
				AbstractCANetwork.EMIT_VIS_EVENTS = (event.getIteration()) % 100 == 0 && (event.getIteration()) > 0;
				
			}
		});

		// DefaultTripRouterFactoryImpl fac = builder.build(sc);
		// DefaultTripRouterFactoryImpl fac = new
		// DefaultTripRouterFactoryImpl(sc, null, null);

		// controller.setTripRouterFactory(fac);
		controller.run();
	}


	protected static void printUsage() {
		System.out.println();
		System.out.println("CARunner");
		System.out.println("Controller for ca (pedestrian) simulations.");
		System.out.println();
		System.out.println("usage : CARunner config multilane_mode visualize");
		System.out.println();
		System.out.println("config:   A MATSim config file.");
		System.out.println("multilane_mode:   one of {true,false}.");
		System.out.println("visualize:   one of {true,false}.");
		System.out.println();
		System.out.println("---------------------");
		System.out.println("2014, matsim.org");
		System.out.println();
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if ((event.getIteration()) % 5 == 0 || event.getIteration() > 0) {
			// this.factory.debug(this.visDebugger);
			this.controller.getEvents().addHandler(this.qSimDrawer);
            this.controller.getConfig().controler().setCreateGraphs(true);
        } else {
			// this.factory.debug(null);
			this.controller.getEvents().removeHandler(this.qSimDrawer);
            this.controller.getConfig().controler().setCreateGraphs(false);
        }
		// this.visDebugger.setIteration(event.getIteration());
	}
}
