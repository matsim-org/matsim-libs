/* *********************************************************************** *
 * project: org.matsim.*
 * Daganzo2012Run
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

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.linkanalysis.TTInOutflowEventHandler;
import playground.dgrether.signalsystems.sylvia.DgSylviaConfig;
import playground.dgrether.signalsystems.sylvia.controler.DgSylviaControlerListenerFactory;


/**
 * @author dgrether
 *
 */
public class Daganzo2012Run {

	private TTInOutflowEventHandler handler3;
	private TTInOutflowEventHandler handler4;
	private String outfile;
	private BufferedWriter writer;
	
	public static void main(String[] args) {
		String config = args[0];
		new Daganzo2012Run().run(config);
	}

	private void run(String config) {
		Controler controler = new Controler(config);
		DgSylviaConfig sylviaConfig = new DgSylviaConfig();
		sylviaConfig.setMaxGreenScale(2.0);
		controler.setSignalsControllerListenerFactory(new DgSylviaControlerListenerFactory(sylviaConfig));
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		addControlerListener(controler);
		controler.run();
	}

	private void addControlerListener(Controler c) {
		handler3 = new TTInOutflowEventHandler(new IdImpl("3"), new IdImpl("5"));
		handler4 = new TTInOutflowEventHandler(new IdImpl("4"));
	
		c.addControlerListener(new StartupListener() {
			public void notifyStartup(StartupEvent e) {
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
	
}
