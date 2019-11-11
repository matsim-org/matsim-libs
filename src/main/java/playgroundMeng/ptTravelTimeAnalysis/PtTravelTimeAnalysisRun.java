package playgroundMeng.ptTravelTimeAnalysis;

import com.google.inject.Guice;
import com.google.inject.Injector;

import playgroundMeng.ptAccessabilityAnalysis.activitiesAnalysis.Trip;
import playgroundMeng.ptAccessabilityAnalysis.linksCategoryAnalysis.LinksPtInfoCollector;
import playgroundMeng.ptAccessabilityAnalysis.run.PtAccessabilityModule;

public class PtTravelTimeAnalysisRun {
	public static void main(String[] args) throws Exception {
		 Injector injector = Guice.createInjector(new PtTravelTimeAnalysisRunModule());
		 RatioCaculator ratioCaculator = injector.getInstance(RatioCaculator.class);
		 for(Trip trip: ratioCaculator.getTrips()) {
			 System.out.println(trip.toString() +" "+ trip.getCarTravelInfo().toString() +" "+ trip.getPtTraveInfo().toString());
		 }
		 
//		 NetworkChangeEventMerge networkChangeEventMerge = injector.getInstance(NetworkChangeEventMerge.class);
//		 networkChangeEventMerge.merge();
//		 
	}
}
