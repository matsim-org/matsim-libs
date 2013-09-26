package pl.poznan.put.vrp.dynamic.data.schedule;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public class Tasks
{
    public static Vertex getBeginVertex(Task task)
    {
        switch (task.getType()) {
            case DRIVE:
                return ((DriveTask)task).getArc().getFromVertex();
            case SERVE:
            case WAIT:
                return ((StayTask)task).getAtVertex();
            default:
                throw new IllegalStateException("Only: DRIVE, SERVE or WAIT");
        }
    }


    public static Vertex getEndVertex(Task task)
    {
        switch (task.getType()) {
            case DRIVE:
                return ((DriveTask)task).getArc().getToVertex();
            case SERVE:
            case WAIT:
                return ((StayTask)task).getAtVertex();
            default:
                throw new IllegalStateException("Only: DRIVE, SERVE or WAIT");
        }
    }
}
