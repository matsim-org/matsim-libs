package example.lsp.lspReplanning;

import java.util.Collection;

import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;

import lsp.LSP;
import lsp.LSPPlan;
import lsp.ShipmentAssigner;
import lsp.shipment.LSPShipment;

/*package-private*/ class TomorrowShipmentAssignerStrategyFactory  {
	
	private ShipmentAssigner assigner;

	/*package-private*/ TomorrowShipmentAssignerStrategyFactory(ShipmentAssigner assigner) {
		this.assigner = assigner;
	}

	/*package-private*/ GenericPlanStrategy<LSPPlan, LSP> createStrategy(){
		GenericPlanStrategyImpl<LSPPlan, LSP> strategy = new GenericPlanStrategyImpl<LSPPlan, LSP>( new BestPlanSelector<LSPPlan, LSP>());
		
		GenericPlanStrategyModule<LSPPlan> tomorrowModule = new GenericPlanStrategyModule<LSPPlan>() {

			@Override
			public void prepareReplanning(ReplanningContext replanningContext) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void handlePlan(LSPPlan plan) {
				plan.setAssigner(assigner);
//				LSP lsp = assigner.getLSP();
				LSP lsp = plan.getLsp();
				Collection<LSPShipment> shipments = lsp.getShipments();
				for(LSPShipment shipment : shipments) {
					assigner.assignShipment(shipment);
				}
			}

			@Override
			public void finishReplanning() {
				// TODO Auto-generated method stub
				
			}
			
		};
		
		strategy.addStrategyModule(tomorrowModule);
		return strategy;
	}
	
}
