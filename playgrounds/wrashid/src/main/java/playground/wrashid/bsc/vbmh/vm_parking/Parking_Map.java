package playground.wrashid.bsc.vbmh.vm_parking;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Keeps a list of all parking in the network.
 * 
 * 
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */



@XmlRootElement
public class Parking_Map {
	private static List<Parking> parkings = new LinkedList<Parking>();

	@XmlElement(name = "Parking")
	public List<Parking> getParkings() {
		return parkings;
	}

	public void setParking(List<Parking> parkings) {

		Parking_Map.parkings = parkings;
	}
	
	public void addParking(Parking parking){
		Parking_Map.parkings.add(parking);
	}
	
	public void create_spots(){
		for (Parking parking : parkings){
			parking.create_spots();
		}
	}
	public void clear_spots(){
		for (Parking parking : parkings){
			parking.create_spots();
		}
	}
	
}
