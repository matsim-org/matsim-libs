package playground.artemc.crowding.newScoringFunctions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;


public class ScoreListener implements IterationEndsListener {
	
	private ScoreTracker scoreTracker;
	private Set<Integer> iterations;
	private Map<Integer, Double> crowdednessUtilityProIteration;
	private Map<Integer, Double> crowdednessExternalitiesProIteration;
	private Map<Integer, Double> inVehicleTimeDelayExternalitiesProIteration;
	private Map<Integer, Double> capacityConstraintsExternalitiesProIteration;
	private Map<Integer, Double> moneyPaidProIteration;
	
	public ScoreListener(ScoreTracker scoreTracker) {
		this.scoreTracker = scoreTracker;
		this.iterations = new HashSet<Integer>();
		this.crowdednessUtilityProIteration = new HashMap<Integer, Double>();
		this.crowdednessExternalitiesProIteration = new HashMap<Integer, Double>();
		this.inVehicleTimeDelayExternalitiesProIteration = new HashMap<Integer, Double>();
		this.capacityConstraintsExternalitiesProIteration = new HashMap<Integer, Double>();
		this.moneyPaidProIteration = new HashMap<Integer, Double>();
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
			
			double totalCrowdednessUtility = scoreTracker.getTotalCrowdednessUtility();
			double totalCrowdednessExternalityCharges = scoreTracker.getTotalCrowdednessExternalityCharges();
			double totalInVehTimeDelayExternalityCharge = scoreTracker.getTotalInVehicleTimeDelayExternalityCharges();
			double totalCapacityConstraintsExternalityCharge = scoreTracker.getTotalCapacityConstraintsExternalityCharges();
			double totalMoneyPaid = scoreTracker.getTotalMoneyPaid();
			int iterationNumber = event.getIteration();
			
			iterations.add(iterationNumber);
			crowdednessUtilityProIteration.put(iterationNumber, totalCrowdednessUtility);
			crowdednessExternalitiesProIteration.put(iterationNumber, totalCrowdednessExternalityCharges);
			inVehicleTimeDelayExternalitiesProIteration.put(iterationNumber, totalInVehTimeDelayExternalityCharge);
			capacityConstraintsExternalitiesProIteration.put(iterationNumber, totalCapacityConstraintsExternalityCharge);
			moneyPaidProIteration.put(iterationNumber, totalMoneyPaid);
			//System.out.println("ItNum " + iterationNumber + ", crowd " + totalCrowdednessUtility);		
	}

	public void finish(int iteration) {

		double totalCrowdednessUtility = scoreTracker.getTotalCrowdednessUtility();
		double totalCrowdednessExternalityCharges = scoreTracker.getTotalCrowdednessExternalityCharges();
		double totalInVehTimeDelayExternalityCharge = scoreTracker.getTotalInVehicleTimeDelayExternalityCharges();
		double totalCapacityConstraintsExternalityCharge = scoreTracker.getTotalCapacityConstraintsExternalityCharges();
		double totalMoneyPaid = scoreTracker.getTotalMoneyPaid();
		int iterationNumber = iteration;

		iterations.add(iterationNumber);
		crowdednessUtilityProIteration.put(iterationNumber, totalCrowdednessUtility);
		crowdednessExternalitiesProIteration.put(iterationNumber, totalCrowdednessExternalityCharges);
		inVehicleTimeDelayExternalitiesProIteration.put(iterationNumber, totalInVehTimeDelayExternalityCharge);
		capacityConstraintsExternalitiesProIteration.put(iterationNumber, totalCapacityConstraintsExternalityCharge);
		moneyPaidProIteration.put(iterationNumber, totalMoneyPaid);
		//System.out.println("ItNum " + iterationNumber + ", crowd " + totalCrowdednessUtility);
	}

	public Set<Integer> getIterations() {
		return iterations;
	}
	
	public Map<Integer, Double> getCrowdednessProIteration() {
		return crowdednessUtilityProIteration;
	}

	public Map<Integer, Double> getCrowdednessExternalitiesProIteration() {
		return crowdednessExternalitiesProIteration;
	}

	public Map<Integer, Double> getInVehicleTimeDelayExternalitiesProIteration() {
		return inVehicleTimeDelayExternalitiesProIteration;
	}

	public Map<Integer, Double> getCapacityConstraintsExternalitiesProIteration() {
		return capacityConstraintsExternalitiesProIteration;
	}

	public Map<Integer, Double> getMoneyPaidProIteration() {
		return moneyPaidProIteration;
	}

	
}

