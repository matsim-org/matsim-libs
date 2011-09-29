package city2000w;

import org.apache.log4j.Logger;

import playground.mzilske.freight.Contract;
import freight.BasicShipperAgentImpl;
import freight.ShipperAgent;
import freight.ShipperAgentTracker;
import freight.ShipperImpl;
import freight.api.ShipperAgentFactory;

public class KarlsruheShipperAgentFactory implements ShipperAgentFactory{

	class KarlsruheShipperAgent extends BasicShipperAgentImpl {

		private Logger logger = Logger.getLogger(KarlsruheShipperAgent.class);
		
		public KarlsruheShipperAgent(ShipperImpl shipper) {
			super(shipper);
		}

		@Override
		public void scoreSelectedPlan() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void informTSPContractAccept(Contract contract) {
			logger.info("hell. i contracted a new tsp. me: " + getId() + "; " + contract.getSeller());
		}

		@Override
		public void informTSPContractCanceled(Contract contract) {
			logger.info("i canceled this fucking contract. me: " + getId() + "; " + contract.getSeller());
			
		}
	}
	
	
	@Override
	public ShipperAgent createShipperAgent(ShipperAgentTracker shipperAgentTracker, ShipperImpl shipper) {
		KarlsruheShipperAgent agent = new KarlsruheShipperAgent(shipper);
		return agent;
	}

}
