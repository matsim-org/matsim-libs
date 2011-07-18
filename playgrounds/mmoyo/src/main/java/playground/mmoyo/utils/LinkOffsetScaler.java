package playground.mmoyo.utils;

import java.io.File;
import java.io.IOException;

import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.PtBseLinkCostOffsetsXMLFileIO;
import cadyts.utilities.misc.DynamicData;

public class LinkOffsetScaler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String transitScheduleFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		String linkCostOffsetFilePath = "../../input/juli/elMejor/500.linkCostOffsets.xml";
		
		DataLoader dLoader = new DataLoader();
		TransitSchedule schedule = dLoader.readTransitSchedule(netFilePath, transitScheduleFilePath);
		
		PtBseLinkCostOffsetsXMLFileIO reader = new PtBseLinkCostOffsetsXMLFileIO (schedule);
		DynamicData<TransitStopFacility> stopOffsets = reader.read(linkCostOffsetFilePath);

		for (TransitStopFacility stopFacility : stopOffsets.keySet()){
			for (int i=0;i<24;i++){
				double linkOffsetValue = stopOffsets.getBinValue(stopFacility, i);
				stopOffsets.put(stopFacility, i, linkOffsetValue/10);
			}
		}
	
		//write
		// the remaining material is, in my view, "just" output:
		File file = new File(linkCostOffsetFilePath);
		String filename = file.getParent() + "/" + file.getName() + "scaled.xml"; 
		try {
			PtBseLinkCostOffsetsXMLFileIO ptBseLinkCostOffsetsXMLFileIO = new PtBseLinkCostOffsetsXMLFileIO( schedule );
			ptBseLinkCostOffsetsXMLFileIO.write( filename , stopOffsets);
			ptBseLinkCostOffsetsXMLFileIO = null;
		}catch(IOException e) {
			e.printStackTrace();
		}
	
	
	
	}

}
