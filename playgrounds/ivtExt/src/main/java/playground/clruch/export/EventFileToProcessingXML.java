package playground.clruch.export;

/**
 * Comments for class EventFileToProcessingXML
 *
 * @author Claudio Ruch
 * @version 1.0
 */


import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import java.io.File;


/**
 * Read an Event file and generate appropriate processing file for network visualization
 */

public class EventFileToProcessingXML {
    /**
     * checks if person with id is a person or an "av-driver", i.e. a virtual agent
     * the virtual agents start with "av".
     *
     * @param id
     * @return
     */
    public static boolean isPerson(String id) {
        return !id.startsWith("av_");
    }


    /**
     * reads an output events file from a matsim simulation and outputs an XML file to be read by
     * processing
     *
     * @param args the path of the project folder
     */
    public static void main(String[] args) {

        // read an event output file given String[] args
        final File dir = new File(args[0]);
        File directory = new File(dir, "output/processing");
        File fileImport = new File(dir, "output/output_events.xml");
        File fileExport3 = new File(dir, "output/processing/vehicleStatus.xml");
        System.out.println("Is directory?  " + dir.isDirectory());

        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(i -> System.out.println("" + i));

        // add event handlers to create waitingCustomers file
        WaitingCustomers waitingCustomers = new WaitingCustomers();
        waitingCustomers.initialize(events);

        // add event handlers to create VehicleStatus file
        VehicleStatus vehicleStatus = new VehicleStatus();
        vehicleStatus.initialize(events);

        // run the events reader
        new MatsimEventsReader(events).readFile(fileImport.toString());

        // write XML files
        waitingCustomers.writeXML(directory);
        vehicleStatus.writeXML(directory);

        System.out.println("routine finished successfully");
    }
}
