package vwExamples.utils.CreateShiftingScenario;


import org.matsim.core.router.TripStructureUtils.Subtour;

public interface SubTourValidator {
	
	boolean isValidSubTour(Subtour subTour);

}
