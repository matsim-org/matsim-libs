package playground.balac.carsharing.preprocess.membership;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class Station
{
  private Coord coord;
  private Id<Station> id;
  private int cars;

  public int getCars()
  {
    return this.cars;
  }
  public void setCars(int cars) {
    this.cars = cars;
  }
  public Coord getCoord() {
    return this.coord;
  }
  public void setCoord(Coord coord) {
    this.coord = coord;
  }
  public Id<Station> getId() {
    return this.id;
  }
  public void setId(Id<Station> id) {
    this.id = id;
  }
}