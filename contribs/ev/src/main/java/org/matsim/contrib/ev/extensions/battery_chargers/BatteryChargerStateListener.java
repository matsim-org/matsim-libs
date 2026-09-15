package org.matsim.contrib.ev.extensions.battery_chargers;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

/**
 * This listener tracks the state of battery-based chargers and writes it out
 * into a CSV file.
 * 
 * @author sebhoerl
 */
public class BatteryChargerStateListener
        implements IterationStartsListener, IterationEndsListener, BatteryChargerStateEventHandler {
    static public final String OUTPUT_FILE = "battery_charger_states.csv";

    private final OutputDirectoryHierarchy outputDirectoryHierarchy;
    private final EventsManager eventsManager;

    public BatteryChargerStateListener(OutputDirectoryHierarchy outputDirectoryHierarchy, EventsManager eventsManager) {
        this.outputDirectoryHierarchy = outputDirectoryHierarchy;
        this.eventsManager = eventsManager;
    }

    private BufferedWriter writer = null;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        try {
            String outputPath = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), OUTPUT_FILE);

            writer = IOUtils.getBufferedWriter(outputPath);

            writer.write(String.join(";", new String[] {
                    "time", "charger_id", "charge_kWh", "soc"
            }) + "\n");

            eventsManager.addHandler(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void handleEvent(BatteryChargerStateEvent event) {
        try {
            writer.write(String.join(";", new String[] {
                    String.valueOf(event.getTime()), //
                    event.getChargerId().toString(), //
                    String.valueOf(event.getState_kWh()), //
                    String.valueOf(event.getSoc()) //
            }) + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        eventsManager.removeHandler(this);

        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
