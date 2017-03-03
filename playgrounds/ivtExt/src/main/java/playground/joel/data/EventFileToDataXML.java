package playground.joel.data;


import java.io.File;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

//import playground.clruch.export.EventFileToProcessingXML;
//import playground.clruch.export.VehicleLocation;
//import playground.clruch.export.VehicleStatus;
//import playground.clruch.export.WaitingCustomers;


/**
 * Created by Joel on 28.02.2017.
 */


/**
 * Comments for class EventFileToProcessingXML
 * Read an Event file and generate appropriate processing file for network visualization
 *
 * @author Claudio Ruch
 * @version 1.0
 */
public class EventFileToDataXML {
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

            File data = new File(output, "data");
            data.mkdir();
            if (data.exists()) { // check if data folder exists
                File fileImport = new File(dir, "output/output_events.xml");

                EventsManager events = EventsUtils.createEventsManager();
                /*
                // add event handlers to create waitingCustomers file
                WaitingCustomers waitingCustomers = new WaitingCustomers();
                waitingCustomers.initialize(events);

                // add event handlers to create VehicleStatus file
                VehicleStatus vehicleStatus = new VehicleStatus();
                vehicleStatus.initialize(events);

                // add vehicle location reader
                VehicleLocation vehicleLocation = new VehicleLocation();
                vehicleLocation.initialize(events);
                */
                // add event handlers to create TravelTimes file
                TravelTimes travelTimes = new TravelTimes();
                travelTimes.initialize(events);

                // add event handlers to create travelTimes file
                BinnedTravelTimes binnedTravelTimes = new BinnedTravelTimes();
                binnedTravelTimes.initialize(events);


                // create TotalData file binnedTravelTimes file
                TotalData totalData = new TotalData();

                // run the events reader
                new MatsimEventsReader(events).readFile(fileImport.toString());


                // write XML files
                travelTimes.writeXML(data);
                binnedTravelTimes.writeXML(data);

                File totalDataDir = new File(data, "totalData.xml");
                totalData.generate("0", String.valueOf(travelTimes.totalTimeRatio), "0", "0", totalDataDir);
                /*
                waitingCustomers.writeXML(directory);
                vehicleStatus.writeXML(directory);
                vehicleLocation.writeXML(directory);
                */
            } else
                new RuntimeException("data directory does not exist").printStackTrace();
        } else
            new RuntimeException("output directory does not exist").printStackTrace();

    }

    public static void main(String[] args) {
        //convert(new File(args[0]));

        //make sure path suits your needs otherwise it may overwrite stuff!
        convert(new File("C:/Users/Joel/Documents/Studium/ETH/Bachelorarbeit/Simulation_Data/2017_02_23_Sioux_onlyUnitCapacityAVs"));
    }
}



