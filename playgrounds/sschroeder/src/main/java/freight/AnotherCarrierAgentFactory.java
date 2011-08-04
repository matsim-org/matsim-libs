package freight;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.population.algorithms.PlanAlgorithm;

import city2000w.RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder;

import playground.mzilske.freight.CarrierAgent;
import playground.mzilske.freight.CarrierAgentFactory;
import playground.mzilske.freight.CarrierAgentTracker;
import playground.mzilske.freight.CarrierCostCalculatorImpl;
import playground.mzilske.freight.CarrierImpl;
import playground.mzilske.freight.CarrierTimeDistanceCostFunction;
import playground.mzilske.freight.CostMemoryConsolePrinter;
import playground.mzilske.freight.CostMemoryImpl;
import playground.mzilske.freight.OfferMaker;
import playground.mzilske.freight.TollCalculator;

public class AnotherCarrierAgentFactory implements CarrierAgentFactory{

	class MotorwayTollCalc implements TollCalculator {

		Network network;
		
		double tollPerKm = 0.2;
		
		CarrierImpl carrier;
		
		public MotorwayTollCalc(Network network) {
			super();
			this.network = network;
		}
		
		public void iniCarrier(CarrierImpl carrier){
			this.carrier = carrier;
		}

		@Override
		public double getToll(Id linkId) {
			if(linkId.toString().equals("spike")){
				double distance = network.getLinks().get(linkId).getLength();
				return (distance/1000)*tollPerKm;
			}
			if(linkId.toString().equals("spikeR")){
				double distance = network.getLinks().get(linkId).getLength();
				return (distance/1000)*tollPerKm;
			}
			return 0;
		}	
	}
	
	private Network network;

	private PlanAlgorithm router;
	
	private LocationsImpl locations;

	private OfferRecorder offerRecorder;
	
	public void setOfferMaker(OfferMaker offerMaker) {
	}

	public AnotherCarrierAgentFactory(Network network, PlanAlgorithm router) {
		super();
		this.network = network;
		this.router = router;
		makeLocations();
	}

	private void makeLocations() {
		locations = new LocationsImpl();
		locations.addAllLinks((Collection<Link>) network.getLinks().values());
	}

	@Override
	public CarrierAgent createAgent(CarrierAgentTracker tracker,CarrierImpl carrier) {
		CarrierAgent carrierAgent = new CarrierAgent(tracker, carrier, router);
		CarrierTimeDistanceCostFunction costFunction = new CarrierTimeDistanceCostFunction();
		costFunction.init(carrier);
		carrierAgent.setCostFunction(costFunction);
		TollCalculator tollCalc = new MotorwayTollCalc(network);
		carrierAgent.setTollCalculator(tollCalc);
//		carrierAgent.setCostAllocator(new CostAllocatorImpl(carrier, network));
//		carrierAgent.setOfferMaker(new RuinAndRecreateMarginalCostOM(carrier, locations));
//		carrierAgent.setOfferMaker(new RuinAndRecreateAverageMarginalCostOM(carrier, locations));
		carrierAgent.setCostCalculator(new CarrierCostCalculatorImpl(network)); 
		CostMemoryImpl costMemory = new CostMemoryImpl();
		CostMemoryImpl.learningRate = 0.3;
		carrierAgent.setCostMemory(costMemory);
		RuinAndRecreateOfferMakingStrategy strategy = new RuinAndRecreateOfferMakingStrategy(carrier);
		strategy.setOfferRecorder(offerRecorder);
		RuinAndRecreateMarginalCostOM marginalCostOM = new RuinAndRecreateMarginalCostOM(carrier, locations);
		marginalCostOM.setCarrierCostCalculator(costFunction);
		marginalCostOM.setCarrierPlanBuilder(new RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder(network));
		RuinAndRecreateAverageMarginalCostOM_V2 avgMarginalCostOM = new RuinAndRecreateAverageMarginalCostOM_V2(carrier, locations);
//		AverageMarginalCostOM averageMarginalCostOfferMaker = new AverageMarginalCostOM(carrier,locations);
//		averageMarginalCostOfferMaker.setNetwork(network);
		avgMarginalCostOM.setNetwork(network);
		avgMarginalCostOM.setCarrierCostFunction(costFunction);
		avgMarginalCostOM.setCarrierPlanBuilder(new RAndRPickupAndDeliveryAndTimeClustersCarrierPlanBuilder(network));
		strategy.addStrategy(marginalCostOM, 0.0);
		strategy.addStrategy(avgMarginalCostOM, 1.0);
		
		carrierAgent.setOfferMaker(strategy);
		carrierAgent.setCarrierAgentTracker(tracker);
		carrierAgent.getCostMemoryListeners().add(new CostMemoryConsolePrinter());
		carrierAgent.setNetwork(network);
		return carrierAgent;
	}

	public void setOfferRecorder(OfferRecorder offerRecorder) {
		this.offerRecorder = offerRecorder;
		
	}

}
