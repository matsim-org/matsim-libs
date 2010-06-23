/* *********************************************************************** *
 * project: org.matsim.*
 * ScoreStats.java
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

package playground.meisterk.eaptus2010;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.jfree.data.time.MovingAverage;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * @author eaptus, meisterk based on mrieser
 */
public class ChangeProbStats implements StartupListener, IterationEndsListener, ShutdownListener {

//	public static final int MOVING_AVERAGE_SKIP_AND_PERIOD = 50;
	
	private enum DataSeries {
		CHANGE_QUOTE("AggregateChangeQuote"),
		MOVING_AVERAGE("movingAverage");

		private final String columnName;

		private DataSeries(String columnName) {
			this.columnName = columnName;
		}

		public String getColumnName() {
			return columnName;
		}

	}

	final private BufferedWriter out;

	private final int movingAverageSkipAndPeriod;
	private XYSeries history = null;
	private int minIteration = 0;
	private ExpBetaPlanChanger2 planChanger;

	private final static Logger log = Logger.getLogger(ChangeProbStats.class);

	/**
	 * Creates a new ScoreStats instance.
	 *
	 * @param population
	 * @param filename
	 * @param createPNG true if in every iteration, the scorestats should be visualized in a graph and written to disk.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public ChangeProbStats(final Population population, final String filename, ExpBetaPlanChanger2 planChanger, final int movingAverageSkipAndPeriod) throws FileNotFoundException, IOException {

		this.planChanger = planChanger;
		this.movingAverageSkipAndPeriod = movingAverageSkipAndPeriod;
		this.out = IOUtils.getBufferedWriter(filename);
		this.out.write("#iteration");
		for (DataSeries column : DataSeries.values()) {
			this.out.write("\t");
			this.out.write(column.getColumnName());
		}
		this.out.write(System.getProperty("line.separator"));
	}

	public void notifyStartup(final StartupEvent event) {
		Controler controler = event.getControler();
		this.minIteration = controler.getFirstIteration();
		int maxIter = controler.getLastIteration();
		int iterations = maxIter - this.minIteration;
		if (iterations > 10000) iterations = 1000; // limit the history size
		this.history = new XYSeries(this.getClass().getSimpleName(), true, false);
	}

	public void notifyIterationEnds(final IterationEndsEvent event) {

		//get the change data from planChanger:
		int numberOfChanges = 0;

		int size = planChanger.getTableOfAgentsAndDecisions().size();

		//go through the keys k in the HashMap tableOfAgentsAndDecisions:
		Iterator<Double> it = planChanger.getTableOfAgentsAndDecisions().values().iterator();
		while (it.hasNext()) {
			Double changed = it.next();
			if (changed == 1) {
				numberOfChanges++;
			}
		}

		//aggregated data:

		double dSize = size;
		double dNumberOfChanges = numberOfChanges;

		double changeQuote = dNumberOfChanges/dSize; //This is the Quote of agents who change their executed plan
		this.history.add(new XYDataItem(event.getIteration(), changeQuote));

		try {
			this.out.write(Integer.toString(event.getIteration()));
			for (DataSeries column : DataSeries.values()) {
				this.out.write("\t");

				switch(column) {
				case CHANGE_QUOTE:
					this.out.write(Double.toString(changeQuote));
					break;
				case MOVING_AVERAGE:
					if (this.history.getItemCount() > this.movingAverageSkipAndPeriod) {
						DefaultTableXYDataset tempDataset = new DefaultTableXYDataset();
						tempDataset.addSeries(this.history);
						XYSeries movingAverage = MovingAverage.createMovingAverage(
								tempDataset, 
								0, 
								"",
								this.movingAverageSkipAndPeriod,
								this.movingAverageSkipAndPeriod);
					this.out.write(Double.toString(movingAverage.getY(movingAverage.indexOf(event.getIteration())).doubleValue()));
					} else {
						this.out.write(Double.toString(Double.NaN));
					}
					break;
				}
			}
			this.out.write(System.getProperty("line.separator"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//clear the table of agents and their decisions:
		planChanger.clearTableOfAgentsAndDecisions();

	}

	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

