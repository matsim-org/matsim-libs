package playground.michalm.taxi.chart;

import java.awt.*;
import java.util.List;

import org.jfree.chart.JFreeChart;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.util.chart.*;
import org.matsim.contrib.dvrp.util.chart.ScheduleChartUtils.DescriptionCreator;
import org.matsim.contrib.dvrp.util.chart.ScheduleChartUtils.PaintSelector;

import playground.michalm.taxi.schedule.*;


public class TaxiScheduleChartUtils
{
    public static JFreeChart chartSchedule(List<Vehicle> vehicles)
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

    private static final Color DRIVE_NON_IDLE_COLOR = new Color(200, 0, 0);
    private static final Color STAY_NON_IDLE_COLOR = new Color(0, 0, 200);

    private static final Color CRUISE_COLOR = new Color(100, 0, 0);
    private static final Color WAIT_COLOR = new Color(0, 0, 100);

    private static final Color CHARGE_COLOR = new Color(0, 200, 0);

    public static final PaintSelector<TaxiTask> TAXI_PAINT_SELECTOR = new PaintSelector<TaxiTask>() {
        public Paint select(TaxiTask task)
        {
            switch (task.getTaxiTaskType()) {
                case PICKUP_DRIVE:
                case DROPOFF_DRIVE:
                    return DRIVE_NON_IDLE_COLOR;

                case PICKUP_STAY:
                case DROPOFF_STAY:
                    return STAY_NON_IDLE_COLOR;

                case CRUISE_DRIVE:
                    return CRUISE_COLOR;

                case WAIT_STAY:
                    return WAIT_COLOR;

                case CHARGE_STAY:
                    return CHARGE_COLOR;

                default:
                    throw new IllegalStateException();
            }
        }
    };
}
