package playground.wrashid.parkingChoice.infrastructure;

import java.util.LinkedList;
import java.util.Stack;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.facilities.ActivityFacility;
import org.xml.sax.Attributes;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;

public class FlatParkingFormatReaderV1 extends MatsimXmlParser implements MatsimSomeReader {

	LinkedList<PParking> parkings=new LinkedList<PParking>();
	
	
	public LinkedList<PParking> getParkings() {
		return parkings;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (name.equalsIgnoreCase("parking")){
			PParking parking=createParking(atts);
			parkings.add(parking);
		}
	}
	
	private PParking createParking(Attributes atts){
		String parkingType=atts.getValue("type");
		String parkingId=atts.getValue("id");
		double x=new Double(atts.getValue("x"));
		double y=new Double(atts.getValue("y"));
		double capacity=new Double(atts.getValue("capacity"));
		Coord coord= new Coord(x, y);
		
		if (parkingType.equalsIgnoreCase("public")){
			PublicParking publicParking=new PublicParking(coord);
			publicParking.setParkingId(Id.create(parkingId, PParking.class));
			publicParking.setType(parkingType);
			publicParking.setGeneralAttributes(atts.getValue("generalAttributs"));
			publicParking.setMaxCapacity(capacity);
			return publicParking;
		}
		
		if (parkingType.equalsIgnoreCase("private")){
			
			Id<ActivityFacility> facilityId=Id.create(atts.getValue("facilityId"), ActivityFacility.class);
			String actType=atts.getValue("actType");
			ActInfo actInfo=new ActInfo(facilityId, actType);
			PrivateParking privateParking=new PrivateParking(coord,actInfo);
			privateParking.setType(parkingType);
			privateParking.setMaxCapacity(capacity);
			privateParking.setParkingId(Id.create(parkingId, PParking.class));
			return privateParking;
		}
		
		if (parkingType.equalsIgnoreCase("reserved")){
			ReservedParking reservedParking=new ReservedParking(coord, atts.getValue("generalAttributs"));
			reservedParking.setType(parkingType);
			reservedParking.setMaxCapacity(capacity);
			reservedParking.setParkingId(Id.create(parkingId, PParking.class));
			return reservedParking;
		}
		
		if (parkingType.equalsIgnoreCase("preferred")){
			PreferredParking preferredParking=new PreferredParking(coord, atts.getValue("generalAttributs"));
			preferredParking.setType(parkingType);
			preferredParking.setMaxCapacity(capacity);
			preferredParking.setParkingId(Id.create(parkingId, PParking.class));
			return preferredParking;
		}
		
		DebugLib.stopSystemAndReportInconsistency("parkingType: '" + parkingType + "' unknown.");
		
		return null;
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}
	
	
}
