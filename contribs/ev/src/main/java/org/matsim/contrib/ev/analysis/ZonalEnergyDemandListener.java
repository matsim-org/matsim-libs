package org.matsim.contrib.ev.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.ev.EvUnits;
import org.matsim.contrib.ev.charging.EnergyChargedEvent;
import org.matsim.contrib.ev.charging.EnergyChargedEventHandler;
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

public class ZonalEnergyDemandListener
        implements IterationStartsListener, IterationEndsListener, EnergyChargedEventHandler, ShutdownListener {
    static public final String OUTPUT_FILE = "ev_zonal_energy_demand.csv";

    private final CompressionType compressionType;
    private final EventsManager eventsManager;
    private final OutputDirectoryHierarchy outputHierarchy;

    private final ChargingInfrastructureSpecification infrastructure;
    private final ZoneSystem zoneSystem;

    private final int interval;
    private boolean isActive = false;

    private final List<Map<Id<Zone>, Item>> data = new ArrayList<>(30);

    private class Item {
        public double energy = 0.0;
        public Set<Id<Vehicle>> vehicles = new HashSet<>();
    }

    public ZonalEnergyDemandListener(EventsManager eventsManager, OutputDirectoryHierarchy outputHierarchy,
            ZoneSystem zoneSystem, ChargingInfrastructureSpecification infrastructure, int interval,
            CompressionType compressionType) {
        this.compressionType = compressionType;
        this.eventsManager = eventsManager;
        this.zoneSystem = zoneSystem;
        this.infrastructure = infrastructure;
        this.interval = interval;
        this.outputHierarchy = outputHierarchy;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        data.clear();

        if (interval > 0 && (event.getIteration() % interval == 0 || event.isLastIteration())) {
            isActive = true;
            eventsManager.addHandler(this);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (isActive) {
            writeData(event.getIteration());
            isActive = false;
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

    private void writeData(int iteration) {
        try {
            String outputPath = outputHierarchy.getIterationFilename(iteration, OUTPUT_FILE, compressionType);
            BufferedWriter writer = IOUtils.getBufferedWriter(outputPath);

            writer.write(String.join(";", new String[] {
                    "zone_id", //
                    "hour", //
                    "users", //
                    "demand_kWh" //
            }) + "\n");

            for (int hour = 0; hour < data.size(); hour++) {
                Map<Id<Zone>, Item> hourData = data.get(hour);

                for (var entry : hourData.entrySet()) {
                    Item item = entry.getValue();

                    writer.write(String.join(";", new String[] {
                            entry.getKey().toString(), //
                            String.valueOf(hour), //
                            String.valueOf(item.vehicles.size()), //
                            String.valueOf(EvUnits.J_to_kWh(item.energy)) //
                    }) + "\n");
                }
            }

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Id<Zone>, Item> getBinData(double time) {
        int hour = (int) Math.floor(time / 3600);

        while (data.size() < hour + 1) {
            data.add(new HashMap<>());
        }

        return data.get(hour);
    }

    private Item getItem(double time, Zone zone) {
        return getBinData(time).computeIfAbsent(zone.getId(), id -> new Item());
    }

    @Override
    public void handleEvent(EnergyChargedEvent event) {
        ChargerSpecification charger = infrastructure.getChargerSpecifications().get(event.getChargerId());
        Optional<Zone> zone = zoneSystem.getZoneForLinkId(charger.getLinkId());

        if (zone.isPresent()) {
            Item item = getItem(event.getTime(), zone.get());
            item.energy += event.getEnergy();
            item.vehicles.add(event.getVehicleId());
        }
    }
}
