/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.jbischoff.taxi.evaluation;

import java.awt.*;
import java.util.*;
import java.util.List;

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.gantt.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;

import pl.poznan.put.util.lang.TimeUtils;
import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import pl.poznan.put.vrp.dynamic.data.schedule.Task;
/**
 * 
 * 
 * 
 * @author jbischoff
 *
 */

public class ScheduleChartUtils
{
    public static JFreeChart chartSchedule(VrpData data)
    {
        // data
        TaskSeriesCollection dataset = createScheduleDataset(data);
        XYTaskDataset xyTaskDataset = new XYTaskDataset(dataset);

        // chart
        String title = "Schedules at: " + TimeUtils.secondsToStringWithDays(data.getTime());
        JFreeChart chart = ChartFactory.createXYBarChart(title, "Time", false, "Vehicles",
                xyTaskDataset, PlotOrientation.HORIZONTAL, false, true, false);
        XYPlot plot = (XYPlot)chart.getPlot();

        // Y axis
        List<Vehicle> vehicles = data.getVehicles();
        String[] series = new String[vehicles.size()];
        for (int i = 0; i < series.length; i++) {
            series[i] = vehicles.get(i).getName();
        }

        SymbolAxis symbolAxis = new SymbolAxis("Vehicles", series);
        symbolAxis.setGridBandsVisible(false);
        plot.setDomainAxis(symbolAxis);

        // X axis
        plot.setRangeAxis(new DateAxis("Time", TimeZone.getTimeZone("GMT"), Locale.getDefault()));

        // Renderer
        XYBarRenderer xyBarRenderer = new ChartTaskRenderer(dataset);
        xyBarRenderer.setUseYInterval(true);
        plot.setRenderer(xyBarRenderer);

        return chart;
    }


    @SuppressWarnings("serial")
    private static class ChartTask
        extends org.jfree.data.gantt.Task
    {
        private Task vrpTask;


        private ChartTask(String description, TimePeriod duration, Task vrpTask)
        {
            super(description, duration);
            this.vrpTask = vrpTask;
        }
    }


    @SuppressWarnings("serial")
    private static class ChartTaskRenderer
        extends XYBarRenderer
    {
        private final TaskSeriesCollection tsc;

        private static final Color DARK_BLUE = new Color(0, 0, 200);
        private static final Color DARK_RED = new Color(200, 0, 0);


        public ChartTaskRenderer(final TaskSeriesCollection tsc)
        {
            this.tsc = tsc;
            setBarPainter(new StandardXYBarPainter());
            setShadowVisible(false);
            setDrawBarOutline(true);

            setBaseToolTipGenerator(new XYToolTipGenerator() {
                public String generateToolTip(XYDataset dataset, int series, int item)
                {
                    return getTask(series, item).getDescription();
                }
            });
        }


        @Override
        public Paint getItemPaint(int row, int column)
        {
            ChartTask t = getTask(row, column);

            ChartTask tt;
            TaskType lastType = TaskType.WAIT;
            if (column > 0){
            	tt = getTask(row,column-1);
            	 lastType = tt.vrpTask.getType();
            }
            
            switch (t.vrpTask.getType()) {
                case WAIT:
                    return Color.DARK_GRAY;
                case DRIVE:
                	if (column > 0){
                		if (lastType.equals(t.vrpTask.getType())){
                			return Color.MAGENTA;
                		}
                		else return DARK_BLUE;
                		
                	}
                	else  return DARK_BLUE;
                case SERVE:
                    if ( ((ServeTask)t.vrpTask).getRequest().getFixedVehicle()) {
                        return Color.ORANGE;
                    }
                    
                    return DARK_RED;
                default:
                    throw new IllegalStateException("only 3 task types are supported");
                    // return null;
                    // Color.BLACK;
            }
        }


        private ChartTask getTask(int series, int item)
        {
            return (ChartTask)tsc.getSeries(series).get(item);
        }

    }


    private static TaskSeriesCollection createScheduleDataset(VrpData data)
    {
        TaskSeriesCollection collection = new TaskSeriesCollection();

        for (Vehicle v : data.getVehicles()) {
            Schedule schedule = v.getSchedule();

            final TaskSeries scheduleTaskSeries = new TaskSeries(v.getName());

            if (schedule.getStatus().isUnplanned()) {
                collection.add(scheduleTaskSeries);
                continue;
            }

            List<Task> tasks = schedule.getTasks();

            for (Task t : tasks) {
                String description = t.getType().name();

                if (t.getType() == TaskType.SERVE) {
                    description += ": " + ((ServeTask)t).getRequest().toString();
                }

                TimePeriod duration = new SimpleTimePeriod(new Date(t.getBeginTime() * 1000),
                        new Date(t.getEndTime() * 1000));

                scheduleTaskSeries.add(new ChartTask(description, duration, t));
            }

            collection.add(scheduleTaskSeries);
        }

        return collection;
    }
}
