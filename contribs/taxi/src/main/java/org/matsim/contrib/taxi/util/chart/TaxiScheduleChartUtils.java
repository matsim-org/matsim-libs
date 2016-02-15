package org.matsim.contrib.taxi.util.chart;

import java.awt.*;
import java.util.Collection;

import org.jfree.chart.JFreeChart;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.chart.ScheduleChartUtils;
import org.matsim.contrib.dvrp.util.chart.ScheduleChartUtils.*;
import org.matsim.contrib.taxi.schedule.*;


public class TaxiScheduleChartUtils
{
    public static JFreeChart chartSchedule(Collection<? extends Vehicle> vehicles)
    {
        return ScheduleChartUtils.chartSchedule(vehicles, TAXI_DESCRIPTION_CREATOR,
                TAXI_PAINT_SELECTOR);
    }


    public static final DescriptionCreator<TaxiTask> TAXI_DESCRIPTION_CREATOR = new DescriptionCreator<TaxiTask>() {
        @Override
        public String create(TaxiTask task)
        {
            return task.getTaxiTaskType().name();
        }
    };

    public static final DescriptionCreator<TaxiTask> TAXI_DESCRIPTION_WITH_PASSENGER_ID_CREATOR = new DescriptionCreator<TaxiTask>() {
        @Override
        public String create(TaxiTask task)
        {
            if (task instanceof TaxiTaskWithRequest) {
                TaxiTaskWithRequest taskWithReq = (TaxiTaskWithRequest)task;
                return task.getTaxiTaskType().name() + "_"
                        + taskWithReq.getRequest().getPassenger().getId();
            }

            return task.getTaxiTaskType().name();
        }
    };

    private static final Color DRIVE_OCCUPIED_COLOR = new Color(200, 0, 0);
    private static final Color PICKUP_DROPOFF_COLOR = new Color(0, 0, 200);

    private static final Color DRIVE_EMPTY_COLOR = new Color(100, 0, 0);
    private static final Color STAY_COLOR = new Color(0, 0, 100);

    public static final PaintSelector<TaxiTask> TAXI_PAINT_SELECTOR = new PaintSelector<TaxiTask>() {
        public Paint select(TaxiTask task)
        {
            switch (task.getTaxiTaskType()) {
                case PICKUP:
                case DROPOFF:
                    return PICKUP_DROPOFF_COLOR;

                case DRIVE_OCCUPIED:
                    return DRIVE_OCCUPIED_COLOR;

                case DRIVE_EMPTY:
                    return DRIVE_EMPTY_COLOR;

                case STAY:
                    return STAY_COLOR;

                default:
                    throw new IllegalStateException();
            }
        }
    };
}
