/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.util.chart;

import java.awt.Color;
import java.awt.Paint;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.gantt.TaskSeriesCollection;
import org.jfree.data.gantt.XYTaskDataset;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.xy.XYDataset;
import org.matsim.contrib.dvrp.data.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;

public class ScheduleCharts {
	public static JFreeChart chartSchedule(List<? extends DvrpVehicle> vehicles) {
		return chartSchedule(vehicles, BASIC_DESCRIPTION_CREATOR, BASIC_PAINT_SELECTOR);
	}

	public static JFreeChart chartSchedule(Collection<? extends DvrpVehicle> vehicles,
			DescriptionCreator descriptionCreator, PaintSelector paintSelector) {
		// data
		TaskSeriesCollection dataset = createScheduleDataset(vehicles, descriptionCreator);
		XYTaskDataset xyTaskDataset = new XYTaskDataset(dataset);

		// chart
		JFreeChart chart = ChartFactory.createXYBarChart("Schedules", "Time", false, "Vehicles", xyTaskDataset,
				PlotOrientation.HORIZONTAL, false, true, false);
		XYPlot plot = (XYPlot)chart.getPlot();

		// Y axis
		String[] series = new String[vehicles.size()];
		int i = 0;
		for (DvrpVehicle v : vehicles) {
			series[i++] = v.getId().toString();
		}

		SymbolAxis symbolAxis = new SymbolAxis("Vehicles", series);
		symbolAxis.setGridBandsVisible(false);
		plot.setDomainAxis(symbolAxis);

		// X axis
		plot.setRangeAxis(new DateAxis("Time", TimeZone.getTimeZone("GMT"), Locale.getDefault()));

		// Renderer
		XYBarRenderer xyBarRenderer = new ChartTaskRenderer(dataset, paintSelector);
		xyBarRenderer.setUseYInterval(true);
		plot.setRenderer(xyBarRenderer);

		return chart;
	}

	@SuppressWarnings("serial")
	private static class ChartTask extends org.jfree.data.gantt.Task {
		private Task vrpTask;

		private ChartTask(String description, TimePeriod duration, Task vrpTask) {
			super(description, duration);
			this.vrpTask = vrpTask;
		}
	}

	@SuppressWarnings("serial")
	private static class ChartTaskRenderer extends XYBarRenderer {
		private final TaskSeriesCollection tsc;
		private final PaintSelector paintSelector;

		public ChartTaskRenderer(final TaskSeriesCollection tsc, PaintSelector paintSelector) {
			this.tsc = tsc;
			this.paintSelector = paintSelector;

			setBarPainter(new StandardXYBarPainter());
			setShadowVisible(false);
			setDrawBarOutline(true);

			setBaseToolTipGenerator(new XYToolTipGenerator() {
				@Override
				public String generateToolTip(XYDataset dataset, int series, int item) {
					return getTask(series, item).getDescription();
				}
			});
		}

		@Override
		public Paint getItemPaint(int row, int column) {
			return paintSelector.select(getTask(row, column).vrpTask);
		}

		private ChartTask getTask(int series, int item) {
			ChartTask chartTask = (ChartTask)tsc.getSeries(series).get(item);
			return chartTask;
		}

	}

	public static interface PaintSelector {
		Paint select(Task task);
	}

	private static final Color WAIT_COLOR = new Color(0, 200, 0);
	private static final Color DRIVE_COLOR = new Color(200, 0, 0);

	public static final PaintSelector BASIC_PAINT_SELECTOR = task -> {
		if (task instanceof DriveTask) {
			return DRIVE_COLOR;
		} else if (task instanceof StayTask) {
			return WAIT_COLOR;
		}
		throw new IllegalStateException();
	};

	public static interface DescriptionCreator {
		String create(Task task);
	}

	public static final DescriptionCreator BASIC_DESCRIPTION_CREATOR = task -> {
		if (task instanceof StayTask) {
			return StayTask.class.toString();
		} else if (task instanceof DriveTask) {
			return DriveTask.class.toString();
		}
		throw new RuntimeException("not implemented");
	};

	private static TaskSeriesCollection createScheduleDataset(Collection<? extends DvrpVehicle> vehicles,
			DescriptionCreator descriptionCreator) {
		TaskSeriesCollection collection = new TaskSeriesCollection();

		for (DvrpVehicle v : vehicles) {
			Schedule schedule = v.getSchedule();

			final TaskSeries scheduleTaskSeries = new TaskSeries(v.getId().toString());

			if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {
				collection.add(scheduleTaskSeries);
				continue;
			}

			for (Task t : schedule.getTasks()) {
				String description = descriptionCreator.create(t);

				TimePeriod duration = new SimpleTimePeriod(//
						new Date((int)Math.floor(t.getBeginTime() * 1000)), //
						new Date((int)Math.ceil(t.getEndTime() * 1000)));

				scheduleTaskSeries.add(new ChartTask(description, duration, t));
			}

			collection.add(scheduleTaskSeries);
		}

		return collection;
	}
}
