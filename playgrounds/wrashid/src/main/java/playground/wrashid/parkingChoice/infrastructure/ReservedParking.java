package playground.wrashid.parkingChoice.infrastructure;

import org.matsim.api.core.v01.Coord;

public class ReservedParking extends Parking {
	public ReservedParking(Coord coord, String attributes) {
		super(coord);
		this.attributes=attributes;
	}

	public String getAttributes() {
		return attributes;
	}

	String attributes=null;
	
	// LinkedList 
	// => provide instead a function, which does this?????
	
	// there are two types of reservation (static and dynamic).
	
	// static reservation: someone provides a list of vehicles (e.g. electic vehicle or attribute of person, e.g. disabled)
	
	// dynamic property: car sharing, if leg is beeing performed with mobility vehicle, then parking for that purpose needs to be in place
	
	// Somehow, people should be able to define different types of reserved parkings and then somewhere I should provide them with an interface
	// which they implement to decide, if that parking is available for that car trip or not.
	// => that interface must know, which person is it and which leg/act we are on (e.g. act start event).
	
	// => eigentlich kann ich mit person + act + facility + tageszeit anfangen -> francesco kann rausfinden, mit welchem vehicle jemand unterwegs ist...
	// => output: yes/no => general reservation parking scheme (ob der parkplatz considert werden soll für die parkplatz wahl oder nicht).
	// => reserved parkings haben low/0 search time (which should be set from outside).
	
	// field: type attribute (e.g. car sharing, el. vehicle, diabled peoples parking)
	// => make also available the attribute value for the decision.
	
	// => good would be to select all parkings in a region and then invoke the static reserved parkings method on that.
}
