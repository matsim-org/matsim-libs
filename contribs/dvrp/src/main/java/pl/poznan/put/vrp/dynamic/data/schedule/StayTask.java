package pl.poznan.put.vrp.dynamic.data.schedule;

import pl.poznan.put.vrp.dynamic.data.network.Vertex;


public interface StayTask
    extends Task
{
    Vertex getAtVertex();

}
