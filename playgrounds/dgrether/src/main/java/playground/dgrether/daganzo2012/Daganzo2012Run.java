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
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.qsim.interfaces.SignalGroupState;
import org.matsim.core.utils.io.IOUtils;

import playground.dgrether.linkanalysis.TTInOutflowEventHandler;
import playground.dgrether.signalsystems.analysis.DgGreenSplitWriter;
import playground.dgrether.signalsystems.analysis.DgSignalGreenSplitHandler;
import playground.dgrether.signalsystems.analysis.DgSignalGroupAnalysisData;


/**
 * @author dgrether
 *
 */
public class Daganzo2012Run {

	private static final String SEPARATOR = "\t";

	private TTInOutflowEventHandler handler3;
	private TTInOutflowEventHandler handler4;
	private DgSignalGreenSplitHandler greenSplitHandler;
	private String outfile;
	private BufferedWriter writer;
	private BufferedWriter splitWriter;
	
	public static void main(String[] args) {
		String config = args[0];
//		String config = "/media/data/work/repos/shared-svn/studies/dgrether/jobfiles/daganzo/1574_config_local.xml";
		new Daganzo2012Run().run(config);
	}

	private void run(String config) {
		Controler controler = new Controler(config);
		//add the signals module
//		DgSylviaConfig sylviaConfig = new DgSylviaConfig();
//		sylviaConfig.setSignalGroupMaxGreenScale(2.0);
//		sylviaConfig.setUseFixedTimeCycleAsMaximalExtension(true);
//		SylviaSignalsModule sylviaSignalsModule = new SylviaSignalsModule();
//		sylviaSignalsModule.setSylviaConfig(sylviaConfig);
//		controler.addOverridingModule(sylviaSignalsModule);
		/* sylvia moved to playground tthunig. If you want to use sylvia use e.g. TtBasicController in playground tthunig. theresa, apr'17 */
		controler.addOverridingModule(new SignalsModule());
		
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.getConfig().controler().setCreateGraphs(false);
        addControlerListener(controler);
        
		controler.run();
	}

	private void addControlerListener(MatsimServices c) {
		handler3 = new TTInOutflowEventHandler(Id.create("3", Link.class), Id.create("5", Link.class));
		handler4 = new TTInOutflowEventHandler(Id.create("4", Link.class));
		greenSplitHandler = new DgSignalGreenSplitHandler();
		greenSplitHandler.addSignalSystem(Id.create("1", SignalSystem.class));
		
		c.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent e) {
				e.getServices().getEvents().addHandler(handler3);
				e.getServices().getEvents().addHandler(handler4);
				outfile = e.getServices().getControlerIO().getOutputFilename("stats.txt");
				writer = IOUtils.getBufferedWriter(outfile);
				String header = "Iteration \t  number_veh_link_4  \t number_veh_link_3_5";
				try {
					writer.append(header);
					writer.newLine();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				
				//green splits
				e.getServices().getEvents().addHandler(greenSplitHandler);
				outfile = e.getServices().getControlerIO().getOutputFilename("splits.txt");
				splitWriter = IOUtils.getBufferedWriter(outfile);
				String splitHeader = DgGreenSplitWriter.createHeader();
				splitHeader = "Iteration \t" + splitHeader;
				try {
					splitWriter.append(splitHeader);
					splitWriter.newLine();
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
				
				
				for (Id<SignalSystem> ssid : greenSplitHandler.getSystemIdAnalysisDataMap().keySet()) {
					Map<Id<SignalGroup>, DgSignalGroupAnalysisData> signalGroupMap = greenSplitHandler.getSystemIdAnalysisDataMap().get(ssid).getSystemGroupAnalysisDataMap();
					for (Entry<Id<SignalGroup>, DgSignalGroupAnalysisData> entry : signalGroupMap.entrySet()) {
						// logg.info("for signalgroup: "+entry.getKey());
						for (Entry<SignalGroupState, Double> ee : entry.getValue().getStateTimeMap().entrySet()) {
							// logg.info(ee.getKey()+": "+ee.getValue());
							StringBuilder line = new StringBuilder();
							line.append(e.getIteration());
							line.append(SEPARATOR);
							line.append(ssid);
							line.append(SEPARATOR);
							line.append(entry.getKey());
							line.append(SEPARATOR);
							line.append(ee.getKey());
							line.append(SEPARATOR);
							line.append(ee.getValue());
							try {
								splitWriter.append(line.toString());
								splitWriter.newLine();
								splitWriter.flush();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
				
				
			}
		});
		
		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent e) {
				try {
					writer.flush();
					writer.close();
					splitWriter.flush();
					splitWriter.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
	
}
