package playground.jbischoff.taxi.optimizer;


import java.util.List;

import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Arc;


interface BestVehicleFinder
{
    interface BestVehicle
    {
        /**
         * @return null} if no vehicle meets requirements
         */
        Vehicle getVehicle();


        int getDepartureTime();


        int getArrivalTime();


        Arc getArc();
    }


    BestVehicle findBestVehicle(Request req, List<Vehicle> vehicles);
}
