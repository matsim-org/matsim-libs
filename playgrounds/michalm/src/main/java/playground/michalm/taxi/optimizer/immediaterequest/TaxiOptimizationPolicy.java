package playground.michalm.taxi.optimizer.immediaterequest;

import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;
import playground.michalm.taxi.optimizer.schedule.*;
import playground.michalm.taxi.optimizer.schedule.TaxiDriveTask.TaxiDriveType;


public interface TaxiOptimizationPolicy
{
    // it is called directly before switching to the next task
    boolean shouldOptimize(Task completedTask);


    static final TaxiOptimizationPolicy ALWAYS = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(Task completedTask)
        {
            return true;
        }
    };

    static final TaxiOptimizationPolicy AFTER_DRIVE_TASKS = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(Task completedTask)
        {
            return (completedTask.getType() == TaskType.DRIVE);
        }
    };

    static final TaxiOptimizationPolicy AFTER_REQUEST = new TaxiOptimizationPolicy() {
        public boolean shouldOptimize(Task completedTask)
        {
            switch (completedTask.getType()) {
                case DRIVE:
                    return ((TaxiDriveTask)completedTask).getDriveType() == TaxiDriveType.DELIVERY;

                case SERVE:
                case WAIT:
                    return false;

                default:
                    throw new IllegalStateException();
            }
        }
    };
}
