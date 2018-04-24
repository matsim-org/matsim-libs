package lspScoringTests;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.functions.InfoFunctionValue;
import lsp.functions.InfoFunctionValueImpl;
import lsp.tracking.SimulationTracker;

public class TipSimulationTracker implements SimulationTracker {

	private TipEventHandler handler;
	private TipInfo info;
	
	public TipSimulationTracker(TipEventHandler handler, TipInfo info) {
		this.info = info;
		this.handler = handler;
	}
	
	@Override
	public Collection<EventHandler> getEventHandlers() {
		ArrayList<EventHandler> handlers = new ArrayList<EventHandler>();
		handlers.add(handler);
		return handlers;
	}

	@Override
	public Collection<Info> getInfos() {
		ArrayList<Info> infos = new ArrayList<Info>();
		infos.add(info);
		return infos;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		double tip = handler.getTip();
		InfoFunctionValue<Double>  value = new InfoFunctionValueImpl<Double>("TIP IN EUR");
		value.setValue(tip);
		info.getFunction().getValues().add(value);
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

	
}
