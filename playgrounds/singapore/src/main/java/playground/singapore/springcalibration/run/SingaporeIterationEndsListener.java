package playground.singapore.springcalibration.run;

import java.io.IOException;

import org.matsim.contrib.travelsummary.events2traveldiaries.EventsToTravelDiaries;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;


public class SingaporeIterationEndsListener implements IterationEndsListener {
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		MatsimServices controler = event.getServices();
		
		if (event.getIteration() % controler.getConfig().controler().getWriteEventsInterval() == 0) {
			String eventsFileName = controler.getControlerIO().getIterationFilename(event.getIteration(), "events") + ".xml.gz";
            int currentIteration = event.getIteration();
            EventsManager events = EventsUtils.createEventsManager();
            
            EventsToTravelDiaries eventsToTravelDiaries =
                    new EventsToTravelDiaries(controler.getScenario());

            events.addHandler(eventsToTravelDiaries);

            new MatsimEventsReader(events).readFile(eventsFileName);

            try {
                eventsToTravelDiaries.writeSimulationResultsToTabSeparated(controler.getControlerIO().getIterationPath(currentIteration), "_" + currentIteration);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }	
	}

}
