package city2000w;

public class TLCostFunction {

	private double costOfCapital = 0.1;
	
	public double getTLC(double unitValueOfShipment, int lotsize, double transportCost) {
		return lotsize/2*unitValueOfShipment*costOfCapital + transportCost;
	}

}
