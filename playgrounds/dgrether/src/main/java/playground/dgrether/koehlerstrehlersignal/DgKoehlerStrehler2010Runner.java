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
package playground.dgrether.koehlerstrehlersignal;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
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
import playground.dgrether.linkanalysis.DgCountPerIterationGraph;
import playground.dgrether.linkanalysis.TTInOutflowEventHandler;


/**
 * @author dgrether
 *
 */
public class DgKoehlerStrehler2010Runner {

	public static final String defaultConfigFile = DgPaths.STUDIESDG + "koehlerStrehler2010/config.xml";

	public static final String lanesConfigFile = DgPaths.STUDIESDG + "koehlerStrehler2010/config_lanes.xml";

	public static final String signalsConfigFile = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals.xml";

	public static final String signalsConfigFileGershenson = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals_gershenson.xml";

	public static final String signalsConfigSol8005050 = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals_signal_control_solution_figure9_from_matsim_population_800_50_50.xml";

	public static final String signalsConfigSol800 = DgPaths.STUDIESDG + "koehlerStrehler2010/config_signals_signal_control_solution_figure9_from_matsim_population_800.xml";

	private String configFile = DgPaths.STUDIESDG + "koehlerStrehler2010/scenario4/config_signals_coordinated.xml";

	private TTInOutflowEventHandler handler23, handler27, handler54, handler58;

	private void runFromConfig(String conf) {
		String c = null;
		if (conf == null){
			c = configFile;
		}
		else {
			c = conf;
		}
		Controler controler = new Controler(c);
		controler.setOverwriteFiles(true);
		this.addControlerListener(controler);

		controler.run();
	}


	private void addControlerListener(Controler c) {
		//add some EventHandler to the EventsManager after the controler is started
		handler23 = new TTInOutflowEventHandler(new IdImpl("23"));
		handler27 = new TTInOutflowEventHandler(new IdImpl("27"));
		handler54 = new TTInOutflowEventHandler(new IdImpl("54"));
		handler58 = new TTInOutflowEventHandler(new IdImpl("58"));

		c.addControlerListener(new StartupListener() {
			public void notifyStartup(StartupEvent e) {
				e.getControler().getEvents().addHandler(handler23);
				e.getControler().getEvents().addHandler(handler27);
				e.getControler().getEvents().addHandler(handler54);
				e.getControler().getEvents().addHandler(handler58);
			}
		});

		//write some output after each iteration
		c.addControlerListener(new IterationEndsListener() {
			public void notifyIterationEnds(IterationEndsEvent e) {
				handler23.iterationsEnds(e.getIteration());
				handler27.iterationsEnds(e.getIteration());
				handler54.iterationsEnds(e.getIteration());
				handler58.iterationsEnds(e.getIteration());

				if ( e.getIteration() % 10 == 0 ) {
					DgTravelTimeCalculatorChart ttcalcChart = new DgTravelTimeCalculatorChart((TravelTimeCalculator)e.getControler().getTravelTimeCalculator());
					ttcalcChart.setStartTime(0.0);
					ttcalcChart.setEndTime(3600.0 * 1.5);
					List<Id> list = new ArrayList<Id>();
					list.add(new IdImpl("23"));
					list.add(new IdImpl("34"));
					list.add(new IdImpl("45"));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<Id>();
					list.add(new IdImpl("27"));
					list.add(new IdImpl("78"));
					list.add(new IdImpl("85"));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<Id>();
					list.add(new IdImpl("54"));
					list.add(new IdImpl("43"));
					list.add(new IdImpl("32"));
					ttcalcChart.addLinkId(list);
					list = new ArrayList<Id>();
					list.add(new IdImpl("58"));
					list.add(new IdImpl("87"));
					list.add(new IdImpl("72"));
					ttcalcChart.addLinkId(list);
					DgChartWriter.writeChart(e.getControler().getControlerIO().getIterationFilename(e.getIteration(), "ttcalculator"), 
							ttcalcChart.createChart());

					DgCountPerIterationGraph chart = new DgCountPerIterationGraph(e.getControler().getConfig().controler());
					chart.addCountEventHandler(handler23);
					chart.addCountEventHandler(handler27);
					chart.addCountEventHandler(handler54);
					chart.addCountEventHandler(handler58);
					DgChartWriter.writeChart(e.getControler().getControlerIO().getOutputFilename("countPerIteration"), chart.createChart());
				}
			}
		});
  	//write some output at shutdown
		c.addControlerListener(new ShutdownListener() {
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
		if (args == null || args.length == 0){
			new DgKoehlerStrehler2010Runner().runFromConfig(null);
		}
		else {
			new DgKoehlerStrehler2010Runner().runFromConfig(args[0]);
		}
	}


}
