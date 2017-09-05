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

    static String path = "C:/Users/Joel/Documents/Studium/ETH/Bachelorarbeit/Simulation_Data/2017_03_22_Sioux_Hungarian_check1av"; //used for test purpose in main()



    public static void convert(final File dir) {
        path = dir.toString();

        File output = new File(dir, "output");
        if (output.exists()) { // check if output folder exists

            File data = new File(output, "data");
            data.mkdir();
            if (data.exists()) { // check if data folder exists
                File fileImport = new File(dir, "output/output_events.xml");

                EventsManager events = EventsUtils.createEventsManager();

                // TODO: can be omitted, just to create travelTimes.xml for test purpose
                // add event handlers to create TravelTimes file
                TravelTimes travelTimes = new TravelTimes();
                travelTimes.initialize(events);

                // add event handlers to create binnedTravelTimes file
                BinnedTravelTimes binnedTravelTimes = new BinnedTravelTimes();
                binnedTravelTimes.initialize(events);

                // add event handlers to create binnedTravelTimes file
                BinnedTravelDistances binnedTravelDistances = new BinnedTravelDistances();
                binnedTravelDistances.initialize(events);

                // add event handlers to create binnedWaitingTimes files
                BinnedWaitingTimes binnedWaitingTimes = new BinnedWaitingTimes();
                binnedWaitingTimes.initialize(events);


                // create TotalData file binnedTravelTimes file
                TotalData totalData = new TotalData();

                // run the events reader
                new MatsimEventsReader(events).readFile(fileImport.toString());


                // write XML files
                // TODO: can be omitted, just to create travelTimes.xml for test purpose
                travelTimes.writeXML(data);

                binnedTravelTimes.writeXML(data);
                binnedTravelDistances.writeXML(data);
                binnedWaitingTimes.writeXML(data);

                File totalDataDir = new File(data, "totalData.xml");
                totalData.generate(String.valueOf(binnedTravelTimes.totalTimeRatio),
                        String.valueOf(binnedTravelDistances.totalDistanceRatio), String.valueOf(BinnedWaitingTimes.totalMean),
                        String.valueOf(BinnedWaitingTimes.totalQuantile50), String.valueOf(BinnedWaitingTimes.totalQuantile95),
                        totalDataDir);
            } else
                new RuntimeException("data directory does not exist").printStackTrace();
        } else
            new RuntimeException("output directory does not exist").printStackTrace();

    }

    public static void main(String[] args) {
        //convert(new File(args[0]));

        //make sure path suits your needs otherwise it may overwrite stuff!
        convert(new File(path));
    }
}



