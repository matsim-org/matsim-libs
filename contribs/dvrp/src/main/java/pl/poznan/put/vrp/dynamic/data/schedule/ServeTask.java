package pl.poznan.put.vrp.dynamic.data.schedule;

import pl.poznan.put.vrp.dynamic.data.model.Request;


public interface ServeTask
    extends StayTask
{
    Request getRequest();
}
