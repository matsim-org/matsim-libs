package cascadingInfoTest;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.tracking.SimulationTracker;



public class AverageTimeTracker implements SimulationTracker {

	private Collection<EventHandler> handlers;
	private Collection<Info> infos;
	private AverageTimeInfo timeInfo;
	private TimeSpanHandler handler;
	
	public AverageTimeTracker() {
		handlers = new ArrayList<EventHandler>();
		handler = new TimeSpanHandler();
		handlers.add(handler);
		infos = new ArrayList<Info>();
		timeInfo = new AverageTimeInfo();
		infos.add(timeInfo);
	}
	
	
	@Override
	public Collection<EventHandler> getEventHandlers() {
		return handlers;
	}

	@Override
	public Collection<Info> getInfos() {
		return infos;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		int numberOfStops = handler.getNumberOfStops();
		double totalTransportTime = handler.getTotalTime();
		double averageTransportTime = totalTransportTime/numberOfStops;
		AverageTimeInfoFunctionValue value =  (AverageTimeInfoFunctionValue) timeInfo.getFunction().getValues().iterator().next();
		value.setValue(averageTransportTime);
	}


	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
}
