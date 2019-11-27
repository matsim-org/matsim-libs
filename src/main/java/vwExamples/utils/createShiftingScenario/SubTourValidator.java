package vwExamples.utils.createShiftingScenario;


import org.matsim.api.core.v01.Coord;
import org.matsim.core.router.TripStructureUtils.Subtour;

public interface SubTourValidator {
	
	boolean isValidSubTour(Subtour subTour);
	boolean isWithinZone(Coord coord);

}
