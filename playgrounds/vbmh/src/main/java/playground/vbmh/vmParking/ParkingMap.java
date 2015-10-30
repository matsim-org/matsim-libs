package playground.vbmh.vmParking;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.matsim.core.utils.collections.QuadTree;

/**
 * Keeps a list of all parking in the network; getPrivateParking() returns an available private parking space at
 * a specific facility; getPublicParking() returns available public parkign spaces in specific area.
 * 
 * !! Quadtree und evntl umkreis Suche hier her?
 * @author Valentin Bemetz & Moritz Hohenfellner
 *
 */



@XmlRootElement
public class ParkingMap {
	private static List<Parking> parkings = new LinkedList<Parking>();
	private static HashMap<String, Parking> parkingsHash;
	private static HashMap<Integer, Parking> idHash;
	private static QuadTree publicParktTree;

	@XmlElement(name = "Parking")
	public List<Parking> getParkings() {
		return parkings;
	}

	public void setParking(List<Parking> parkings) {

		ParkingMap.parkings = parkings;
	}

	public void addParking(Parking parking){
		ParkingMap.parkings.add(parking);
	}

	public void createSpots(PricingModels pricing){
		for (Parking parking : parkings){
			parking.createSpots(pricing);
		}
	}
	public void clearSpots(){
		for (Parking parking : parkings){
			parking.clearSpots();
		}
	}

	public void initHashMap(){
		//privates:
		parkingsHash = new HashMap<String, Parking>();
		for(Parking parking : parkings){
			if(parking.type.equals("private")){
				if(parking.facilityActType!=null && parking.facilityId!=null){
					parkingsHash.put(parking.facilityId+parking.facilityActType, parking);
				}
			}
		}
		//-----

		//publics:
		//

		//Karten begrenzung finden:
		double maxX=0;
		double maxY=0;
		double minX=0;
		double minY=0;
		for(Parking parking : parkings){
			if(parking.type.equals("public")){
				if(parking.coordinateX>maxX){
					maxX=parking.coordinateX;
				}
				if(parking.coordinateY>maxY){
					maxY=parking.coordinateY;
				}
				if(parking.coordinateX<minX){
					minX=parking.coordinateX;
				}
				if(parking.coordinateX<minY){
					minY=parking.coordinateY;
				}
			}
		}

		publicParktTree = new QuadTree(minX, minY, maxX, maxY);
		// Zum tree hinzufuegen
		for(Parking parking : parkings){
			if(parking.type.equals("public")){
				publicParktTree.put(parking.coordinateX, parking.coordinateY, parking);
			}
		}
		this.idHash= new HashMap<Integer, Parking>();
		for(Parking parking : parkings){
			this.idHash.put(parking.id, parking);
		}
	}





	public Parking getPrivateParking(String facId, String facActType){
		return parkingsHash.get(facId+facActType);
	}

	
	public Collection<Parking> getPublicParkings(double x, double y, double radius){
		return publicParktTree.getDisk(x, y, radius);
	}

	public Parking getParkingById(int id){
		return idHash.get(id);
	}

}
