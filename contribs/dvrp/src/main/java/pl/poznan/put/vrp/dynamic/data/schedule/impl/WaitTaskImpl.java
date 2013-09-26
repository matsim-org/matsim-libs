package pl.poznan.put.vrp.dynamic.data.schedule.impl;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.WaitTask;


public class WaitTaskImpl
    extends AbstractTask
    implements WaitTask
{
    private final Vertex atVertex;


    public WaitTaskImpl(int beginTime, int endTime, Vertex atVertex)
    {
        super(beginTime, endTime);
        this.atVertex = atVertex;
    }


    @Override
    public Vertex getAtVertex()
    {
        return atVertex;
    }


    @Override
    public TaskType getType()
    {
        return TaskType.WAIT;
    }


    @Override
    public String toString()
    {
        return "W(@" + atVertex.getId() + ")" + commonToString();
    }
}