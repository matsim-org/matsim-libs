package example.lsp.simulationTrackers;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.LSPInfo;
import lsp.functions.LSPInfoFunctionValue;
import lsp.controler.LSPSimulationTracker;

/*package-private*/ class LinearCostTracker implements LSPSimulationTracker{

	private Collection<EventHandler> eventHandlers;
	private Collection<LSPInfo> infos;
	private double distanceCosts;
	private double timeCosts;
	private double loadingCosts;
	private double vehicleFixedCosts;
	private int totalNumberOfShipments;
	private int totalWeightOfShipments;
	
	private double fixedUnitCosts;
	private double linearUnitCosts;
	
	private double shareOfFixedCosts;
	
	public LinearCostTracker(double shareOfFixedCosts) {
		this.shareOfFixedCosts = shareOfFixedCosts;
		example.lsp.simulationTrackers.CostInfo costInfo = new example.lsp.simulationTrackers.CostInfo();
		infos = new ArrayList<LSPInfo>();
		infos.add(costInfo);
		this.eventHandlers = new ArrayList<EventHandler>();
	}
	
	
	@Override
	public Collection<EventHandler> getEventHandlers() {
		return eventHandlers;
	}

	@Override
	public Collection<LSPInfo> getInfos() {
		return infos;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		for(EventHandler handler : eventHandlers) {
			if(handler instanceof example.lsp.simulationTrackers.TourStartHandler) {
				example.lsp.simulationTrackers.TourStartHandler startHandler = (example.lsp.simulationTrackers.TourStartHandler) handler;
				this.vehicleFixedCosts = startHandler.getVehicleFixedCosts();
			}
			if(handler instanceof example.lsp.simulationTrackers.DistanceAndTimeHandler) {
				example.lsp.simulationTrackers.DistanceAndTimeHandler distanceHandler = (example.lsp.simulationTrackers.DistanceAndTimeHandler) handler;
				this.distanceCosts = distanceHandler.getDistanceCosts();
				this.timeCosts = distanceHandler.getTimeCosts();
			}
			if(handler instanceof example.lsp.simulationTrackers.CollectionServiceHandler) {
				example.lsp.simulationTrackers.CollectionServiceHandler collectionHandler = (example.lsp.simulationTrackers.CollectionServiceHandler) handler;
				totalNumberOfShipments = collectionHandler.getTotalNumberOfShipments();
				System.out.println(totalNumberOfShipments);
				totalWeightOfShipments = collectionHandler.getTotalWeightOfShipments();
				loadingCosts = collectionHandler.getTotalLoadingCosts();
			}
		}
		
		double totalCosts = distanceCosts + timeCosts + loadingCosts + vehicleFixedCosts;
		fixedUnitCosts = (totalCosts * shareOfFixedCosts)/totalNumberOfShipments;
		linearUnitCosts = (totalCosts * (1-shareOfFixedCosts))/totalWeightOfShipments;
		
		example.lsp.simulationTrackers.CostInfo info = (example.lsp.simulationTrackers.CostInfo) infos.iterator().next();
		for(LSPInfoFunctionValue value : info.getFunction().getValues()) {
			if(value instanceof example.lsp.simulationTrackers.FixedCostFunctionValue) {
				((example.lsp.simulationTrackers.FixedCostFunctionValue)value).setValue(fixedUnitCosts);
			}
			if(value instanceof example.lsp.simulationTrackers.LinearCostFunctionValue) {
				((example.lsp.simulationTrackers.LinearCostFunctionValue)value).setValue(linearUnitCosts);
			}
		}
		
		
	}


	@Override
	public void reset() {
		distanceCosts = 0;
		timeCosts = 0;
		loadingCosts = 0;
		vehicleFixedCosts = 0;
		totalNumberOfShipments = 0;
		totalWeightOfShipments = 0;
		fixedUnitCosts = 0;
		linearUnitCosts = 0;
		
	}

	
	
}
