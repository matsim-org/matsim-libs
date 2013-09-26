package pl.poznan.put.vrp.dynamic.data.schedule;

public interface ScheduleListener
{
    void taskAdded(Task task);
    
    /**
     * As a result of Schedule.nextTask()
     * @param task
     */
    void currentTaskChanged(Schedule schedule);
}