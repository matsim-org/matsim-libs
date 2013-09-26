package pl.poznan.put.vrp.dynamic.data.schedule.impl;

import pl.poznan.put.vrp.dynamic.data.schedule.*;


public abstract class AbstractTask
    implements Task
{
    // ==== BEGIN: fields managed by ScheduleImpl
    /*package*/Schedule schedule;
    /*package*/int taskIdx;

    /*package*/int beginTime;
    /*package*/int endTime;

    /*package*/TaskStatus status;
    // ==== END: fields managed by ScheduleImpl

    private TaskInfo info;


    public AbstractTask(int beginTime, int endTime)
    {
        this.beginTime = beginTime;
        this.endTime = endTime;
    }


    protected void notifyAdded()
    {}


    protected void notifyRemoved()
    {}


    @Override
    public final TaskStatus getStatus()
    {
        return status;
    }


    @Override
    public final int getTaskIdx()
    {
        return taskIdx;
    }


    @Override
    public final Schedule getSchedule()
    {
        return schedule;
    }


    @Override
    public final int getBeginTime()
    {
        return beginTime;
    }


    @Override
    public final int getEndTime()
    {
        return endTime;
    }


    @Override
    public final TaskInfo getInfo()
    {
        return info;
    }


    @Override
    public void setBeginTime(int beginTime)
    {
        if (status != TaskStatus.PLANNED) { // PERFORMED or STARTED
            throw new IllegalStateException("Allowed only for PLANNED tasks");
        }

        this.beginTime = beginTime;
    }


    @Override
    public void setEndTime(int endTime)
    {
        if (status != TaskStatus.PLANNED && status != TaskStatus.STARTED) { // PERFORMED
            throw new IllegalStateException("Allowed only for PLANNED and STARTED tasks");
        }

        this.endTime = endTime;
    }


    @Override
    public void setInfo(TaskInfo info)
    {
        this.info = info;
    }


    protected String commonToString()
    {
        return (info != null ? " " + info.toString() : "")//
                + " [" + beginTime + " : " + endTime + "]";
    }
}