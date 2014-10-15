package playground.balac.carsharing.preprocess.membership;

import java.util.Vector;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class PersonWithClosestStations
{
  private Id<Person> id;
  Vector<Station> orderedClosestStationsWork = new Vector();
  Vector<Station> orderedClosestStationsHome = new Vector();
  private Double accessibilityHome;
  private Double accessibilityWork;
  private Coord coordWork;
  private Coord coordHome;

  public Id<Person> getId()
  {
    return this.id;
  }
  public Coord getCoordWork() {
    return this.coordWork;
  }
  public void setCoordWork(Coord coordWork) {
    this.coordWork = coordWork;
  }
  public Coord getCoordHome() {
    return this.coordHome;
  }
  public void setCoordHome(Coord coordHome) {
    this.coordHome = coordHome;
  }
  public void setId(Id<Person> id) {
    this.id = id;
  }

  public Vector<Station> getOrderedClosestStationsWork() {
    return this.orderedClosestStationsWork;
  }

  public void setOrderedClosestStationsWork(Vector<Station> orderedClosestStationsWork) {
    this.orderedClosestStationsWork = orderedClosestStationsWork;
  }
  public Vector<Station> getOrderedClosestStationsHome() {
    return this.orderedClosestStationsHome;
  }

  public void setOrderedClosestStationsHome(Vector<Station> orderedClosestStationsHome) {
    this.orderedClosestStationsHome = orderedClosestStationsHome;
  }
  public Double getAccessibilityHome() {
    return this.accessibilityHome;
  }
  public void setAccessibilityHome(Double accessibilityHome) {
    this.accessibilityHome = accessibilityHome;
  }
  public Double getAccessibilityWork() {
    return this.accessibilityWork;
  }
  public void setAccessibilityWork(Double accessibilityWork) {
    this.accessibilityWork = accessibilityWork;
  }
}