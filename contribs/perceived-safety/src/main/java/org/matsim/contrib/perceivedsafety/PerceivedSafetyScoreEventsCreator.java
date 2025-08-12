package org.matsim.contrib.perceivedsafety;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;

/**
 * an event handler, which throws additional scoring events for perceived safety.
 * @author simei94
 */
public class PerceivedSafetyScoreEventsCreator implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler,
        VehicleLeavesTrafficEventHandler, TeleportationArrivalEventHandler {
    private static final Logger log = LogManager.getLogger(PerceivedSafetyScoreEventsCreator.class);

    private final Network network;
    private final EventsManager eventsManager;
    private final AdditionalPerceivedSafetyLinkScore additionalPerceivedSafetyLinkScore;

    private final Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();

    @Inject
    PerceivedSafetyScoreEventsCreator(Scenario scenario, EventsManager eventsManager, AdditionalPerceivedSafetyLinkScore additionalPerceivedSafetyLinkScore) {
        this.network = scenario.getNetwork();
        this.eventsManager = eventsManager;
        this.additionalPerceivedSafetyLinkScore = additionalPerceivedSafetyLinkScore;
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
//        register driver and vehicle
        vehicle2driver.handleEvent(event);
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (vehicle2driver.getDriverOfVehicle(event.getVehicleId()) != null) {
            double amount = additionalPerceivedSafetyLinkScore.computeLinkBasedScore(network.getLinks().get(event.getLinkId()), event.getVehicleId());

            final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle(event.getVehicleId());
            Gbl.assertNotNull(driverOfVehicle);
            this.eventsManager.processEvent(new PersonScoreEvent(event.getTime(), driverOfVehicle, amount, "perceivedSafetyAdditionalLinkScore"));
        } else {
            log.fatal("no driver found for vehicleId={}; not clear why this could happen. Perceived safety is not scored in this case!", event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        vehicle2driver.handleEvent(event);
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        double amount = additionalPerceivedSafetyLinkScore.computeTeleportationBasedScore(event.getDistance(), event.getMode());

        this.eventsManager.processEvent(new PersonScoreEvent(event.getTime(), event.getPersonId(), amount, "perceivedSafetyAdditionalTeleportationScore"));
    }
}
