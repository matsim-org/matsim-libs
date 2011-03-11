package playground.mzilske.prognose2025;

public interface TripFlowSink {
	
	public void process(Zone quelle, Zone ziel, int quantity, String mode, String destinationActivityType, double departureTimeOffset);

	public void complete();

}
