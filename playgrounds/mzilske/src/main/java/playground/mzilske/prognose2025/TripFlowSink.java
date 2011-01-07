package playground.mzilske.prognose2025;

public interface TripFlowSink {
	
	public void process(Zone quelle, Zone ziel, int quantity, String mode, String destinationActivityType, double travelTimeOffset);

	public void complete();

}
