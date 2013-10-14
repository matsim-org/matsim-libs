package playground.balac.onewaycarsharing.router;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.api.experimental.facilities.Facility;

public class CarSharingStation implements Facility
{
	  private final Id id;
	  private final Coord coord;
	  private int cars = 0;
	  private LinkImpl link;
	
	public CarSharingStation(Id id, Coord coord, LinkImpl link)
	{
	    this.id = id;
	    this.coord = coord;
	    this.link = link;
	}
	  
	public CarSharingStation(Id id, Coord coord, LinkImpl link, int cars)
	{
	    this.id = id;
	    this.coord = coord;
	    this.link = link;
	    this.cars = cars;
	}
	
	public Id getId() {
	    return this.id;
	}
	
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
	public Id getLinkId() {
		// TODO Auto-generated method stub
		return link.getId();
	}
}
