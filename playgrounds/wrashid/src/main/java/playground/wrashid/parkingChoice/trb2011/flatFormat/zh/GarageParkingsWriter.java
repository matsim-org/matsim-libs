package playground.wrashid.parkingChoice.trb2011.flatFormat.zh;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;

public class GarageParkingsWriter extends MatsimXmlWriter {

	public void writeFile(final String filename, String source, StringMatrix garageParkingData) {
		String dtd = "./test/input/playground/wrashid/parkingChoice/infrastructure/flatParkingFormat_v1.dtd";

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("flatParkings", dtd);

			this.writer.write("<!-- data source: "+ source +" -->\n\n");
			
			this.writer.write("<flatParkings>\n");
			
			writeFacilities(garageParkingData, this.writer);
			
			this.writer.write("</flatParkings>\n");
			
			this.writer.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void writeFacilities(StringMatrix garageParkingData, BufferedWriter writer) throws IOException {
		for (int i=1;i<garageParkingData.getNumberOfRows();i++){
			writer.write("\t<parking type=\"public\"");
			writer.write(" id=\"gp-" + i +"\"");
			writer.write(" x=\""+ garageParkingData.getDouble(i, 0) +"\"");
			writer.write(" y=\""+ garageParkingData.getDouble(i, 1) +"\"");
			writer.write(" capacity=\""+ Integer.parseInt(garageParkingData.getString(i, 2)) +"\"");
			writer.write("/>\n");
		}
		
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcePath = "ETH/static data/parking/zürich city/Parkhaeuser/parkhäuser.txt";

		StringMatrix garageParkingData = GeneralLib.readStringMatrix("c:/data/My Dropbox/" + sourcePath);
		
		GarageParkingsWriter garageParkingsWriter=new GarageParkingsWriter();
		garageParkingsWriter.writeFile("C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/garageParkings.xml", sourcePath,garageParkingData);

	}
	
}
