package playground.mmoyo.utils.calibration;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.pt.TransitStopFacilityLookUp;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.analysis.counts.reader.CountsReader;
import playground.mmoyo.utils.DataLoader;
import cadyts.utilities.misc.DynamicData;

public class LinkOffsetValidator {
	final static String SEP = " ";

	/**comparison approximation between link offsets and data occupancy*/
	public LinkOffsetValidator(){

	}

	public static void main(String[] args) {
		String trScheduleFile= "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String linkCostOffsetFilePath = "../../input/juli/completeRun/500.linkCostOffsets.xml";
		String ocuppFilePath = "../../input/juli/completeRun/500.simBseCountCompareOccupancy.txt";

		DataLoader dataLoader= new DataLoader();
		TransitSchedule schedule = dataLoader.readTransitSchedule(trScheduleFile);

//		CadytsPtLinkCostOffsetsXMLFileIO reader = new CadytsPtLinkCostOffsetsXMLFileIO (schedule);
		CadytsCostOffsetsXMLFileIO<TransitStopFacility> reader 
		   = new CadytsCostOffsetsXMLFileIO<TransitStopFacility> (new TransitStopFacilityLookUp(schedule), TransitStopFacility.class);

		DynamicData<TransitStopFacility> stopOffsets = reader.read(linkCostOffsetFilePath);

		/////create a PtBseOccupancyAnalyzer with simulated occupancy values
		CountsReader countsReader = new CountsReader (ocuppFilePath);
		for (Id stopId : countsReader.getStopsIds()){
			double[] dblSimValues = countsReader.getSimulatedScaled(stopId);
			double[] dblRealValues = countsReader.getRealValues(stopId);

			System.out.println("\n" + stopId);
			for (int i=0;i<24;i++){  //convert from double to integer
				TransitStopFacility stopFacility = schedule.getFacilities().get(stopId);
				double linkOffValue = stopOffsets.getBinValue(stopFacility, i);
				System.out.println(dblRealValues[i] + SEP + dblSimValues[i] + SEP + linkOffValue) ;
			}
		}

	}
}
