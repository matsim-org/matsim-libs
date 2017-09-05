package playground.joel.data;

import java.io.File;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.clruch.utils.HelperPredicates;
/**
 * Created by Joel on 28.02.2017.
 */
class TravelTimes extends AbstractData {

    NavigableMap<String, NavigableMap<Double, Double>> travelTimes = new TreeMap<>();
    HashMap<String, Set<Id<Person>>> vehicleCustomers = new HashMap<>();
    HashMap<String, Double> avTripStart = new HashMap<>();
    double totalTimeWithCust = 0;
    double totalTimeRatio;
    static int numAVs = 0;

    // cut the total time ratio
    DecimalFormat ratioForm = new DecimalFormat("#.####");


    void calculateTimeRatio() {
       if(!(numAVs == 0)) {
            totalTimeRatio = Double.parseDouble(ratioForm.format(totalTimeWithCust / (numAVs * 108000)));
        } else System.out.println("no AVs found while calculating the time ratio");

    }

    private void setStartTime(PersonEntersVehicleEvent event){
        avTripStart.put(event.getVehicleId().toString(), event.getTime());
    }

    private double getStartTime(PersonLeavesVehicleEvent event){
        return avTripStart.get(event.getVehicleId().toString());
    }

    private void put(String vehicle, double startTime, double endTime) {
        if (!travelTimes.containsKey(vehicle))
            travelTimes.put(vehicle, new TreeMap<>());
        //travelTimes.get(vehicle).put(startTime, avStatus);
        travelTimes.get(vehicle).put(startTime, endTime);
        if (endTime <= 108000) {
            totalTimeWithCust += endTime - startTime;
        } else{
            totalTimeWithCust += 108000 - startTime;
        }
        //System.out.println("time with customer: " + totalTimeWithCust);
    }

    @Override
    void initialize(EventsManager events) {
        // add handlers to read vehicle status
        {

            // personentersvehicle
            events.addHandler(new PersonEntersVehicleEventHandler() {
                // <event time="21574.0" type="PersonEntersVehicle" person="27114_1" vehicle="av_av_op1_174" />
                // <event time="21589.0" type="PersonEntersVehicle" person="av_av_op1_174" vehicle="av_av_op1_174" />
                @Override
                public void handleEvent(PersonEntersVehicleEvent event) {
                    final String vehicle = event.getVehicleId().toString();
                    final Id<Person> person = event.getPersonId();
                    if (HelperPredicates.isHuman(person))
                        setStartTime(event); // remember startTime
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });

            // personleavesvehicle
            events.addHandler(new PersonLeavesVehicleEventHandler() {
                // <event time="21796.0" type="PersonLeavesVehicle" person="27114_1" vehicle="av_av_op1_174" />
                @Override
                public void handleEvent(PersonLeavesVehicleEvent event) {
                    final String vehicle = event.getVehicleId().toString();
                    final Id<Person> person = event.getPersonId();
                    if (HelperPredicates.isHuman(person))
                        put(vehicle, getStartTime(event), event.getTime()); // add both travel times
                }

                @Override
                public void reset(int iteration) {
                    // intentionally empty
                }
            });

        }

    }

    @Override
    void writeXML(File directory) {

        File fileExport = new File(directory, "travelTimes.xml");

        // export to node-based XML file
        new TravelTimesXML().generate(travelTimes, fileExport);

        numAVs = avTripStart.size();

        calculateTimeRatio();

    }
}
