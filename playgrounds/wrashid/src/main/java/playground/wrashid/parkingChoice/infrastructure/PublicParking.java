package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;

public class PublicParking extends ParkingImpl {

	public PublicParking(Coord coord) {
		super(coord);
	}

	String generalAttributes="";
	
	public void setGeneralAttributes(String metaData) {
		this.generalAttributes = metaData;
	}

	public String getGeneralAttributes(){
		return generalAttributes;
	}
	
}
