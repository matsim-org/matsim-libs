package playground.mzilske.freight.vrp;



public interface Constraints {

	public boolean tourDoesNotViolateConstraints(VehicleTour newTour, Costs costs);

}