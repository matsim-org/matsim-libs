/* *********************************************************************** *
 * project: org.matsim.*
 * Test.java
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

package playground.gregor.mviProd;

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;


public class PlotsSlideShow implements AgentArrivalEventHandler  {


	private static final int RESOLUTION = 60;
	private int evacuated = 0;
	private double time;
	private int offset;
	private final List<DataPoint> data = new ArrayList<DataPoint>();
	private final String eventFile;
	private double maxX;
	private double maxY;
	private final String outputDir;

	
	
	
	public PlotsSlideShow(final String eventFile, final String outputDir) {
		this.eventFile = eventFile;
		this.outputDir = outputDir;
	}


	public void run() {
		readEventFile();
		genPlots();
	}
	
	
	private void genPlots() {
		DataPoint dp = this.data.get(this.data.size()-1);
		this.maxX = dp.x;
		this.maxY = dp.y;
		for (int i = 0; i < this.data.size(); i++) {
			double [] x = new double[i+1];
			double [] y = new double[i+1];
			for (int j = 0; j <= i; j++) {
				DataPoint tmp = this.data.get(j);
				x[j] = tmp.x;
				y[j] = tmp.y;
			}
			
			String fill="0";
			if (i < 10) {
				fill = "000";
			} else if (i < 100) {
				fill = "00";
			}
			String filename = this.outputDir + "/plot" + fill + i + ".png";
			createXYLineChart(filename, x, y);
			
		}
	}


	private void readEventFile() {
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(this);
		new EventsReaderTXTv1(events).readFile(this.eventFile);
		
	}


	public void createXYLineChart(final String filename, final double [] x, final double [] y) {
		XYLineChartFixedRange chart = new XYLineChartFixedRange("Evacuation curve", "time", "# evacuees",this.maxX,this.maxY);
		chart.addSeries("", x, y);
		chart.saveAsPng(filename, 400, 300);
	}
	
	
	public void handleEvent(final AgentArrivalEvent event) {
		this.evacuated++;
		if (this.time == 0) {
			this.offset = (int)event.getTime();
			this.time = 1;
			addDataPoint();
		}
		
		
		if ((event.getTime() -this.offset)> this.time && ((int)(event.getTime()-this.offset) % RESOLUTION == 0)){
			this.time = (int) event.getTime() - this.offset;
			addDataPoint();
		}
	}

	private void addDataPoint() {
		DataPoint d = new DataPoint();
		d.x = this.time / RESOLUTION;
		d.y = this.evacuated;
		this.data.add(d);
	}

	public void reset(final int iteration) {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(final String [] args) {
		new PlotsSlideShow("../outputs/output/ITERS/it.0/0.events.txt.gz", "./tmp/").run();
	}


	private static class DataPoint {
		double x;
		double y;
	}


}
