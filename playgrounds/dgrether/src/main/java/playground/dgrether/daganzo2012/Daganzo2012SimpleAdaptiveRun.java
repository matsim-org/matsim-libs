/* *********************************************************************** *
 * project: org.matsim.*
 * Daganzo2012SimpleAdaptiveRun
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
package playground.dgrether.daganzo2012;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimFactory;
import org.matsim.core.utils.io.IOUtils;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;

import java.io.BufferedWriter;
import java.io.IOException;


/**
 * @author dgrether
 *
 */
public class Daganzo2012SimpleAdaptiveRun {
	
	private static final Logger log = Logger.getLogger(Daganzo2012SimpleAdaptiveRun.class);
	
	private TTInOutflowEventHandler handler3;
	private TTInOutflowEventHandler handler4;
	private String outfile;
	private BufferedWriter writer;


	private void run(String config) {
		final Controler controler = new Controler(config);
		controler.setOverwriteFiles(true);
        controler.getConfig().controler().setCreateGraphs(false);
        final SimpleAdaptiveControl adaptiveControl = new SimpleAdaptiveControl();
		addControlerListener(controler, adaptiveControl);
		final MobsimFactory mf = QSimFactory.createQSimFactory();
		
		controler.setMobsimFactory(new MobsimFactory() {
			private QSim mobsim;

			@Override
			public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
				mobsim = (QSim) mf.createMobsim(sc, eventsManager);
				mobsim.addMobsimEngine(adaptiveControl);
				return mobsim;
			}
			
		});
		
		controler.run();
	}

	private void addControlerListener(Controler c, final SimpleAdaptiveControl adaptiveControl) {
		handler3 = new TTInOutflowEventHandler(Id.create("3", Link.class), Id.create("5", Link.class));
		handler4 = new TTInOutflowEventHandler(Id.create("4", Link.class));
		
		c.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent e) {
				String initialRedString = e.getControler().getConfig().getParam("daganzo2012", "initialRedOn4");
				log.debug("using initial red of " + initialRedString + " s");
				double initialRed = Double.parseDouble(initialRedString);
				adaptiveControl.setInitialRedOn4(initialRed);
				e.getControler().getEvents().addHandler(adaptiveControl);
				e.getControler().getEvents().addHandler(handler3);
				e.getControler().getEvents().addHandler(handler4);
				outfile = e.getControler().getControlerIO().getOutputFilename("stats.txt");
				writer = IOUtils.getBufferedWriter(outfile);
				String header = "Iteration \t  number_veh_link_4  \t number_veh_link_3_5";
				try {
					writer.append(header);
					writer.newLine();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent e) {
				handler3.iterationsEnds(e.getIteration());
				handler4.iterationsEnds(e.getIteration());
				
				StringBuilder sb = new StringBuilder();
				sb.append(e.getIteration());
				sb.append("\t");
				sb.append(handler4.getCountPerIteration().get(e.getIteration()));
				sb.append("\t");
				sb.append(handler3.getCountPerIteration().get(e.getIteration()));
				try {
					writer.append(sb.toString());
					writer.newLine();
					writer.flush();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			}
		});
		
		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent e) {
				try {
					writer.flush();
					writer.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	
	
	public static void main(String[] args) {
		String config = args[0];
//		String config = "/media/data/work/repos/shared-svn/studies/dgrether/jobfiles/daganzo/1579_config_local.xml";

		new Daganzo2012SimpleAdaptiveRun().run(config);
	}


}
