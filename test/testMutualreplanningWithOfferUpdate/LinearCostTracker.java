package testMutualreplanningWithOfferUpdate;

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.events.handler.EventHandler;

import lsp.functions.Info;
import lsp.functions.InfoFunctionValue;
import lsp.tracking.SimulationTracker;

public class LinearCostTracker implements SimulationTracker{

	private Collection<EventHandler> eventHandlers;
	private Collection<Info> infos;
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
		CostInfo costInfo = new CostInfo();
		infos = new ArrayList<Info>();
		infos.add(costInfo);
		this.eventHandlers = new ArrayList<EventHandler>();
	}
	
	
	@Override
	public Collection<EventHandler> getEventHandlers() {
		return eventHandlers;
	}

	@Override
	public Collection<Info> getInfos() {
		return infos;
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		for(EventHandler handler : eventHandlers) {
			if(handler instanceof TourStartHandler) {
				TourStartHandler startHandler = (TourStartHandler) handler;
				this.vehicleFixedCosts = startHandler.getVehicleFixedCosts();
			}
			if(handler instanceof DistanceAndTimeHandler) {
				DistanceAndTimeHandler distanceHandler = (DistanceAndTimeHandler) handler;
				this.distanceCosts = distanceHandler.getDistanceCosts();
				this.timeCosts = distanceHandler.getTimeCosts();
			}
			if(handler instanceof CollectionServiceHandler) {
				CollectionServiceHandler collectionHandler = (CollectionServiceHandler) handler;
				totalNumberOfShipments = collectionHandler.getTotalNumberOfShipments();
				totalWeightOfShipments = collectionHandler.getTotalWeightOfShipments();
				loadingCosts = collectionHandler.getTotalLoadingCosts();
			}
		}
		
		double totalCosts = distanceCosts + timeCosts + loadingCosts + vehicleFixedCosts;
		fixedUnitCosts = (totalCosts * shareOfFixedCosts)/totalNumberOfShipments;
		linearUnitCosts = (totalCosts * (1-shareOfFixedCosts))/totalWeightOfShipments;
				
		CostInfo info = (CostInfo) infos.iterator().next();
		for(InfoFunctionValue value : info.getFunction().getValues()) {
			if(value instanceof FixedCostFunctionValue) {		
				((FixedCostFunctionValue)value).setValue(Double.toString(fixedUnitCosts));
			}
			if(value instanceof LinearCostFunctionValue) {
				((LinearCostFunctionValue)value).setValue(Double.toString(linearUnitCosts));
			}
		}
		
		
	}


	@Override
	public void reset() {
		for(EventHandler eventHandler : eventHandlers) {
			eventHandler.reset(0);
		}
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
