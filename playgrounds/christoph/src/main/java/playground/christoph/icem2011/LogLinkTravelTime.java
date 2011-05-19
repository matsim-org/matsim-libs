/* *********************************************************************** *
 * project: org.matsim.*
 * LogLinkTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.icem2011;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;

public class LogLinkTravelTime implements SimulationBeforeSimStepListener, BeforeMobsimListener, AfterMobsimListener {

	private static final Logger log = Logger.getLogger(LogLinkTravelTime.class);
	
	private Collection<Link> links;
	private TravelTime expectedTravelTime;
	private TravelTime measuredTravelTime;
	
	private String delimiter = ",";
	private Charset charset = Charset.forName("UTF-8");

	private int nextWrite = 0;
	private int writeInterval = 60;
	private int graphCutOffTime = 30*3600;
	
//	private Map<Id, StringBuffer> data = null;
	private Map<Id, List<Double>> data = null;	// <LinkId, <ExpectedTravelTime>>
	private List<Double> times;
	
	public LogLinkTravelTime(Collection<Link> links, TravelTime expectedTravelTime, TravelTime measuredTravelTime) {
		this.links = links;
		this.expectedTravelTime = expectedTravelTime;
		this.measuredTravelTime = measuredTravelTime;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		
//		data = new HashMap<Id, StringBuffer>();
//		
//		for (Link link : links) {
//			data.put(link.getId(), new StringBuffer());
//		}
		times = new ArrayList<Double>();
		data = new HashMap<Id, List<Double>>();
		for (Link link : links) {
			data.put(link.getId(), new ArrayList<Double>());
		}
	}
	
	@Override
	public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
	
		double time = e.getSimulationTime();
		if (time >= nextWrite) {
			nextWrite += writeInterval;
			times.add(time);
			
			for (Link link : links) {
//				double ett = expectedTravelTime.getLinkTravelTime(link, time);
//				double mtt = measuredTravelTime.getLinkTravelTime(link, time);
//				data.get(link.getId()).append(time + delimiter + ett + delimiter + mtt + "\n");
				double ett = expectedTravelTime.getLinkTravelTime(link, time);
				data.get(link.getId()).add(ett);
			}
		}
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		try {
			log.info("Writing expected travel time files...");
			Counter counter = new Counter("Writing expected travel time files: "); 
			for (Link link : links) {
				String file = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "expectedLinkTravelTimes_" + link.getId() + ".txt");
		
				FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
				BufferedWriter bw = new BufferedWriter(osw);
				
				bw.write("time" + delimiter + "expected travel time" + delimiter + "measured travel time" + "\n");
//				bw.write(data.get(link.getId()).toString());
				List<Double> etts = data.get(link.getId());
				for (int i = 0; i < times.size(); i++) {
					double time = times.get(i);
					double ett = etts.get(i);
					double mtt = measuredTravelTime.getLinkTravelTime(link, time);
					String string = time + delimiter + ett + delimiter + mtt + "\n";
					bw.write(string);
				}
				
				bw.close();
				osw.close();
				fos.close();
				
				String chartFile = event.getControler().getControlerIO().getIterationFilename(event.getIteration(), "expectedLinkTravelTimes_" + link.getId() + ".png");
				createChart(link, chartFile);
				counter.incCounter();
			}
			counter.printCounter();
			log.info("done.");
		} catch (IOException e1) {
			Gbl.errorMsg(e1);
		}
	}
	
	private void createChart(Link link, String file) {
		
//		String s = data.get(linkId).toString();
//		String[] travelTimes = s.split("\n");
		List<Double> etts = data.get(link.getId());
		
		final XYSeriesCollection xyData = new XYSeriesCollection();
		final XYSeries expectedTravelTimes = new XYSeries("expected travel times", false, true);
		final XYSeries measuredTravelTimes = new XYSeries("measured travel times", false, true);
		for (int i = 0; i < times.size(); i++) {
			double time = times.get(i);
			if (time > graphCutOffTime) break;	// do not display values > 30h in the graph
			double hour = Double.valueOf(time) / 3600.0;
			expectedTravelTimes.add(hour, Double.valueOf(etts.get(i)));
			measuredTravelTimes.add(hour, Double.valueOf(measuredTravelTime.getLinkTravelTime(link, time)));
		}

		xyData.addSeries(expectedTravelTimes);
		xyData.addSeries(measuredTravelTimes);

//		final JFreeChart chart = ChartFactory.createXYStepChart(
		final JFreeChart chart = ChartFactory.createXYLineChart(
				"Compare expected and measured travel times for link " + link.getId().toString(),
				"time", "travel time",
	        xyData,
	        PlotOrientation.VERTICAL,
	        true,   // legend
	        false,   // tooltips
	        false   // urls
	    );

		XYPlot plot = chart.getXYPlot();

		final CategoryAxis axis1 = new CategoryAxis("hour");
		axis1.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 7));
		plot.setDomainAxis(new NumberAxis("time"));

		try {
			ChartUtilities.saveChartAsPNG(new File(file), chart, 1024, 768);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}