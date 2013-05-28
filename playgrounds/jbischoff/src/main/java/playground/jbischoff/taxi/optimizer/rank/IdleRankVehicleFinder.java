package playground.jbischoff.taxi.optimizer.rank;

import java.util.Collections;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.optimizer.taxi.TaxiUtils;


public class IdleRankVehicleFinder
{
    private final VrpData data;
    private final boolean straightLineDistance;


    public IdleRankVehicleFinder(VrpData data, boolean straightLineDistance)
    {
        this.data = data;
        this.straightLineDistance = straightLineDistance;
    }


    public Vehicle findClosestVehicle(Request req)
    {
        Vehicle bestVeh = null;
        double bestDistance = Double.MAX_VALUE;
        Collections.shuffle(data.getVehicles());
        for (Vehicle veh : data.getVehicles()) {
            double distance = calculateDistance(req, veh);

            if (distance < bestDistance) {
                bestVeh = veh;
                bestDistance = distance;
            }
        }

        return bestVeh;
    }


    private double calculateDistance(Request req, Vehicle veh)
    {
        Schedule sched = veh.getSchedule();
        int time = data.getTime();
        Vertex departVertex;

        if (!TaxiUtils.isIdle(veh, time, true)) {
            return Double.MAX_VALUE;
        }

        Task currentTask = sched.getCurrentTask();

        switch (currentTask.getType()) {
            case WAIT:
                departVertex = ((WaitTask)currentTask).getAtVertex();
                break;

            case DRIVE:// only CRUISE possible
                throw new IllegalStateException();// currently, no support for vehicle diversion

            default:
                throw new IllegalStateException();
        }

        return distance(departVertex, req.getFromVertex(), time);
    }


    private double distance(Vertex fromVertex, Vertex toVertex, int departTime)
    {
        if (straightLineDistance) {
            double deltaX = toVertex.getX() - fromVertex.getX();
            double deltaY = toVertex.getY() - fromVertex.getY();

            // this is a SQUARED distance!!! (to avoid unnecessary Math.sqrt() call)
            return deltaX * deltaX + deltaY * deltaY;
        }
        else {
            return data.getVrpGraph().getArc(fromVertex, toVertex).getCostOnDeparture(departTime);
        }
    }
}
