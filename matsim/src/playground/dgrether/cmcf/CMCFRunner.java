/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package playground.dgrether.cmcf;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.controler.events.StartupEvent;
import org.matsim.controler.listener.StartupListener;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.run.OTFVis;
import org.matsim.trafficmonitoring.LinkSensorManager;
import org.matsim.trafficmonitoring.LinkTravelTimeCounter;
import org.matsim.utils.charts.XYLineChart;

import playground.dgrether.utils.MatsimIo;


/**
 * @author dgrether
 *
 */
public class CMCFRunner {
	
	private static final Logger log = Logger.getLogger(CMCFRunner.class);
	
	private static final boolean visualizationOnly = false ;
	
	
	public CMCFRunner() {
		Config config = null;
		CMCFScenarioGenerator.main(null);
		final MyEventHandler myHandler = new MyEventHandler();
		myHandler.setLink(new IdImpl("4"));
		
		if (!visualizationOnly) {			
			Controler controler = new Controler(CMCFScenarioGenerator.configOut);
			final LinkSensorManager lsm = new LinkSensorManager();
			lsm.addLinkSensor("3");
			lsm.addLinkSensor("4");
			controler.setOverwriteFiles(true);
			controler.addControlerListener(new StartupListener() {
				public void notifyStartup(StartupEvent e) {
					e.getControler().getEvents().addHandler(lsm);
					LinkTravelTimeCounter.init(e.getControler().getEvents(), e.getControler().getNetwork().getLinks().size());
					
//					e.getControler().getEvents().addHandler(myHandler);
				}});
			
			controler.run();
			NetworkLayer net = controler.getNetwork();
			Link link2 = net.getLink(new IdImpl("2"));
			Link link3 = net.getLink(new IdImpl("3"));
			Link link4 = net.getLink(new IdImpl("4"));
			
			double tt2 = controler.getTravelTimeCalculator().getLinkTravelTime(link2, 7.0 * 3600.0);
			double tt3 = controler.getTravelTimeCalculator().getLinkTravelTime(link3, 7.0 * 3600.0);
			double tt4 = controler.getTravelTimeCalculator().getLinkTravelTime(link4, 7.0 * 3600.0);
			
			log.info("avg tt on link 2 at 7:00: " + tt2);
			log.info("avg tt on link 3 at 7:00: " + tt3);
			log.info("avg tt on link 4 at 7:00: " + tt4);
			
			
			log.info("traffic on link 3: " + lsm.getLinkTraffic("3"));		
			log.info("traffic on link 4: " + lsm.getLinkTraffic("4"));
			
			log.info("Last travel time on 2: " + LinkTravelTimeCounter.getInstance().getLastLinkTravelTime("2"));
			log.info("Last travel time on 3: " + LinkTravelTimeCounter.getInstance().getLastLinkTravelTime("3"));
			log.info("Last travel time on 4: " + LinkTravelTimeCounter.getInstance().getLastLinkTravelTime("4"));
			config = controler.getConfig();
		  
//			writeGraphs(myHandler, config.controler().getOutputDirectory());
		}
		if (config == null) {
			config = new Config();
			config.addCoreModules();
			config = MatsimIo.loadConfig(config, CMCFScenarioGenerator.configOut);
		}
		
		System.out.println(config.controler());
		
		String[] args = {config.controler().getOutputDirectory() + "/ITERS/it." + config.controler().getLastIteration() + "/" + config.controler().getLastIteration() + ".otfvis.mvi"};
		
		OTFVis.main(args);
	}
	
	private void writeTTChart(MyEventHandler handler, String basePath) {
		XYLineChart ttimeChart = new XYLineChart("Experienced travel times at time t on link 4", "time", "travel time");
		double firstTimeStep = handler.travelTimes.firstKey();
		double lastTimeStep = handler.travelTimes.lastKey();
		int numberTimeSteps = (int)(lastTimeStep - firstTimeStep);
		double timeSteps[] = new double[numberTimeSteps];
		double travelTimes[] = new double[numberTimeSteps];
		
		double lastTT = handler.travelTimes.get(handler.travelTimes.firstKey());
		
		for (int i = 0; i < numberTimeSteps; i++) {
			timeSteps[i] = i + firstTimeStep;
			Double tt = handler.travelTimes.get(i + firstTimeStep);
			if (tt != null) {
				travelTimes[i] = tt;
				lastTT = tt;
				System.out.println(i + firstTimeStep + " tt is " + tt + " eventEnter at: " + (i + firstTimeStep - tt));
			}
			else {
				travelTimes[i] = lastTT;
			}

		}
		ttimeChart.addSeries("travel times", timeSteps, travelTimes);
		ttimeChart.saveAsPng(basePath + "/" + "ttlink4.png", 600, 800);
	}
	
	private void writeInOutFlowChart(MyEventHandler handler, String basePath) {
		double firstInflowTimeStep = handler.inflow.firstKey();
		double lastInflowTimeStep = handler.inflow.lastKey();
		double firstOutflowTimeStep = handler.outflow.firstKey();
		double lastOutflowTimeStep = handler.outflow.lastKey();
		System.out.println("firstin: " + firstInflowTimeStep);
		System.out.println("firstout: " + firstOutflowTimeStep);
		System.out.println("lastin: " + lastInflowTimeStep);
		System.out.println("lastOut: " + lastOutflowTimeStep);
		double firstTimeStep = Math.min(firstInflowTimeStep, firstOutflowTimeStep);
		double lastTimeStep = Math.max(lastInflowTimeStep, lastOutflowTimeStep);
		int numberTimeSteps = (int)(lastTimeStep - firstTimeStep);
		double timesteps[] = new double[numberTimeSteps];
		double inflow[] = new double[numberTimeSteps];
		double outflow[] = new double[numberTimeSteps];
		
		int index;
		Integer out, in;
		double lastOut = 0.0;
		double lastIn = 0.0;
		for (double i = firstTimeStep; i < lastTimeStep; i++) {
			index = (int) (i - firstTimeStep);
			timesteps[index] = i;
			in = handler.inflow.get(i);
			out = handler.outflow.get(i);
			if (in != null) {
				lastIn = in;
			}
			else {
				lastIn = 0.0;
			}
			if (out != null) {
				lastOut = out;
			}
			else {
				lastOut = 0.0;
			}
			if (index != 0) {
				inflow[index] = inflow[index-1] + lastIn;
				outflow[index] = outflow[index-1] + lastOut;				
			}
			else {
				inflow[index] = lastIn;
				outflow[index] = lastOut;
			}
		}
		//writing in and outlflow
		XYLineChart inoutflowChart = new XYLineChart("In and outflows on link 4", "time", "flow");
		inoutflowChart.addSeries("inflows", timesteps, inflow);
		inoutflowChart.addSeries("outflows", timesteps, outflow);
		inoutflowChart.saveAsPng(basePath + "/" + "inoutlink4.png", 600, 800);
//		inoutflowChart2.saveAsPng(basePath + "/" + "inoutlink42.png", 600, 800);
	
	}	
	
	private void writeGraphs(MyEventHandler handler, String basePath) {
		writeTTChart(handler, basePath);
		writeInOutFlowChart(handler, basePath);
	}
	
	private static class MyEventHandler implements EventHandlerLinkEnterI, EventHandlerLinkLeaveI {

		private Id linkId;

		private Map<Id, EventLinkEnter> enterEvents;
		
		private SortedMap<Double, Integer> inflow;
		
		private SortedMap<Double, Integer> outflow;
		
		private SortedMap<Double, Double> travelTimes;
		
		public MyEventHandler(){
			this.enterEvents = new HashMap<Id, EventLinkEnter>();
			this.inflow = new TreeMap<Double, Integer>();
			this.outflow = new TreeMap<Double, Integer>();
			this.travelTimes = new TreeMap<Double, Double>();
		}
		
		
		public void setLink(Id linkId) {
			this.linkId = linkId;
		}
		
		public void handleEvent(EventLinkEnter event) {
			Id id = event.link.getId();
			if (linkId.equals(id)) {
				Integer in = inflow.get(event.time);
				if (in == null) {
					in = Integer.valueOf(1);
				  inflow.put(event.time, in);
				}
				else {
					in = Integer.valueOf(in.intValue() + 1);
				}
				this.enterEvents.put(event.agent.getId(), event);
			}
		}


		public void handleEvent(EventLinkLeave event) {
			Id id = event.link.getId();
			if (linkId.equals(id)) {
				Integer out = outflow.get(event.time);
				if (out == null) {
					out = Integer.valueOf(1);
					outflow.put(event.time, out);
				}
				else {
					out = Integer.valueOf(out.intValue() + 1);
				}
				EventLinkEnter enterEvent = this.enterEvents.get(event.agent.getId());
				double tt = event.time - enterEvent.time;
				Double ttravel = travelTimes.get(event.time);
				if (ttravel == null) {
					travelTimes.put(Double.valueOf(event.time), Double.valueOf(tt));
				}
				else {
					ttravel = Double.valueOf(((ttravel.doubleValue() * (out.doubleValue() - 1)) + tt) / out.doubleValue());
					travelTimes.put(Double.valueOf(event.time), ttravel);
				}
			}
		}

		public void reset(int iteration) {
			this.enterEvents.clear();
			this.inflow.clear();
			this.outflow.clear();
			this.travelTimes.clear();
		}

		
	};
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new CMCFRunner();
		
	}

}
