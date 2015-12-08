package playground.sergioo.hits2012.stages;

public class TaxiStage extends WaitStage {

	private final int numPassengers;
	private final double taxiFare;
	private final boolean taxiReimbursment;
	
	public TaxiStage(String id, String mode, double walkTime, double inVehicleTime,
			double lastWalkTime, double waitTime, int numPassengers,
			double taxiFare, boolean taxiReimbursment) {
		super(id, mode, walkTime, inVehicleTime, lastWalkTime, waitTime, "TAXI");
		this.numPassengers = numPassengers;
		this.taxiFare = taxiFare;
		this.taxiReimbursment = taxiReimbursment;
	}

	public int getNumPassengers() {
		return numPassengers;
	}

	public double getTaxiFare() {
		return taxiFare;
	}

	public boolean isTaxiReimbursment() {
		return taxiReimbursment;
	}

}
