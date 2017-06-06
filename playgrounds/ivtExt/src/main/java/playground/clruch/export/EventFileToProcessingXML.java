// code by clruch
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
     * reads an output events file from a matsim simulation and
     * outputs an XML file to be read by processing
     *
     * @param folder
     *            of the project folder
     */
    public static void convert(final File dir) {
        File output = new File(dir, "output");
        if (output.exists()) { // check if output folder exists

            File directory = new File(output, "processing");
            directory.mkdir();
            File fileImport = new File(output, "output_events.xml");

            EventsManager events = EventsUtils.createEventsManager();

            // add event handlers to create waitingCustomers file
            WaitingCustomers waitingCustomers = new WaitingCustomers();
            waitingCustomers.initialize(events);

            // add event handlers to create VehicleStatus file
            VehicleStatus vehicleStatus = new VehicleStatus();
            vehicleStatus.initialize(events);

            // add vehicle location reader
            VehicleLocation vehicleLocation = new VehicleLocation();
            vehicleLocation.initialize(events);

            // run the events reader
            new MatsimEventsReader(events).readFile(fileImport.toString());

            // write XML files
            waitingCustomers.writeXML(directory);
            vehicleStatus.writeXML(directory);
            vehicleLocation.writeXML(directory);
        } else
            new RuntimeException("output directory does not exist").printStackTrace();

    }

    public static void main(String[] args) {
        //convert(new File(args[0]));
        //convert(new File("C:/Users/Joel/Documents/Studium/ETH/Bachelorarbeit/Simulation_Data/2017_02_07_Sioux_onlyUnitCapacityAVs"));
        convert(new File("/media/datahaki/data/ethz/2017_03_14_Sioux_consensus"));
    }
}
