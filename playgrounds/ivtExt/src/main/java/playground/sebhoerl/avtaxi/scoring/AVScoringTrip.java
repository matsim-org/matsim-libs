package playground.sebhoerl.avtaxi.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.schedule.AVTransitEvent;

public class AVScoringTrip {
    public enum Stage {
        NONE, WAITING, TRANSIT, FINISHED
    }

    private Stage stage = Stage.NONE;

    private Id<AVOperator> operatorId = null;
    private double distance = Double.NaN;

    private double departureTime = Double.NaN;
    private double inVehicleTravelTime = Double.NaN;
    private double waitingTime = Double.NaN;

    public void processDeparture(PersonDepartureEvent event) {
        if (!stage.equals(Stage.NONE)) throw new IllegalStateException();

        departureTime = event.getTime();
        stage = Stage.WAITING;
    }

    public void processEnterVehicle(PersonEntersVehicleEvent event) {
        if (!stage.equals(Stage.WAITING)) throw new IllegalStateException();

        waitingTime = event.getTime() - departureTime;
        stage = Stage.TRANSIT;
    }

    public void processTransit(AVTransitEvent event) {
        if (!stage.equals(Stage.TRANSIT)) throw new IllegalStateException();

        inVehicleTravelTime = event.getTime() - departureTime - waitingTime;
        distance = event.getRequest().getRoute().getDistance();
        operatorId = event.getRequest().getOperator().getId();

        stage = Stage.FINISHED;
    }

    public Id<AVOperator> getOperatorId() {
        if (!stage.equals(Stage.FINISHED)) throw new IllegalStateException();
        return operatorId;
    }

    public double getDistance() {
        if (!stage.equals(Stage.FINISHED)) throw new IllegalStateException();
        return distance;
    }

    public double getWaitingTime() {
        if (!stage.equals(Stage.FINISHED)) throw new IllegalStateException();
        return waitingTime;
    }

    public double getInVehicleTravelTime() {
        if (!stage.equals(Stage.FINISHED)) throw new IllegalStateException();
        return inVehicleTravelTime;
    }

    public double getTotalTravelTime() {
        if (!stage.equals(Stage.FINISHED)) throw new IllegalStateException();
        return inVehicleTravelTime + waitingTime;
    }

    public boolean isFinished() {
        return stage.equals(Stage.FINISHED);
    }
}
