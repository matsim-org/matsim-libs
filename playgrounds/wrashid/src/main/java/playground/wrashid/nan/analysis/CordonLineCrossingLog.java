package playground.wrashid.nan.analysis;

import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioImpl;

import playground.wrashid.lib.GeneralLib;

public class CordonLineCrossingLog {

	public static void main(String[] args) {
		String basePath="H:/data/experiments/ARTEMIS/output/run2/";
		String plansFile=basePath + "output_plans.xml.gz";
		String networkFile=basePath + "output_network.xml.gz";
		String facilititiesPath=basePath + "output_facilities.xml.gz";
		String eventsFileName = basePath + "ITERS/it.50/50.events.txt.gz";
		
		ScenarioImpl scenario = (ScenarioImpl) GeneralLib.readScenario(plansFile, networkFile, facilititiesPath);
		
		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();

		CordonVolumeCounter cordonVolumeCounter=new CordonVolumeCounter();
		eventsManager.addHandler(cordonVolumeCounter);

		new MatsimEventsReader(eventsManager).readFile(eventsFileName);		
	}
	
	private static class CordonVolumeCounter implements LinkEnterEventHandler,ActivityEndEventHandler, AgentDepartureEventHandler {

		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			// check, if act on other size of cordon (prev/next)
			// provide statistics separately for the directions!
			// just only log the time at cordon!
		}

		@Override
		public void handleEvent(AgentDepartureEvent event) {
			// TODO: sich das merken.
			
		}

		@Override
		public void handleEvent(ActivityEndEvent event) {
			// TODO Auto-generated method stub
			
		}
		
		
	}
	
}
