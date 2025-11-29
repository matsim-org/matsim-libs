package org.matsim.contrib.ev.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import com.google.common.io.Files;

public class VehicleTrajectoryListener implements LinkLeaveEventHandler,
        VehicleLeavesTrafficEventHandler, EnergyChargedEventHandler,
        IdlingEnergyConsumptionEventHandler, DrivingEnergyConsumptionEventHandler, IterationStartsListener,
        IterationEndsListener, ShutdownListener {
	static public final String OUTPUT_FILE = "ev_trajectories.csv";

    private final String fileEnding;
    private final EventsManager eventsManager;
    private final OutputDirectoryHierarchy outputHierarchy;

    private final Network network;
    private final ElectricFleetSpecification electricFleet;

    private final IdMap<Vehicle, Double> energy = new IdMap<>(Vehicle.class);
    private final IdMap<Vehicle, Double> capacity = new IdMap<>(Vehicle.class);

    private BufferedWriter writer;
    private final int interval;

    public VehicleTrajectoryListener(EventsManager eventsManager, Network network,
            ElectricFleetSpecification electricFleet, OutputDirectoryHierarchy outputHierarchy, int interval,
            CompressionType compressionType) {
        this.fileEnding = compressionType.fileEnding;
        this.eventsManager = eventsManager;
        this.network = network;
        this.electricFleet = electricFleet;
        this.interval = interval;
        this.outputHierarchy = outputHierarchy;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (interval > 0 && (event.getIteration() % interval == 0 || event.isLastIteration())) {
            energy.clear();
            capacity.clear();

            for (ElectricVehicleSpecification vehicle : electricFleet.getVehicleSpecifications().values()) {
                energy.put(vehicle.getId(), vehicle.getInitialCharge());
                capacity.put(vehicle.getId(), vehicle.getBatteryCapacity());
            }

            String outputPath = outputHierarchy.getIterationFilename(event.getIteration(),
                    OUTPUT_FILE + this.fileEnding);

            writer = IOUtils.getBufferedWriter(outputPath);

            try {
                writer.write(String.join(";", new String[] {
                        "vehicle_id", //
                        "time", //
                        "link_id", //
                        "x", //
                        "y", //
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
                outputHierarchy.getIterationFilename(event.getIteration(), OUTPUT_FILE + this.fileEnding));
        File outputPath = new File(outputHierarchy.getOutputFilename(OUTPUT_FILE + this.fileEnding));

        try {
            Files.copy(iterationPath, outputPath);
        } catch (IOException e) {
        }
    }

    @Override
    public void handleEvent(DrivingEnergyConsumptionEvent event) {
        energy.compute(event.getVehicleId(), (id, value) -> {
            return event.getEndCharge();
        });
    }

    @Override
    public void handleEvent(IdlingEnergyConsumptionEvent event) {
        energy.compute(event.getVehicleId(), (id, value) -> {
            return event.getEndCharge();
        });
    }

    @Override
    public void handleEvent(EnergyChargedEvent event) {
        energy.compute(event.getVehicleId(), (id, value) -> {
            return event.getEndCharge();
        });
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        pingVehicle(event.getTime(), event.getVehicleId(), event.getLinkId(), false);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        pingVehicle(event.getTime(), event.getVehicleId(), event.getLinkId(), false);
    }

    private void pingVehicle(double time, Id<Vehicle> vehicleId, Id<Link> linkId, boolean useFromNode) {
        if (!electricFleet.getVehicleSpecifications().containsKey(vehicleId)) {
            return;
        }

        Link link = network.getLinks().get(linkId);
        Coord location = useFromNode ? link.getFromNode().getCoord() : link.getToNode().getCoord();

        try {
            writer.write(String.join(";", new String[] {
                    vehicleId.toString(), //
                    String.valueOf(time), //
                    linkId.toString(), //
                    String.valueOf(location.getX()), //
                    String.valueOf(location.getY()), //
                    String.valueOf(EvUnits.J_to_kWh(energy.getOrDefault(vehicleId, Double.NaN))), //
                    String.valueOf(
                            energy.getOrDefault(vehicleId, Double.NaN) / capacity.getOrDefault(vehicleId, Double.NaN)) //
            }) + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
