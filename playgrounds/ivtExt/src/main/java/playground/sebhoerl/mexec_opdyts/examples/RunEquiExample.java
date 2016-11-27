package playground.sebhoerl.mexec_opdyts.examples;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import playground.sebhoerl.mexec.Environment;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec_opdyts.OpdytsExecutor;
import playground.sebhoerl.mexec_opdyts.opdyts.EventsBasedObjectiveFunction;
import playground.sebhoerl.mexec_opdyts.opdyts.IterationEventHandler;
import playground.sebhoerl.mexec_opdyts.opdyts.IterationObjectiveFunction;

public class RunEquiExample {
    static public class CustomEventHandler implements PersonDepartureEventHandler, IterationEventHandler {
        @Override
        public double getValue() {
            return 0;
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {}

        @Override
        public void reset(int iteration) {}
    }

    static public void main(String[] args) {
        Environment environment;

        EventsBasedObjectiveFunction objective = new EventsBasedObjectiveFunction(new CustomEventHandler());


        OpdytsExecutor opdyts = new OpdytsExecutor(environment, "/home/sebastian/opdyts.cache");
    }
}
