package playground.jbischoff.carsharing.data;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordImpl;

class CarsharingVehicleData
{
  private Id<CarsharingVehicleData> vid;
  private Coord location;
  private long mileage;
  private double fuel;
  private long time;
  private String provider;
  private String license;
  
  CarsharingVehicleData(Id<CarsharingVehicleData> vid, String license, String lati, String longi, String mileage, String fuel, String provider)
  {
    this.vid = vid;
    this.license = license;
    this.location = new CoordImpl(lati, longi);
    this.mileage = Long.parseLong(mileage);
    this.fuel = Double.parseDouble(fuel);
    this.time = System.currentTimeMillis();
    this.provider = provider;
  }
  
  public Id<CarsharingVehicleData> getVid()
  {
    return this.vid;
  }
  
  public Coord getLocation()
  {
    return this.location;
  }
  
  public long getMileage()
  {
    return this.mileage;
  }
  
  public double getFuel()
  {
    return this.fuel;
  }
  
  public long getTime()
  {
    return this.time;
  }
  
  public String getProvider()
  {
    return this.provider;
  }
  
  public String getLicense()
  {
    return this.license;
  }
  
  public String toString()
  {
    return this.license + "\t" + this.vid + "\t" + this.location + "\t" + this.mileage;
  }
}
