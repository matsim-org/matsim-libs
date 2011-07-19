package playground.wrashid.parkingChoice.trb2011.flatFormat.zhCity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlWriter;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.lib.GeneralLib;
import playground.wrashid.lib.obj.StringMatrix;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;

public class PrivateParkingsOutdoorWriter_v0 extends MatsimXmlWriter {

	private static QuadTree<ActivityFacilityImpl> facilitiesQuadTree;
	private static LinkedList<PrivateParking> privateParkings;
	
	public static void main(String[] args) {
		String sourcePathPrivateParkingsOutdoor = "ETH/static data/parking/zürich city/Private Parkplätze/PrivateParkingIndoor.txt";
		StringMatrix privateParkingOutdoorFile = GeneralLib.readStringMatrix("c:/data/My Dropbox/" + sourcePathPrivateParkingsOutdoor);

		facilitiesQuadTree = PrivateParkingsIndoorWriter_v0.getFacilitiesQuadTree();
		
		privateParkings = new LinkedList<PrivateParking>();
		
		for (int i=1;i<privateParkingOutdoorFile.getNumberOfRows();i++){
			int parkingCapacity= privateParkingOutdoorFile.getInteger(i, 3);
			Coord coord=new CoordImpl(privateParkingOutdoorFile.getDouble(i, 1),privateParkingOutdoorFile.getDouble(i, 2));
		
			if (parkingCapacity>0){
				PrivateParkingsIndoorWriter_v0.assignParkingCapacityToClosestFacility(coord, parkingCapacity, facilitiesQuadTree, privateParkings);
			}
		}
		
		PrivateParkingsOutdoorWriter_v0 privateParkingsWriter=new PrivateParkingsOutdoorWriter_v0();
		privateParkingsWriter.writeFile("C:/data/My Dropbox/ETH/Projekte/TRB Aug 2011/parkings/flat/privateParkingsOutdoor.xml", sourcePathPrivateParkingsOutdoor);
	}
	
	public void writeFile(final String filename, String source) {
		String dtd = "./test/input/playground/wrashid/parkingChoice/infrastructure/flatParkingFormat_v1.dtd";

		try {
			openFile(filename);
			writeXmlHead();
			writeDoctype("flatParkings", dtd);

			this.writer.write("<!-- data source: "+ source +" -->\n\n");
			
			this.writer.write("<flatParkings>\n");
			
			createPrivateParkings(this.writer);
			
			this.writer.write("</flatParkings>\n");
			
			this.writer.close();
		}
		catch (IOException e) {
			Gbl.errorMsg(e);
		}
	}
	
	private void createPrivateParkings(BufferedWriter writer) throws IOException {
		for (int i=0;i<privateParkings.size();i++){
			if (privateParkings.get(i).getCapacity()<1){
				DebugLib.stopSystemAndReportInconsistency();
			}
			
			writer.write("\t<parking type=\"private\"");
			writer.write(" id=\"ppOutdoor-" + i +"\"");
			writer.write(" x=\""+ privateParkings.get(i).getCoord().getX() +"\"");
			writer.write(" y=\""+ privateParkings.get(i).getCoord().getY() +"\"");
			writer.write(" capacity=\""+ privateParkings.get(i).getCapacity() +"\"");
			writer.write(" facilityId=\""+ privateParkings.get(i).getActInfo().getFacilityId() +"\"");
			writer.write(" actType=\""+ privateParkings.get(i).getActInfo().getActType() +"\"");
			writer.write("/>\n");
		}
		
	}
}