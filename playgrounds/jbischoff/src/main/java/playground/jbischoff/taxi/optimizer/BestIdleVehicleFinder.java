package playground.jbischoff.taxi.optimizer;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.Vertex;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Task.TaskType;


public class BestIdleVehicleFinder
{
    private VrpData data;
    private boolean straightLineDistance;


    public BestIdleVehicleFinder(VrpData data, boolean straightLineDistance)
    {
        this.data = data;
        this.straightLineDistance = straightLineDistance;
    }


    public Vehicle findBestVehicle(Request req)
    {
        Vehicle bestVeh = null;
        double bestDistance = Double.MAX_VALUE;

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

        switch (sched.getStatus()) {
            case COMPLETED:
                return Double.MAX_VALUE;

            case UNPLANNED:
                if (veh.getT0() > time) {
                    return Double.MAX_VALUE;
                }

                departVertex = veh.getDepot().getVertex();
                break;

            case PLANNED:
                if (Schedules.getFirstTask(sched).getBeginTime() == time) {
                    // a request has been added to this schedule just before
                    // (i.e. in this time step, while handling TaxiModeDepart)
                    // therefore PLANNED is to be changed into STARTED
                    // (at this time step: its beginTime == time)
                    return Double.MAX_VALUE;
                }
                else {
                    // definitely should not happen
                    throw new IllegalStateException();
                }

            case STARTED:
                Task currentTask = sched.getCurrentTask();

                if (currentTask.getType() != TaskType.WAIT) {
                    return Double.MAX_VALUE;
                }

                // WAIT may be current and not the last
                if (currentTask != Schedules.getLastTask(sched)) {
                    if (currentTask.getEndTime() <= time) {
                        // a request has been added to this schedule just before
                        // (i.e. in this time step, while handling TaxiModeDepart)
                        // therefore WAIT is to be ended
                        // (at this time step: its endTime == time)
                        return Double.MAX_VALUE;
                    }
                    else {
                    	System.out.println("this is evil agent number" +veh.getId());
                    			System.out.println(currentTask);
                    			System.out.println(Schedules.getLastTask(sched));
                    			System.out.println(Schedules.getNextTask(currentTask));
                    			System.out.println(currentTask.getEndTime() +" vs "+ time);
                    			
                        // definitely should not happen 
                        throw new IllegalStateException();
                    }
                }

                departVertex = ((WaitTask)currentTask).getAtVertex();
                break;
                
            default:
                throw new IllegalStateException();
        }

        // TODO!!!
        // consider CLOSING (T1) time windows of the vehicle
        if (time > Schedules.getActualT1(sched)) {
            return Double.MAX_VALUE;
        }

        return distance(departVertex, req.getFromVertex(), time);
    }


    private double distance(Vertex fromVertex, Vertex toVertex, int departTime)
    {
        if (straightLineDistance) {
            double deltaX = toVertex.getX() - fromVertex.getX();
            double deltaY = toVertex.getY() - fromVertex.getY();

            // this is a SQUARED distance!!! (to avoid uncecessary Math.sqrt() call)
            return deltaX * deltaX + deltaY * deltaY;
        }
        else {
            return data.getVrpGraph().getArc(fromVertex, toVertex).getCostOnDeparture(departTime);
        }
    }
}
