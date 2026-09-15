package org.matsim.contrib.ev.extensions.placement;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.contrib.ev.charging.ChargingStartEvent;
import org.matsim.contrib.ev.charging.ChargingStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * Information on charger placement
 */
public class ChargerPlacementListener implements StartupListener, ShutdownListener, IterationStartsListener,
        IterationEndsListener, ChargingStartEventHandler {
    private static final String OUTPUT_FILE = "ev_charger_placement_status.csv";

    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final EventsManager eventsManager;
    private final ChargerPlacementManager chargerManager;

    public ChargerPlacementListener(ChargerPlacementManager chargerManager, EventsManager eventsManager,
            OutputDirectoryHierarchy outputDirectoryHierarchy) {
        this.chargerManager = chargerManager;
        this.eventsManager = eventsManager;
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
    }

    private BufferedWriter writer;

    @Override
    public void notifyStartup(StartupEvent event) {
        try {
            String path = outputDirectoryHierarchy.getOutputFilename(OUTPUT_FILE);
            writer = IOUtils.getBufferedWriter(path);

            writer.write(String.join(";", new String[] {
                    "iteration", "charging_events", "blacklisted_events", "chargers", "blacklisted_chargers"
            }) + "\n");

            writer.flush();
        } catch (IOException e) {
        }
    }

    private int chargingEvents = 0;
    private int blacklistedEvents = 0;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        eventsManager.addHandler(this);

        chargingEvents = 0;
        blacklistedEvents = 0;
    }

    @Override
    public void handleEvent(ChargingStartEvent event) {
        chargingEvents++;

        if (chargerManager.isBlacklisted(event.getChargerId())) {
            blacklistedEvents++;
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        eventsManager.removeHandler(this);

        try {
            writer.write(String.join(";", new String[] {
                    String.valueOf(event.getIteration()), String.valueOf(chargingEvents),
                    String.valueOf(blacklistedEvents),
                    String.valueOf(chargerManager.getChargers().size()),
                    String.valueOf(chargerManager.getBlacklist().size())
            }) + "\n");

            writer.flush();
        } catch (IOException e) {
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            writer.close();
        } catch (IOException e) {
        }
    }
}
