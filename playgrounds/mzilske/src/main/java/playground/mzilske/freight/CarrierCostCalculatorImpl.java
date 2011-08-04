package playground.mzilske.freight;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

public class CarrierCostCalculatorImpl implements CarrierCostCalculator {
	
	private Network network;
	
	public CarrierCostCalculatorImpl(Network network) {
		super();
		this.network = network;
	}

	public void run(CarrierVehicle carrierVehicle, Collection<Contract> contracts, CostMemory costMemory, Double totalCosts) {
		double sumCosts = getSumOfWeights(carrierVehicle.getLocation(),contracts);
		for(Contract c : contracts){
			double avgMc = getAvgMC(carrierVehicle.getLocation(), totalCosts, sumCosts, c.getShipment());
			costMemory.memorizeCost(c.getShipment().getFrom(), c.getShipment().getTo(), c.getShipment().getSize(), avgMc);
		}
	}

	public double getAvgMC(Id depotLocation, Double totalCosts, double sumCosts, Shipment shipment) {
		return getBeeLineDistance(depotLocation,shipment) * f(shipment.getSize())/sumCosts * totalCosts;
	}

	public double getSumOfWeights(Id location, Collection<Contract> contracts) {
		double sumCosts = 0.0;
		for(Contract c : contracts){
			sumCosts += getBeeLineDistance(location,c.getShipment()) * f(c.getShipment().getSize());
		}
		return sumCosts;
	}

	private double f(int size) {
		return Math.log(size);
	}

	private double getBeeLineDistance(Id depotLocation, Shipment shipment) {
		Coord locCoord = findCoord(depotLocation);
		Coord from = findCoord(shipment.getFrom());
		Coord to = findCoord(shipment.getTo());
		double distance = CoordUtils.calcDistance(locCoord, from) +
				CoordUtils.calcDistance(from, to) + CoordUtils.calcDistance(to, locCoord);
		return distance;
	}

	private Coord findCoord(Id location) {
		return network.getLinks().get(location).getCoord();
	}

}
