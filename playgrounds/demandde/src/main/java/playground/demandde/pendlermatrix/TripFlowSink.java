package playground.demandde.pendlermatrix;

public interface TripFlowSink {
	
	public void process(Zone quelle, Zone ziel, int quantity, String mode, String destinationActivityType, double departureTimeOffset);

	public void complete();

}
