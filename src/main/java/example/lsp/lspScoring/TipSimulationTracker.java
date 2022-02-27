package example.lsp.lspScoring;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;
import lsp.controler.LSPSimulationTracker;

/*package-private*/ class TipSimulationTracker implements LSPSimulationTracker{

	private final TipEventHandler handler;
	private final LSPInfo info;

	/*package-private*/ TipSimulationTracker(TipEventHandler handler, LSPInfo info) {
		this.info = info;
		this.handler = handler;
	}
	
	@Override
	public Collection<EventHandler> getEventHandlers() {
		ArrayList<EventHandler> handlers = new ArrayList<>();
		handlers.add(handler);
		return handlers;
	}

	@Override
	public Collection<LSPInfo> getInfos() {
		ArrayList<LSPInfo> infos = new ArrayList<>();
		infos.add(info);
		return infos;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		double tip = handler.getTip();
//		LSPInfoFunctionValueImpl<Object> value = LSPInfoFunctionUtils.createInfoFunctionValue( "TIP IN EUR" );
//		value.setValue(tip);
//		info.getAttributes().getAttributes().add(value );
		info.getAttributes().putAttribute( "TIP IN EUR", tip );
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

	
}
