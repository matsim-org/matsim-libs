package vwExamples.utils.tripAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ModalSplitSegment {
	Set<String> acceptedMainModes;
	int SegmentClassNr;
	HashMap<String,ArrayList<Double>> mode2TripDistance;
	

	ModalSplitSegment(int i, Set<String> acceptedMainModes)

	{
		this.SegmentClassNr = i;
		this.acceptedMainModes = acceptedMainModes;
		this.mode2TripDistance = new HashMap<String, ArrayList<Double>>();

	}
	
	int getNumberOfTripsPerMode(String mode)
	{
		if (mode2TripDistance.containsKey(mode))
		{
			return mode2TripDistance.get(mode).size();
			
		}
		return 0;
		
	}
	
//	int getSumOfAllTrip()
//	{
//		if (mode2TripDistance.containsKey(mode))
//		{
//			return mode2TripDistance.get(mode).size();
//			
//		}
//		return 0;
//		
//	}
	
	

}
