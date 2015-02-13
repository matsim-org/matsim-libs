package playground.balac.carsharing.preprocess.membership.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.facilities.ActivityFacilityImpl;

import playground.balac.carsharing.router.CarSharingStation;
import playground.balac.carsharing.router.CarSharingStations;
import playground.balac.retailers.data.Retailer;
import playground.balac.retailers.utils.CountFacilityCustomers;


public class CarSharingStationsSummaryWriter
{
  private FileWriter fw = null;
  private BufferedWriter out = null;
  private CarSharingStations stations;

  public CarSharingStationsSummaryWriter(String outfile)
  {
    try
    {
      this.fw = new FileWriter(outfile);
      System.out.println(outfile);
      this.out = new BufferedWriter(this.fw);
      this.out.write("\tFac_id\tfac_x\tfac_y\tLink_id\n");
      this.out.flush();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
    System.out.println("    done.");
  }

  public final void close()
  {
    try
    {
      this.out.flush();
      this.out.close();
      this.fw.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public void write(CarSharingStations stations)
  {
    try
    {
      this.stations = stations;

      for (CarSharingStation station : stations.getStations()) {
        this.out.write(station.getId() + "\t");
        this.out.write(station.getCoord().getX() + "\t");
        this.out.write(station.getCoord().getY() + "\t");
        this.out.write(station.getLinkId() + "\t");
        this.out.write("\n");
      }

      this.out.flush();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void write(Retailer retailer, int iter, CountFacilityCustomers cfc)
  {
    try {
      for (ActivityFacilityImpl f : retailer.getFacilities().values()) {
        System.out.println("fac Id = " + f.getId());
        this.out.write(retailer.getId() + "\t");
        this.out.write(f.getId() + "\t");
        this.out.write(f.getCoord().getX() + "\t");
        this.out.write(f.getCoord().getY() + "\t");
        this.out.write(f.getLinkId() + "\t");
        this.out.write(cfc.countCustomers(f) + "\t");
        this.out.write(iter + "\n");
      }
      this.out.flush();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}