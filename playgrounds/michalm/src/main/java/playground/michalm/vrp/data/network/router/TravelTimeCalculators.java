package playground.michalm.vrp.data.network.router;

import org.matsim.api.core.v01.*;
import org.matsim.core.api.experimental.events.*;
import org.matsim.core.events.*;
import org.matsim.core.trafficmonitoring.*;


public class TravelTimeCalculators
{
    public static TravelTimeCalculator createTravelTimeFromEvents(String eventFileName,
            Scenario scenario)
    {
        TravelTimeCalculator ttimeCalc = new TravelTimeCalculatorFactoryImpl()
                .createTravelTimeCalculator(scenario.getNetwork(), scenario.getConfig()
                        .travelTimeCalculator());

        EventsManager inputEvents = EventsUtils.createEventsManager();
        inputEvents.addHandler(ttimeCalc);
        new EventsReaderXMLv1(inputEvents).parse(eventFileName);

        return ttimeCalc;
    }
}
