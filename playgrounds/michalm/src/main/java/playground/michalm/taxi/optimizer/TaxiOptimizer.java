package playground.michalm.taxi.optimizer;

import pl.poznan.put.vrp.dynamic.data.model.*;


public interface TaxiOptimizer
{
    void init();


    void taxiRequestSubmitted(Request request);


    void nextTask(Vehicle vehicle);
}
