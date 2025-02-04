package org.matsim.contrib.dvrp.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.VehicleCapacityChangedEvent;
import org.matsim.contrib.dvrp.vrpagent.VehicleCapacityChangedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * This class performs analysis on the capacities and loads that are available /
 * required at any time during the simulation.
 *
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class CapacityLoadAnalysisHandler
        implements VehicleCapacityChangedEventHandler, PassengerRequestSubmittedEventHandler,
        PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler, PassengerRequestRejectedEventHandler,
        IterationStartsListener, IterationEndsListener {
    static public final String CAPACITIES_FILE = "dvrp_capacities_{mode}.csv";
    static public final String LOADS_FILE = "dvrp_loads_{mode}.csv";

    private final String mode;
    private final FleetSpecification fleetSpecification;
    private final EventsManager eventsManager;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final DvrpLoadType loadType;

    public CapacityLoadAnalysisHandler(String mode, FleetSpecification fleetSpecification,
            OutputDirectoryHierarchy outputHierarchy,
            EventsManager eventsManager, int analysisInterval, DvrpLoadType loadType) {
        this.outputHierarchy = outputHierarchy;
        this.mode = mode;
        this.fleetSpecification = fleetSpecification;
        this.eventsManager = eventsManager;
        this.analysisInterval = analysisInterval;
        this.loadType = loadType;
    }

    private class CapacityRecord {
        final Id<DvrpVehicle> vehicleId;
        final DvrpLoad load;
        final double startTime;
        double endTime = Double.POSITIVE_INFINITY;

        CapacityRecord(Id<DvrpVehicle> vehicleId, DvrpLoad load, double startTime) {
            this.vehicleId = vehicleId;
            this.load = load;
            this.startTime = startTime;
        }
    }

    private final List<CapacityRecord> capacityRecords = new LinkedList<>();
    private final IdMap<DvrpVehicle, CapacityRecord> activeCapacities = new IdMap<>(DvrpVehicle.class);

    @Override
    public void handleEvent(VehicleCapacityChangedEvent event) {
        if (!this.fleetSpecification.getVehicleSpecifications().containsKey(event.getVehicleId())) {
            return;
        }
        
        CapacityRecord current = activeCapacities.get(event.getVehicleId());
        final double startTime;
        if (current != null) {
            current.endTime = event.getTime();
            startTime = event.getTime();
        } else {
            startTime = Double.NEGATIVE_INFINITY;
        }

        CapacityRecord next = new CapacityRecord(event.getVehicleId(), event.getNewVehicleCapacity(), startTime);
        capacityRecords.add(next);
        activeCapacities.put(event.getVehicleId(), next);
    }

    private class LoadRecord {
        final Id<Request> requestId;
        final DvrpLoad load;

        Id<DvrpVehicle> vehicleId;
        double pickupTime = Double.NaN;
        double dropoffTime = Double.NaN;

        LoadRecord(Id<Request> requestId, DvrpLoad load, double pickupTime) {
            this.requestId = requestId;
            this.load = load;
            this.pickupTime = pickupTime;
        }
    }

    private final List<LoadRecord> loadRecords = new LinkedList<>();
    private final IdMap<Request, LoadRecord> activeLoads = new IdMap<>(Request.class);

    @Override
    public void handleEvent(PassengerRequestSubmittedEvent event) {
        if (event.getMode().equals(mode)) {
            LoadRecord record = new LoadRecord(event.getRequestId(), event.getLoad(), event.getTime());
            activeLoads.put(event.getRequestId(), record);
        }
    }

    @Override
    public void handleEvent(PassengerRequestRejectedEvent event) {
        if (event.getMode().equals(mode)) {
            activeLoads.remove(event.getRequestId());
        }
    }

    @Override
    public void handleEvent(PassengerPickedUpEvent event) {
        if (event.getMode().equals(mode)) {
            LoadRecord record = Objects.requireNonNull(activeLoads.get(event.getRequestId()));
            record.pickupTime = event.getTime();
            record.vehicleId = event.getVehicleId();
            loadRecords.add(record);
        }
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent event) {
        if (event.getMode().equals(mode)) {
            LoadRecord record = Objects.requireNonNull(activeLoads.remove(event.getRequestId()));
            record.dropoffTime = event.getTime();
        }
    }

    private void writeCapacities(BufferedWriter writer) throws IOException {
        writer.write(String.join(";", new String[] { //
                "vehicle_id", "start_time", "end_time", "slot", "capacity"
        }) + "\n");

        for (CapacityRecord record : capacityRecords) {
            List<String> names = loadType.getDimensions();

            for (int k = 0; k < names.size(); k++) {
                writer.write(String.join(";", new String[] { //
                        record.vehicleId.toString(), //
                        String.valueOf(record.startTime), //
                        String.valueOf(record.endTime), //
                        names.get(k), //
                        String.valueOf(record.load.getElement(k)) //
                }) + "\n");
            }
        }
    }

    private void writeLoads(BufferedWriter writer) throws IOException {
        writer.write(String.join(";", new String[] { //
                "vehicle_id", "request_id", "pickup_time", "dropoff_time", "slot", "load"
        }) + "\n");

        for (LoadRecord record : loadRecords) {
            List<String> names = loadType.getDimensions();

            for (int k = 0; k < names.size(); k++) {
                writer.write(String.join(";", new String[] { //
                        String.valueOf(record.vehicleId), // may be zero when teleported
                        record.requestId.toString(), //
                        String.valueOf(record.pickupTime), //
                        String.valueOf(record.dropoffTime), //
                        names.get(k), //
                        String.valueOf(record.load.getElement(k)) //
                }) + "\n");
            }
        }
    }

    private final int analysisInterval;
    private boolean isActive = false;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        isActive = analysisInterval > 0 && (event.getIteration() % analysisInterval == 0 || event.isLastIteration());

        if (isActive) {
            eventsManager.addHandler(this);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (isActive) {
            eventsManager.removeHandler(this);

            try {
                BufferedWriter writer = IOUtils
                        .getBufferedWriter(outputHierarchy.getIterationFilename(event.getIteration(),
                                CAPACITIES_FILE.replace("{mode}", mode)));
                writeCapacities(writer);
                writer.close();

                writer = IOUtils.getBufferedWriter(
                        outputHierarchy.getIterationFilename(event.getIteration(), LOADS_FILE.replace("{mode}", mode)));
                writeLoads(writer);
                writer.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            capacityRecords.clear();
            activeCapacities.clear();

            loadRecords.clear();
            activeLoads.clear();
        }
    }
}
