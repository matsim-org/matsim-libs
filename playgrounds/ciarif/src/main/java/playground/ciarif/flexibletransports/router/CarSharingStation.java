package playground.ciarif.flexibletransports.router;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.LinkImpl;

public class CarSharingStation
{
  private final Id id;
  private final Coord coord;
  private int cars = 0;
  private LinkImpl link;

  CarSharingStation(Id id, Coord coord, LinkImpl link)
  {
    this.id = id;
    this.coord = coord;
    this.link = link;
  }
  
  CarSharingStation(Id id, Coord coord, LinkImpl link, int cars)
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
}
