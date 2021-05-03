/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
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

package org.matsim.contrib.drt.analysis.zonal;

import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.vrpagent.AbstractTaskEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.collections.Tuple;

import com.opencsv.CSVWriter;

public class ZonalIdleVehicleXYVisualiser
		implements TaskStartedEventHandler, TaskEndedEventHandler, IterationEndsListener {

	private final String mode;
	private final DrtZonalSystem zonalSystem;
	private final MatsimServices services;

	private final Map<DrtZone, LinkedList<Tuple<Double, Integer>>> zoneEntries = new HashMap<>();

	public ZonalIdleVehicleXYVisualiser(MatsimServices services, String mode, DrtZonalSystem zonalSystem) {
		this.services = services;
		this.mode = mode;
		this.zonalSystem = zonalSystem;
		initEntryMap();
	}

	private void initEntryMap() {
		for (DrtZone z : zonalSystem.getZones().values()) {
			LinkedList<Tuple<Double, Integer>> list = new LinkedList<>();
			list.add(new Tuple<>(0d, 0));
			zoneEntries.put(z, list);
		}
	}

	@Override
	public void handleEvent(TaskStartedEvent event) {
		handleEvent(event, zone -> {
			LinkedList<Tuple<Double, Integer>> zoneTuples = zoneEntries.get(zone);
			Integer oldNrOfVeh = zoneTuples.getLast().getSecond();
			zoneTuples.add(new Tuple<>(event.getTime(), oldNrOfVeh + 1));
		});
	}

	@Override
	public void handleEvent(TaskEndedEvent event) {
		handleEvent(event, zone -> {
			LinkedList<Tuple<Double, Integer>> zoneTuples = zoneEntries.get(zone);
			Integer oldNrOfVeh = zoneTuples.getLast().getSecond();
			zoneTuples.add(new Tuple<>(event.getTime(), oldNrOfVeh - 1));
		});
	}

	private void handleEvent(AbstractTaskEvent event, Consumer<DrtZone> handler) {
		if (event.getDvrpMode().equals(mode) && event.getTaskType().equals(DrtStayTask.TYPE)) {
			DrtZone zone = zonalSystem.getZoneForLinkId(event.getLinkId());
			if (zone != null) {
				handler.accept(zone);
			}
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		String filename = services.getControlerIO()
				.getIterationFilename(services.getIterationNumber(), mode + "_idleVehiclesPerZoneXY.csv");

		try {
			CSVWriter writer = new CSVWriter(Files.newBufferedWriter(Paths.get(filename)), ';', '"', '"', "\n");
			writer.writeNext(new String[] { "zone", "X", "Y", "time", "idleDRTVehicles" }, false);
			this.zoneEntries.forEach((zone, entriesList) -> {
				Coord c = zone.getCentroid();
				entriesList.forEach(entry -> writer.writeNext(
						new String[] { zone.getId(), "" + c.getX(), "" + c.getY(), "" + entry.getFirst(),
								"" + entry.getSecond() }, false));
			});

			writer.close();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		//////////// EDIT: /////////////////////////////////////////////////////////////
		int lastIteration = services.getConfig().controler().getLastIteration();
		int numberOfIteration = services.getIterationNumber();

		if( numberOfIteration == lastIteration ) {
			DefaultTableXYDataset testDataSet = new DefaultTableXYDataset();
			for (Map.Entry<DrtZone, LinkedList<Tuple<Double, Integer>>> e : this.zoneEntries.entrySet()) {
				String zoneId = e.getKey().getId();
				XYSeries series = new XYSeries(zoneId, true, false);
				for (Tuple<Double, Integer> entry : e.getValue()) {
					double time = entry.getFirst();
					int numberOfIdleVehicles = entry.getSecond();
					// because item = 0, numberOfVehics = 0 gets always written out,
					// even if item = 0, numberOfVehics = 1
					if (!(time == 0.0 & numberOfIdleVehicles == 0)) {
						series.addOrUpdate(time, numberOfIdleVehicles);
					}
				}
				testDataSet.addSeries(series);
			}

			// fill out the missing time points for plot
			DefaultTableXYDataset dataSetPlot = new DefaultTableXYDataset();
			for( int i = 0; i < testDataSet.getSeriesCount(); i++) {
				String zoneId = testDataSet.getSeriesKey(i).toString();
				XYSeries xySeries = testDataSet.getSeries(i);
				XYSeries seriesForPlot = new XYSeries( zoneId, true, false );
				double lastNumberOfIdleVehicles = 0.;
				double lastTime = 0.;
				for(Object o : xySeries.getItems())   {
					XYDataItem item = (XYDataItem) o;
					double time = item.getXValue();
					double numberOfIdleVehicles = item.getYValue();
					if (Double.isNaN(numberOfIdleVehicles)) {
						for(double timeIter = lastTime; timeIter <= time; timeIter++) {
							seriesForPlot.addOrUpdate(timeIter, lastNumberOfIdleVehicles);
						}
					} else {
						for(double timeIter = lastTime; timeIter < time; timeIter++) {
							seriesForPlot.addOrUpdate(timeIter, lastNumberOfIdleVehicles);
						}
						seriesForPlot.add(time, numberOfIdleVehicles);
						lastNumberOfIdleVehicles = numberOfIdleVehicles;
					}
					lastTime = time;
				}
				dataSetPlot.addSeries( seriesForPlot );
			}

			// to do
			// * for the length of simulation in [time]
			// ** create (time, vehics = same as last, or 0 if none before)
			// ** for every zone XYset

			JFreeChart testChart = ChartFactory.createStackedXYAreaChart("Idle vehicles per zone", "Time [sec]", "count", dataSetPlot,
					PlotOrientation.VERTICAL, true, false, false);
			makeStayTaskSeriesGrey(testChart.getXYPlot());
			saveAsPNG(testChart, "testChart2", 800, 600);
		}
		//////////// EDIT: /////////////////////////////////////////////////////////////

	}

	@Override
	public void reset(int iteration) {
		initEntryMap();
	}

	////////////copied methods - to not depend on dvrp ///////////////////////////////////////////////////////
	private void makeStayTaskSeriesGrey(XYPlot plot) {
		XYDataset dataset = plot.getDataset(0);
		for (int i = 0; i < dataset.getSeriesCount(); i++) {
			plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
			return;
		}
	}
	private static void saveAsPNG(JFreeChart chart, String filename, int width, int height) {
		try {
			ChartUtils.writeChartAsPNG(new FileOutputStream(filename + ".png"), chart, width, height);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////

}
