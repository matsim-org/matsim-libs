package playground.sebhoerl.av.utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.sebhoerl.av.framework.AVModule;

public class AVTripCounter implements PersonDepartureEventHandler {
    private PrintWriter output;
    
    private long countAVTrips = 0;
    private long countAllTrips = 0;
    
    public AVTripCounter(OutputDirectoryHierarchy hierarchy) {
        try {
            output = new PrintWriter(hierarchy.getOutputFilename("trip_counts.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        if (output != null) {
            output.println("iteration\ttotal_trips\tav_trips");
            output.flush();
        }
    }

    @Override
    public void reset(int iteration) {
        if (output != null) {
            output.println(String.format("%d\t%d\t%d", 
                    iteration, countAllTrips, countAVTrips));
            output.flush();
        }
        
        countAVTrips = 0;
        countAllTrips = 0;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getPersonId().toString().startsWith("av")) {
            return;
        }
        
        if (event.getLegMode().equals(AVModule.AV_MODE)) {
            countAVTrips++;
        } else if (!event.getLegMode().equals(TransportMode.transit_walk)) {
            countAllTrips++;
        }
    }
}
