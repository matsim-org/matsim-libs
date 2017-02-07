package playground.clruch.export;

import java.io.File;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;


/**
 * Comments for class EventFileToProcessingXML
 * Read an Event file and generate appropriate processing file for network visualization
 *
 * @author Claudio Ruch
 * @version 1.0
 */
public class EventFileToProcessingXML {
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
        directory.mkdirs();
        File fileImport = new File(dir, "output/output_events.xml");
        System.out.println("Is directory?  " + dir.isDirectory());

        // TODO reduce printouts
        // TODO move into function of class
        EventsManager events = EventsUtils.createEventsManager();
        events.addHandler(i -> System.out.println("" + i));

        // add event handlers to create waitingCustomers file
        WaitingCustomers waitingCustomers = new WaitingCustomers();
        waitingCustomers.initialize(events);

        // add event handlers to create VehicleStatus file
        VehicleStatus vehicleStatus = new VehicleStatus();
        vehicleStatus.initialize(events);

        // add experimental file to reverse engineer XML file
        VehicleStatusLab vehicleStatusLab = new VehicleStatusLab();
        vehicleStatusLab.initialize(events);

        // add vehicle location reader
        VehicleLocation vehicleLocation = new VehicleLocation();
        vehicleLocation.initialize(events);

        // run the events reader
        new MatsimEventsReader(events).readFile(fileImport.toString());

        // write XML files
        waitingCustomers.writeXML(directory);
        vehicleStatus.writeXML(directory);
        vehicleStatusLab.writeXML(directory);
        vehicleLocation.writeXML(directory);

        System.out.println("routine finished successfully");
    }
}
