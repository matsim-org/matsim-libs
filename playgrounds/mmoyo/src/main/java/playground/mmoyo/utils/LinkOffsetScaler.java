package playground.mmoyo.utils;

import java.io.File;
import java.io.IOException;

import org.matsim.contrib.cadyts.general.CadytsCostOffsetsXMLFileIO;
import org.matsim.contrib.cadyts.pt.TransitStopFacilityLookUp;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import cadyts.utilities.misc.DynamicData;

public class LinkOffsetScaler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String transitScheduleFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String linkCostOffsetFilePath = "../../input/juli/elMejor/500.linkCostOffsets.xml";

		DataLoader dLoader = new DataLoader();
		TransitSchedule schedule = dLoader.readTransitSchedule(transitScheduleFilePath);

//		CadytsPtLinkCostOffsetsXMLFileIO reader = new CadytsPtLinkCostOffsetsXMLFileIO (schedule);
		CadytsCostOffsetsXMLFileIO<TransitStopFacility> reader 
		   = new CadytsCostOffsetsXMLFileIO<TransitStopFacility> (new TransitStopFacilityLookUp(schedule), TransitStopFacility.class);

		DynamicData<TransitStopFacility> stopOffsets = reader.read(linkCostOffsetFilePath);

		for (TransitStopFacility stopFacility : stopOffsets.keySet()){
			for (int i=0;i<24;i++){
				double linkOffsetValue = stopOffsets.getBinValue(stopFacility, i);
				stopOffsets.put(stopFacility, i, linkOffsetValue/10);
			}
		}

		//write
		File file = new File(linkCostOffsetFilePath);
		String filename = file.getParent() + "/" + file.getName() + "scaled.xml";
		try {
//			CadytsPtLinkCostOffsetsXMLFileIO cadytsPtLinkCostOffsetsXMLFileIO = new CadytsPtLinkCostOffsetsXMLFileIO( schedule );
			CadytsCostOffsetsXMLFileIO<TransitStopFacility> cadytsPtLinkCostOffsetsXMLFileIO 
			   = new CadytsCostOffsetsXMLFileIO<TransitStopFacility> (new TransitStopFacilityLookUp(schedule), TransitStopFacility.class);

			cadytsPtLinkCostOffsetsXMLFileIO.write( filename , stopOffsets);
			cadytsPtLinkCostOffsetsXMLFileIO = null;
		}catch(IOException e) {
			e.printStackTrace();
		}

	}
}
