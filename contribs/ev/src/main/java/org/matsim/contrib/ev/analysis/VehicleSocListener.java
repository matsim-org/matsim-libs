package org.matsim.contrib.ev.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.EnergyChargedEvent;
import org.matsim.contrib.ev.charging.EnergyChargedEventHandler;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.DrivingEnergyConsumptionEventHandler;
import org.matsim.contrib.ev.discharging.IdlingEnergyConsumptionEvent;
import org.matsim.contrib.ev.discharging.IdlingEnergyConsumptionEventHandler;
import org.matsim.contrib.ev.fleet.ElectricFleetSpecification;
import org.matsim.contrib.ev.fleet.ElectricVehicleSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import com.google.common.io.Files;
import com.google.common.util.concurrent.AtomicDouble;

public class VehicleSocListener implements EnergyChargedEventHandler,
        IdlingEnergyConsumptionEventHandler, DrivingEnergyConsumptionEventHandler,
        IterationStartsListener,
        IterationEndsListener, ShutdownListener, MobsimInitializedListener, MobsimAfterSimStepListener {
    static public final String OUTPUT_FILE = "ev_socs.csv";

    private final CompressionType compressionType;
    private final EventsManager eventsManager;
    private final OutputDirectoryHierarchy outputHierarchy;

    private final ElectricFleetSpecification electricFleet;

    private record VehicleState(AtomicDouble state, double capacity) {
    }

    private final IdMap<Vehicle, VehicleState> states = new IdMap<>(Vehicle.class);

    private BufferedWriter writer;
    private final int writeInterval;
    private final int trackingInterval;

    public VehicleSocListener(EventsManager eventsManager,
            ElectricFleetSpecification electricFleet,
            OutputDirectoryHierarchy outputHierarchy, int writeInterval, int trackingInterval,
            CompressionType compressionType) {
        this.compressionType = compressionType;
        this.eventsManager = eventsManager;
        this.electricFleet = electricFleet;
        this.writeInterval = writeInterval;
        this.trackingInterval = trackingInterval;
        this.outputHierarchy = outputHierarchy;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (writeInterval > 0 && (event.getIteration() % writeInterval == 0 || event.isLastIteration())) {
            states.clear();

            String outputPath = outputHierarchy.getIterationFilename(event.getIteration(),
                    OUTPUT_FILE, compressionType);

            writer = IOUtils.getBufferedWriter(outputPath);

            try {
                writer.write(String.join(";", new String[] {
                        "vehicle_id", //
                        "time", //
                        "charge_kWh", //
                        "soc" //
                }) + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            eventsManager.addHandler(this);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (writer != null) {
            writeTimestep(Double.POSITIVE_INFINITY);

            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            writer = null;
            eventsManager.removeHandler(this);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        File iterationPath = new File(
                outputHierarchy.getIterationFilename(event.getIteration(), OUTPUT_FILE, compressionType));
        File outputPath = new File(outputHierarchy.getOutputFilename(OUTPUT_FILE, compressionType));

        try {
            Files.copy(iterationPath, outputPath);
        } catch (IOException e) {
        }
    }

    @Override
    public void handleEvent(DrivingEnergyConsumptionEvent event) {
        states.computeIfPresent(event.getVehicleId(), (id, state) -> {
            state.state.set(event.getEndCharge());
            return state;
        });
    }

    @Override
    public void handleEvent(IdlingEnergyConsumptionEvent event) {
        states.computeIfPresent(event.getVehicleId(), (id, state) -> {
            state.state.set(event.getEndCharge());
            return state;
        });
    }

    @Override
    public void handleEvent(EnergyChargedEvent event) {
        states.computeIfPresent(event.getVehicleId(), (id, state) -> {
            state.state.set(event.getEndCharge());
            return state;
        });
    }

    private void writeTimestep(double time) {
        for (var entry : states.entrySet()) {
            VehicleState state = entry.getValue();

            try {
                writer.write(String.join(";", new String[] {
                        entry.getKey().toString(), //
                        String.valueOf(time), //
                        String.valueOf(EvUnits.J_to_kWh(state.state.get())), //
                        String.valueOf(
                                state.state.get()
                                        / state.capacity) //
                }) + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
        for (ElectricVehicleSpecification vehicle : electricFleet.getVehicleSpecifications().values()) {
            states.put(vehicle.getId(),
                    new VehicleState(new AtomicDouble(vehicle.getInitialCharge()), vehicle.getBatteryCapacity()));
        }

        writeTimestep(Double.NEGATIVE_INFINITY);
    }

    @Override
    public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
        double time = e.getSimulationTime();

        if (trackingInterval > 0 && ((int) time) % trackingInterval == 0) {
            // trackingInterval = 0 is a valid option if we only want to have start and end
            writeTimestep(time);
        }
    }
}
