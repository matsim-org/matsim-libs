package playground.sebhoerl.ant.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.vehicles.Vehicle;

import playground.sebhoerl.ant.DataFrame;
import playground.sebhoerl.av_paper.BinCalculator;

public class OccupancyHandler extends AbstractHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
    final private Map<Id<Vehicle>, Integer> passengerCount = new HashMap();
    final private Map<Id<Vehicle>, Double> lastStateChange = new HashMap<>();

    public OccupancyHandler(DataFrame data) {
        super(data);
    }

    private void ensureVehicle(Id<Vehicle> vehicleId) {
        if (!passengerCount.containsKey(vehicleId)) {
            passengerCount.put(vehicleId, 0);
            lastStateChange.put(vehicleId, 0.0);
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        if (!event.getVehicleId().toString().startsWith("av_")) return;

        ensureVehicle(event.getVehicleId());
        process(event.getVehicleId(), event.getTime(), +1);
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (!data.isOrdinaryPerson(event.getPersonId())) return;
        if (!event.getVehicleId().toString().startsWith("av_")) return;

        ensureVehicle(event.getVehicleId());
        process(event.getVehicleId(), event.getTime(), -1);
    }

    protected void process(Id<Vehicle> vehicleId, double time, int change) {
        int current = passengerCount.get(vehicleId);
        double lastChange = lastStateChange.get(vehicleId);
        List<Double> countBin = data.occupancy.get(current);

        for (BinCalculator.BinEntry entry : data.binCalculator.getBinEntriesNormalized(lastChange, time)) {
            countBin.set(entry.getIndex(), countBin.get(entry.getIndex()) + entry.getWeight());
        }

        passengerCount.put(vehicleId, current + change);
        lastStateChange.put(vehicleId, time);
    }

    @Override
    protected void finish() {
        for (Id<Vehicle> vehicleId : passengerCount.keySet()) {
            process(vehicleId, data.binCalculator.getEnd(), 0);
        }
    }
}