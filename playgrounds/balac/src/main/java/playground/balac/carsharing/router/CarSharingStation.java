package playground.balac.carsharing.router;

import java.util.Map;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.Facility;
import org.matsim.core.network.LinkImpl;

public class CarSharingStation
  implements Facility
{
  private final Id id;
  private Coord coord;
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

  public Map<String, Object> getCustomAttributes()
  {
    return null;
  }

  public Id getLinkId()
  {
    return this.link.getId();
  }

  public void setCoord(Coord coord2) {
    this.coord = coord2;
  }

  public void setLink(LinkImpl stationLink) {
    this.link = stationLink;
  }
}