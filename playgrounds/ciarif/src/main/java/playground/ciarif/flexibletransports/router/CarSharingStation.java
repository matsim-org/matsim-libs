package playground.ciarif.flexibletransports.router;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.LinkImpl;
import org.matsim.facilities.Facility;

public class CarSharingStation implements Facility
{
	  private final Id<CarSharingStation> id;
	  private final Coord coord;
	  private int cars = 0;
	  private LinkImpl link;
	
	CarSharingStation(Id<CarSharingStation> id, Coord coord, LinkImpl link)
	{
	    this.id = id;
	    this.coord = coord;
	    this.link = link;
	}
	  
	CarSharingStation(Id<CarSharingStation> id, Coord coord, LinkImpl link, int cars)
	{
	    this.id = id;
	    this.coord = coord;
	    this.link = link;
	    this.cars = cars;
	}
	
	@Override
	public Id<CarSharingStation> getId() {
	    return this.id;
	}
	
	@Override
	public Coord getCoord() {
	    return this.coord;
	}
	
	public LinkImpl getLink() {
	    return this.link;
	}
	  
	public int getCars() {
		return this.cars;
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Id<Link> getLinkId() {
		// TODO Auto-generated method stub
		return null;
	}
}
