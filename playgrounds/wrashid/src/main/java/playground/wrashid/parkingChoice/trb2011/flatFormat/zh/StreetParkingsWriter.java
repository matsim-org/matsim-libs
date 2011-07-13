package playground.wrashid.parkingChoice.trb2011.flatFormat.zh;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;

import playground.wrashid.lib.GeneralLib;

public class StreetParkingsWriter extends MatsimXmlWriter {

	public void writeFile(final String filename, String source, ActivityFacilitiesImpl streetParkingFacilities) {
		String dtd = "./test/input/playground/wrashid/parkingChoice/infrastructure/flatParkingFormat_v1.dtd";

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("flatParkings", dtd);

			this.writer.write("<!-- data source: "+ source +" -->\n\n");
			
			this.writer.write("<flatParkings>\n");
			
			writeFacilities(streetParkingFacilities, this.writer);
			
			this.writer.write("</flatParkings>\n");
			
			this.writer.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void writeFacilities(ActivityFacilitiesImpl streetParkingFacilities, BufferedWriter writer) throws IOException {
		for (Id facilityId:streetParkingFacilities.getFacilities().keySet()){
			ActivityFacilityImpl facilityImpl=(ActivityFacilityImpl) streetParkingFacilities.getFacilities().get(facilityId);
			
			Map<String, ActivityOption> activityOptions = facilityImpl.getActivityOptions();
			
			writer.write("\t<parking type=\"public\"");
			writer.write(" id=\"stp-"+ facilityId +"\"");
			writer.write(" x=\""+ facilityImpl.getCoord().getX() +"\"");
			writer.write(" y=\""+ facilityImpl.getCoord().getY() +"\"");
			writer.write(" capacity=\""+ Math.round(activityOptions.get("parking").getCapacity()) +"\"");
			writer.write("/>\n");
		}
		
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcePath = "ETH/static data/parking/zürich city/Strassenparkplaetze/street parkings old - 2007 - aufbereitet/streetpark_facilities.xml";
		ActivityFacilitiesImpl streetParkingFacilities = GeneralLib.readActivityFacilities("C:/data/My Dropbox/" + sourcePath);
		
		StreetParkingsWriter streetParkingWriter=new StreetParkingsWriter();
		streetParkingWriter.writeFile("C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/streetParkings.xml", sourcePath,streetParkingFacilities);

	}

}
