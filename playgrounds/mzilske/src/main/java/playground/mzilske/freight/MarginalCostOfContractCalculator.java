package playground.mzilske.freight;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

public class MarginalCostOfContractCalculator implements CarrierCostCalculator {
	
	private Network network;
	
	private CostMemory costMemory;
	
	public MarginalCostOfContractCalculator(Network network, CostMemory costMemory) {
		super();
		this.network = network;
		this.costMemory = costMemory;
	}

	public void run(Id depotLocation, Collection<CarrierContract> contracts, Double totalCostsToAllocate) {
		double sumOfWeightedBeeLineDistances = getSumOfWeightedBeeLineDistances(depotLocation,contracts);
		double sumAvgCosts = 0.0;
		for(CarrierContract c : contracts){
			double avgMc = getAverageMarginalCosts(depotLocation, totalCostsToAllocate, sumOfWeightedBeeLineDistances, c.getShipment());
			sumAvgCosts += avgMc;
			costMemory.memorizeCost(c.getShipment().getFrom(), c.getShipment().getTo(), c.getShipment().getSize(), avgMc);
		}
		assertEqual(totalCostsToAllocate,sumAvgCosts);
	}

	private void assertEqual(Double totalCostsToAllocate, double sumAvgCosts) {
		if(sumAvgCosts >= totalCostsToAllocate*0.99 && sumAvgCosts <= totalCostsToAllocate*1.01){
			return;
		}
		else{
			throw new IllegalStateException(totalCostsToAllocate + " and " + sumAvgCosts + " should be equal");
		}
		
	}

	private double getAverageMarginalCosts(Id depotLocation, Double totalCosts, double sumCosts, Shipment shipment) {
		return (getBeeLineDistance(depotLocation,shipment.getFrom(),shipment.getTo()) * f(shipment.getSize()) / sumCosts) * totalCosts;
	}

	private double getSumOfWeightedBeeLineDistances(Id depotLocation, Collection<CarrierContract> contracts) {
		double sumCosts = 0.0;
		for(CarrierContract c : contracts){
			sumCosts += getBeeLineDistance(depotLocation,c.getShipment().getFrom(),c.getShipment().getTo()) * f(c.getShipment().getSize());
		}
		return sumCosts;
	}

	public double getBeeLineDistance(Id depotLocation, Id from, Id to) {
		Coord locCoord = findCoord(depotLocation);
		Coord fromCoord = findCoord(from);
		Coord toCoord = findCoord(to);
		double distance = CoordUtils.calcDistance(locCoord, fromCoord) +
				CoordUtils.calcDistance(fromCoord, toCoord) + CoordUtils.calcDistance(toCoord, locCoord);
		return distance;
	}

	private double f(int size) {
		return Math.log(size);
	}

	private Coord findCoord(Id location) {
		return network.getLinks().get(location).getCoord();
	}

}
