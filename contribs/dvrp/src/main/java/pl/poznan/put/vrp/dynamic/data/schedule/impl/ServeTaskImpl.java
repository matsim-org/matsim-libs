package pl.poznan.put.vrp.dynamic.data.schedule.impl;

import pl.poznan.put.vrp.dynamic.data.model.Request;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.ServeTask;


public class ServeTaskImpl
    extends AbstractTask
    implements ServeTask
{
    private final Vertex atVertex;
    private final Request request;


    public ServeTaskImpl(int beginTime, int endTime, Vertex atVertex, Request request)
    {
        super(beginTime, endTime);
        this.atVertex = atVertex;
        this.request = request;
    }


    @Override
    protected void notifyAdded()
    {
        request.notifyScheduled(this);
    }


    @Override
    protected void notifyRemoved()
    {
        request.notifyUnscheduled();
    }


    @Override
    public Request getRequest()
    {
        return request;
    }


    @Override
    public Vertex getAtVertex()
    {
        return atVertex;
    }


    @Override
    public TaskType getType()
    {
        return TaskType.SERVE;
    }


    @Override
    public String toString()
    {
        return "S(R_" + request.getId() + ",@" + atVertex.getId() + ")" + commonToString();
    }
}