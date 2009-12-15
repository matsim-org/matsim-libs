package playground.anhorni.locationchoice.analysis.facilities.facilityLoad;

import org.matsim.core.gbl.Gbl;

public class AnalyzeFacilityLoad {
		
	public static void main(final String[] args) {
		Gbl.startMeasurement();
		final AnalyzeFacilityLoad analyzer = new AnalyzeFacilityLoad();
		analyzer.run();
		Gbl.printElapsedTime();
	}	
		
	public void run() {
		FacilityLoadReader reader = new FacilityLoadReader();
		reader.readFiles();
		
		FacilityLoadsWriter writer = new FacilityLoadsWriter();
		writer.write(reader.getFacilityLoads());		
	}

}
