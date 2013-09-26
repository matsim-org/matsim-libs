package pl.poznan.put.vrp.dynamic.data.schedule;

import java.util.List;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;


public interface Schedule
{
    public enum ScheduleStatus
    {
        UNPLANNED(0), PLANNED(1), STARTED(2), COMPLETED(3);

        private final int stage;


        private ScheduleStatus(int stage)
        {
            this.stage = stage;
        }


        public boolean isUnplanned()
        {
            return this == UNPLANNED;
        }


        public boolean isPlanned()
        {
            return this == PLANNED;
        }


        public boolean isStarted()
        {
            return this == STARTED;
        }


        public boolean isCompleted()
        {
            return this == COMPLETED;
        }


        public boolean ge(ScheduleStatus other)
        {
            return this.stage >= other.stage;
        }


        public boolean gt(ScheduleStatus other)
        {
            return this.stage > other.stage;
        }


        public boolean le(ScheduleStatus other)
        {
            return this.stage <= other.stage;
        }


        public boolean lt(ScheduleStatus other)
        {
            return this.stage < other.stage;
        }


        public int compareStage(ScheduleStatus other)
        {
            return this.stage - other.stage;
        }
    };


    Vehicle getVehicle();


    List<Task> getTasks();// unmodifiableList


    int getTaskCount();


    Task getCurrentTask();


    ScheduleStatus getStatus();


    int getBeginTime();


    int getEndTime();


    // schedule modification functionality:

    void addTask(Task task);


    void addTask(int taskIdx, Task task);


    void removeAllPlannedTasks();


    void removeLastPlannedTask();


    void removePlannedTask(int taskIdx);


    Task nextTask();// sets the next task as the current one, updates this schedule status


    void addScheduleListener(ScheduleListener listener);


    void removeScheduleListener(ScheduleListener listener);
}
