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

import java.awt.*;
import java.util.*;
import java.util.List;

import org.jfree.chart.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;
import org.matsim.contrib.util.chart.CoordDataset;
import org.matsim.contrib.util.chart.CoordDataset.CoordSource;


/**
 * @author michalm
 */
public class RouteChartUtils
{
    public static JFreeChart chartRoutes(List<? extends Vehicle> vehicles)
    {
        CoordDataset lData = new CoordDataset();

        for (int i = 0; i < vehicles.size(); i++) {
            Schedule<?> schedule = vehicles.get(i).getSchedule();
            lData.addSeries(Integer.toString(i), ScheduleCoordSources.createCoordSource(schedule));
        }

        JFreeChart chart = ChartFactory.createXYLineChart("Routes", "X", "Y", lData,
                PlotOrientation.VERTICAL, true, true, false);

        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Color.white);

        NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
        yAxis.setAutoRangeIncludesZero(false);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesItemLabelsVisible(0, true);

        renderer.setBaseItemLabelGenerator(new XYItemLabelGenerator() {
            public String generateLabel(XYDataset dataset, int series, int item)
            {
                return ((CoordDataset)dataset).getText(series, item);
            }
        });

        for (int i = 1; i <= vehicles.size(); i++) {
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesItemLabelsVisible(i, true);
        }

        return chart;
    }


    public static JFreeChart chartRoutesByStatus(List<? extends Vehicle> vehicles)
    {
        CoordDataset nData = new CoordDataset();

        for (int i = 0; i < vehicles.size(); i++) {
            Schedule<?> schedule = vehicles.get(i).getSchedule();
            Map<TaskStatus, CoordSource> vsByStatus = createLinkSourceByStatus(schedule);
            nData.addSeries(i + "-PR", vsByStatus.get(TaskStatus.PERFORMED));
            nData.addSeries(i + "-ST", vsByStatus.get(TaskStatus.STARTED));
            nData.addSeries(i + "-PL", vsByStatus.get(TaskStatus.PLANNED));
        }

        JFreeChart chart = ChartFactory.createXYLineChart("Routes", "X", "Y", nData,
                PlotOrientation.VERTICAL, false, true, false);

        XYPlot plot = (XYPlot)chart.getPlot();
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Color.white);

        NumberAxis yAxis = (NumberAxis)plot.getRangeAxis();
        yAxis.setAutoRangeIncludesZero(false);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer)plot.getRenderer();
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesLinesVisible(0, false);
        renderer.setSeriesItemLabelsVisible(0, true);

        renderer.setBaseItemLabelGenerator(new LabelGenerator());

        Paint[] paints = DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE;
        Shape[] shapes = DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE;

        for (int i = 0; i < vehicles.size(); i++) {
            int s = 3 * i;

            renderer.setSeriesItemLabelsVisible(s + 1, true);
            renderer.setSeriesItemLabelsVisible(s + 2, true);
            renderer.setSeriesItemLabelsVisible(s + 3, true);

            renderer.setSeriesShapesVisible(s + 1, true);
            renderer.setSeriesShapesVisible(s + 2, true);
            renderer.setSeriesShapesVisible(s + 3, true);

            renderer.setSeriesLinesVisible(s + 1, true);
            renderer.setSeriesLinesVisible(s + 2, true);
            renderer.setSeriesLinesVisible(s + 3, true);

            renderer.setSeriesPaint(s + 1, paints[ (i + 1) % paints.length]);
            renderer.setSeriesPaint(s + 2, paints[ (i + 1) % paints.length]);
            renderer.setSeriesPaint(s + 3, paints[ (i + 1) % paints.length]);

            renderer.setSeriesShape(s + 1, shapes[ (i + 1) % shapes.length]);
            renderer.setSeriesShape(s + 2, shapes[ (i + 1) % shapes.length]);
            renderer.setSeriesShape(s + 3, shapes[ (i + 1) % shapes.length]);

            renderer.setSeriesStroke(s + 2, new BasicStroke(3));
            renderer.setSeriesStroke(s + 3, new BasicStroke(1, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 1, new float[] { 5f, 5f }, 0));
        }

        return chart;
    }


    private static Map<TaskStatus, CoordSource> createLinkSourceByStatus(
            Schedule<? extends Task> schedule)
    {
        Iterable<DriveTask> tasks = Schedules.createDriveTaskIter(schedule);

        // creating lists of DriveTasks
        Map<TaskStatus, List<DriveTask>> taskListByStatus = new EnumMap<>(TaskStatus.class);

        for (TaskStatus ts : TaskStatus.values()) {
            taskListByStatus.put(ts, new ArrayList<DriveTask>());
        }

        for (DriveTask t : tasks) {
            taskListByStatus.get(t.getStatus()).add(t);
        }

        // creating LinkSources
        Map<TaskStatus, CoordSource> linkSourceByStatus = new EnumMap<>(TaskStatus.class);

        for (TaskStatus ts : TaskStatus.values()) {
            linkSourceByStatus.put(ts, ScheduleCoordSources.createCoordSource(taskListByStatus.get(ts)));
        }

        return linkSourceByStatus;
    }


    private static class LabelGenerator
        implements XYItemLabelGenerator
    {
        public String generateLabel(XYDataset dataset, int series, int item)
        {
            if (series == 0) {
                return "D";
            }

            if (item == 0) {
                return null;
            }

            return ((CoordDataset)dataset).getText(series, item);
        }
    }
}
