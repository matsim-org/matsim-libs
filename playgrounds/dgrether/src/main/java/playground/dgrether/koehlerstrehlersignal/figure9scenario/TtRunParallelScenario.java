/* *********************************************************************** *
 * project: org.matsim.*
 * DgKoehlerStrehler2010Runner
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.dgrether.koehlerstrehlersignal.figure9scenario;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import playground.dgrether.DgPaths;
import playground.dgrether.analysis.charts.DgTravelTimeCalculatorChart;
import playground.dgrether.analysis.charts.utils.DgChartWriter;
import playground.dgrether.koehlerstrehlersignal.analysis.DgMfd;
import playground.dgrether.linkanalysis.DgCountPerIterationGraph;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;


/**
 * This class is not working yet!
 * 
 * @author tthunig
 *
 */
public class TtRunParallelScenario {

	public static final String defaultConfigFile = DgPaths.STUDIESDG + "koehlerStrehler2010/config.xml";

	public static final String lanesConfigFile = DgPaths.STUDIESDG + "koehlerStrehler2010/config_lanes.xml";

	public static final String signalsConfigFile = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals.xml";

	public static final String signalsConfigFileGershenson = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals_gershenson.xml";

	public static final String signalsConfigSol8005050 = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals_signal_control_solution_figure9_from_matsim_population_800_50_50.xml";

	public static final String signalsConfigSol800 = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals_signal_control_solution_figure9_from_matsim_population_800.xml";

//	private String configFile = DgPaths.STUDIESDG + "koehlerStrehler2010/scenario5/config_signals_coordinated.xml";
	
	private String configFile = DgPaths.STUDIESDG + "koehlerStrehler2010/scenario5/config_testing.xml";

	private TTInOutflowEventHandler handler23, handler27, handler54, handler58;

	private DgMfd mfdHandler;
	
	private void runFromConfig(String conf) {
		String c = null;
		if (conf == null){
			c = configFile;
		}
		else {
			c = conf;
		}
		Controler controler = new Controler(c);
		this.addControlerListener(controler);

		controler.run();
	}


	private void addControlerListener(Controler c) {
		
		//add some EventHandler to the EventsManager after the controler is started
		handler23 = new TTInOutflowEventHandler(Id.create("23", Link.class));
		handler27 = new TTInOutflowEventHandler(Id.create("27", Link.class));
		handler54 = new TTInOutflowEventHandler(Id.create("54", Link.class));
		handler58 = new TTInOutflowEventHandler(Id.create("58", Link.class));
		
		
		c.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent e) {
				mfdHandler = new DgMfd(e.getControler().getScenario().getNetwork(), 1.0);
				e.getControler().getEvents().addHandler(handler23);
				e.getControler().getEvents().addHandler(handler27);
				e.getControler().getEvents().addHandler(handler54);
				e.getControler().getEvents().addHandler(handler58);
				
				e.getControler().getEvents().addHandler(mfdHandler);
			}
		});

		//write some output after each iteration
		c.addControlerListener(new IterationEndsListener() {
			@Override
			public void notifyIterationEnds(IterationEndsEvent e) {
				handler23.iterationsEnds(e.getIteration());
				handler27.iterationsEnds(e.getIteration());
				handler54.iterationsEnds(e.getIteration());
				handler58.iterationsEnds(e.getIteration());

				if ( e.getIteration() % 10 == 0 ) {
					DgTravelTimeCalculatorChart ttcalcChart = new DgTravelTimeCalculatorChart((TravelTimeCalculator)e.getControler().getLinkTravelTimes());
					ttcalcChart.setStartTime(0.0);
					ttcalcChart.setEndTime(3600.0 * 1.5);
					List<Id<Link>> list = new ArrayList<>();
					list.add(Id.create("23", Link.class));
					list.add(Id.create("34", Link.class));
					list.add(Id.create("45", Link.class));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<>();
					list.add(Id.create("27", Link.class));
					list.add(Id.create("78", Link.class));
					list.add(Id.create("85", Link.class));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<>();
					list.add(Id.create("54", Link.class));
					list.add(Id.create("43", Link.class));
					list.add(Id.create("32", Link.class));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<>();
					list.add(Id.create("58", Link.class));
					list.add(Id.create("87", Link.class));
					list.add(Id.create("72", Link.class));
					ttcalcChart.addLinkId(list);
					DgChartWriter.writeChart(e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "ttcalculator"), 
							ttcalcChart.createChart());

					DgCountPerIterationGraph chart = new DgCountPerIterationGraph(e.getControler().getConfig().controler());
					chart.addCountEventHandler(handler23);
					chart.addCountEventHandler(handler27);
					chart.addCountEventHandler(handler54);
					chart.addCountEventHandler(handler58);
					DgChartWriter.writeChart(e.getControler().getControlerIO().getOutputFilename("countPerIteration"), chart.createChart());
				
				
					mfdHandler.writeFile(e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "mfd.txt"));
				}
			}
		});
  	//write some output at shutdown
		c.addControlerListener(new ShutdownListener() {
			@Override
			public void notifyShutdown(ShutdownEvent e) {
				DgCountPerIterationGraph chart = new DgCountPerIterationGraph(e.getControler().getConfig().controler());
				chart.addCountEventHandler(handler23, "Number of cars on link 23");
				chart.addCountEventHandler(handler27, "Number of cars on link 27");
				chart.addCountEventHandler(handler54, "Number of cars on link 54");
				chart.addCountEventHandler(handler58, "Number of cars on link 58");
				DgChartWriter.writeChart(e.getControler().getControlerIO().getOutputFilename("countPerIteration"), chart.createChart());
			}
		});



	}


	public static void main(String[] args) {
		
//		int capacity = 1800;
//		String date = "2015-04-02";
//		String signals = "green"; // e.g. "bc" or "green"

//		String baseDir = DgPaths.RUNSSVN + "cottbus/parallelScenario/" + date
//				+ "_cap" + capacity + "_sig-" + signals;

		
		if (args == null || args.length == 0){
			new TtRunParallelScenario().runFromConfig(null);
		}
		else {
			new TtRunParallelScenario().runFromConfig(args[0]);
		}
	}


}
