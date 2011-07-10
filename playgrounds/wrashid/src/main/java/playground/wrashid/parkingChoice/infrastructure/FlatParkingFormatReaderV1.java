package playground.wrashid.parkingChoice.infrastructure;

import java.util.LinkedList;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

import playground.wrashid.lib.DebugLib;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;

public class FlatParkingFormatReaderV1 extends MatsimXmlParser implements MatsimSomeReader {

	LinkedList<Parking> parkings=new LinkedList<Parking>();
	
	
	public LinkedList<Parking> getParkings() {
		return parkings;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equalsIgnoreCase("parking")){
			Parking parking=createParking(atts);
			parkings.add(parking);
		}
	}
	
	private Parking createParking(Attributes atts){
		String parkingType=atts.getValue("type");
		double x=new Double(atts.getValue("x"));
		double y=new Double(atts.getValue("y"));
		int capacity=new Integer(atts.getValue("capacity"));
		Coord coord=new CoordImpl(x, y);
		
		if (parkingType.equalsIgnoreCase("public")){
			PublicParking publicParking=new PublicParking(coord);
			publicParking.setGeneralAttributes(atts.getValue("generalAttributs"));
			publicParking.setMaxCapacity(capacity);
			return publicParking;
		}
		
		if (parkingType.equalsIgnoreCase("private")){
			Id facilityId=new IdImpl(atts.getValue("facilityId"));
			String actType=atts.getValue("actType");
			ActInfo actInfo=new ActInfo(facilityId, actType);
			PrivateParking privateParking=new PrivateParking(coord,actInfo);
			privateParking.setMaxCapacity(capacity);
			return privateParking;
		}
		
		if (parkingType.equalsIgnoreCase("reserved")){
			ReservedParking reservedParking=new ReservedParking(coord, atts.getValue("generalAttributs"));
			reservedParking.setMaxCapacity(capacity);
			return reservedParking;
		}
		
		if (parkingType.equalsIgnoreCase("preferred")){
			PreferredParking reservedParking=new PreferredParking(coord, atts.getValue("generalAttributs"));
			reservedParking.setMaxCapacity(capacity);
			return reservedParking;
		}
		
		DebugLib.stopSystemAndReportInconsistency("parkingType: '" + parkingType + "' unknown.");
		
		return null;
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		// TODO Auto-generated method stub
		
	}
	
	

	
	
}
