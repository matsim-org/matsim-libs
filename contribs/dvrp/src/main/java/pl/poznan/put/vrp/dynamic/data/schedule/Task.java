package pl.poznan.put.vrp.dynamic.data.schedule;

public interface Task
{
    public enum TaskType
    {
        SERVE, DRIVE, WAIT;
    }


    public enum TaskStatus
    {
        PLANNED, STARTED, PERFORMED;
    }


    public interface TaskInfo
    {}


    TaskType getType();


    TaskStatus getStatus();


    // inclusive
    int getBeginTime();


    // exclusive
    int getEndTime();


    Schedule getSchedule();


    int getTaskIdx();


    TaskInfo getInfo();


    // SETTERS:
    void setBeginTime(int beginTime);


    void setEndTime(int endTime);


    void setInfo(TaskInfo info);
}
