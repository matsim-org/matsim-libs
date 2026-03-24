package org.matsim.contrib.ev.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.ChargingEndEvent;
import org.matsim.contrib.ev.charging.ChargingEndEventHandler;
import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup.CompressionType;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import com.google.common.io.Files;

/**
 * TODO: Complete this class with the wait start and end tracking functionality
 */
public class ChargingActivityListener implements ChargingStartEventHandler, ChargingEndEventHandler,
        IterationEndsListener, IterationStartsListener, ShutdownListener {

    public static final String OUTPUT_FILE_NAME = "ev_charging_activities.csv";

    private final IdMap<Vehicle, ChargingStartEvent> ongoing = new IdMap<>(Vehicle.class);
    private final ChargingInfrastructureSpecification infrastructure;
    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final Network network;
    private final EventsManager eventsManager;
    private final int interval;
    private final CompressionType compressionType;

    public ChargingActivityListener(ChargingInfrastructureSpecification infrastructure,
            Network network, OutputDirectoryHierarchy outputDirectoryHierarchy, EventsManager eventsManager,
            int interval, CompressionType compressionType) {
        this.infrastructure = infrastructure;
        this.network = network;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.eventsManager = eventsManager;
        this.interval = interval;
        this.compressionType = compressionType;
    }

    private BufferedWriter writer = null;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (interval > 0 && (event.getIteration() % interval == 0 || event.isLastIteration())) {
            writer = IOUtils.getBufferedWriter(
                    outputDirectoryHierarchy.getIterationFilename(event.getIteration(),
                            OUTPUT_FILE_NAME, compressionType));

            try {
                writer.write(String.join(";", new String[] {
                        "vehicle_id",
                        "link_id",
                        "charger_id",
                        "x",
                        "y",
                        "start_time",
                        "end_time",
                        "start_charge_kWh",
                        "end_charge_kWh",
                }) + "\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.eventsManager.addHandler(this);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            writer = null;
            this.eventsManager.removeHandler(this);
        }
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        ongoing.put(event.getVehicleId(), event);
    }

    @Override
    public void handleEvent(ChargingEndEvent end) {
        ChargingStartEvent start = ongoing.remove(end.getVehicleId());
        Objects.requireNonNull(start,
                () -> String.format("ChargingEndEvent fired for vehicle '%s' with no matching ChargingStartEvent",
                        end.getVehicleId().toString()));

        ChargerSpecification charger = infrastructure.getChargerSpecifications()
                .get(end.getChargerId());
        Link link = network.getLinks().get(charger.getLinkId());
        Objects.requireNonNull(link, () -> String.format("Link %s not found", charger.getLinkId().toString()));

        try {
            writer.write(String.join(";", new String[] {
                    end.getVehicleId().toString(),
                    link.getId().toString(),
                    end.getChargerId().toString(),
                    String.valueOf(link.getCoord().getX()),
                    String.valueOf(link.getCoord().getY()),
                    String.valueOf(start.getTime()),
                    String.valueOf(end.getTime()),
                    String.valueOf(EvUnits.J_to_kWh(start.getCharge())),
                    String.valueOf(EvUnits.J_to_kWh(end.getCharge()))
            }) + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        File iterationPath = new File(
                outputDirectoryHierarchy.getIterationFilename(shutdownEvent.getIteration(),
                        OUTPUT_FILE_NAME, compressionType));

        File outputPath = new File(
                outputDirectoryHierarchy.getOutputFilename(OUTPUT_FILE_NAME, compressionType));

        try {
            Files.copy(iterationPath, outputPath);
        } catch (IOException ignored) {
        }
    }

    @Override
    public void reset(int iteration) {
        this.ongoing.clear();
    }
}
