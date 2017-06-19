/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.drt.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEventHandler;
import org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEventHandler;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class DrtRequestAnalyzer implements DrtRequestRejectedEventHandler, DrtRequestScheduledEventHandler,
		DrtRequestSubmittedEventHandler, PersonEntersVehicleEventHandler {

	
	private final Map<Id<Request>,DrtRequestSubmittedEvent> submittedRequests = new HashMap<>();
	private final Map<Id<Request>,Tuple<Double,Double>> waitTimeCompare = new HashMap<>();
	private final Map<Id<Person>,DrtRequestScheduledEvent> scheduledRequests = new HashMap<>();
	private final List<String> rejections = new ArrayList<>();
	private final Network network;
	
	@Inject
	public DrtRequestAnalyzer(EventsManager events, Network network) {
		events.addHandler(this);
		this.network = network;
	}
	
	
	/* (non-Javadoc)
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		submittedRequests.clear();
		scheduledRequests.clear();
		waitTimeCompare.clear();
		rejections.clear();
	}

	/* (non-Javadoc)
	 * @see org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler#handleEvent(org.matsim.api.core.v01.events.PersonEntersVehicleEvent)
	 */
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.scheduledRequests.containsKey(event.getPersonId())){
			DrtRequestScheduledEvent scheduled = scheduledRequests.remove(event.getPersonId());
			DrtRequestSubmittedEvent submission  = this.submittedRequests.remove(scheduled.getRequestId());
			double actualWaitTime = event.getTime() - submission.getTime();
			double estimatedWaitTime =  scheduled.getPickupTime() - submission.getTime();
			waitTimeCompare.put(submission.getRequestId(), new Tuple<>(actualWaitTime,estimatedWaitTime));
			
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler#handleEvent(org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEvent)
	 */
	@Override
	public void handleEvent(DrtRequestScheduledEvent event) {
		DrtRequestSubmittedEvent submission  = this.submittedRequests.get(event.getRequestId());
		if (submission!=null){
			this.scheduledRequests.put(submission.getPersonId(),event);
		}
		else throw new RuntimeException("Vehicle allocation without submission?");
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEventHandler#handleEvent(org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent)
	 */
	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		this.submittedRequests.put(event.getRequestId(), event);
	}

	/* (non-Javadoc)
	 * @see org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEventHandler#handleEvent(org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent)
	 */
	@Override
	public void handleEvent(DrtRequestRejectedEvent event) {
		DrtRequestSubmittedEvent submission = this.submittedRequests.remove(event.getRequestId());
		Coord fromCoord = network.getLinks().get(submission.getFromLinkId()).getCoord();
		Coord toCoord = network.getLinks().get(submission.getToLinkId()).getCoord();
		this.rejections.add(submission.getTime()+";"+submission.getPersonId()+";"+submission.getFromLinkId()+";"+submission.getToLinkId()+";"+fromCoord.getX()+";"+fromCoord.getY()+";"+toCoord.getX()+";"+toCoord.getY());
	}
	
	/**
	 * @return the waitTimeCompare
	 */
	public Map<Id<Request>, Tuple<Double, Double>> getWaitTimeCompare() {
		return waitTimeCompare;
	}
	
	/**
	 * @return the rejections
	 */
	public List<String> getRejections() {
		return rejections;
	}
	
	public void writeAndPlotWaitTimeEstimateComparison(String plotFileName, String textFileName) {
		BufferedWriter bw = IOUtils.getBufferedWriter(textFileName);
		XYSeriesCollection times = new XYSeriesCollection();

		XYSeries timess = new XYSeries("waittimes", true, true);
		times.addSeries(timess);

		try {
			bw.append("RequestId;actualWaitTime;estimatedWaitTime;deviate");
			for (Entry<Id<Request>, Tuple<Double, Double>> e : this.waitTimeCompare.entrySet()){
				bw.newLine();
				double first = e.getValue().getFirst();
				double second = e.getValue().getSecond();
				bw.append(e.getKey().toString()+";"+first+";"+second+";"+(first-second));
				timess.add(first, second);
			}
			bw.flush();
			bw.close();
			final JFreeChart chart2 = ChartFactory.createScatterPlot("Wait time", "Actual wait time [s]",
					"Estimated wait time [s]", times);
			NumberAxis yAxis = (NumberAxis)((XYPlot)chart2.getPlot()).getRangeAxis();
			NumberAxis xAxis = (NumberAxis)((XYPlot)chart2.getPlot()).getDomainAxis();
			yAxis.setUpperBound(xAxis.getUpperBound());
			xAxis.setLowerBound(0);
			yAxis.setLowerBound(0);
			ChartUtilities.writeChartAsPNG(new FileOutputStream(plotFileName), chart2, 1500, 1500);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
