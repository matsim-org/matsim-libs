package playground.sebhoerl.avtaxi.data;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.VehicleImpl;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;

public class AVVehicle extends VehicleImpl {
    private AVOperator operator;
    private AVDispatcher dispatcher;

    public AVVehicle(Id<Vehicle> id, Link startLink, double capacity, double t0, double t1, AVOperator operator) {
        super(id, startLink, capacity, t0, t1);
        this.operator = operator;
    }

    public AVVehicle(Id<Vehicle> id, Link startLink, double capacity, double t0, double t1) {
        this(id, startLink, capacity, t0, t1, null);
    }

    public AVOperator getOperator() {
        return operator;
    }
    public AVDispatcher getDispatcher() { return dispatcher; }

    public void setOpeartor(AVOperator operator) {
        this.operator = operator;
    }
    public void setDispatcher(AVDispatcher dispatcher) { this.dispatcher = dispatcher; }
}
