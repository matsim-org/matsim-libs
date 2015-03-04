package playground.balac.carsharing.preprocess.membership.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;

import playground.balac.carsharing.preprocess.membership.PersonWithClosestStations;
import playground.balac.carsharing.preprocess.membership.Station;


public class Persons_CS_Stations_Distances_Writer
{
  private static final Logger log = Logger.getLogger(MembershipSummaryWriter.class);
  private FileWriter fw = null;
  private BufferedWriter out = null;
  private WGS84toCH1903LV03 coordTranformer = new WGS84toCH1903LV03();

  public Persons_CS_Stations_Distances_Writer(String outfile)
  {
    try
    {
      this.fw = new FileWriter(outfile);
      System.out.println(outfile);
      this.out = new BufferedWriter(this.fw);
      this.out.write("Pers_Id\n");
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

  public void write(PersonWithClosestStations person)
  {
    try
    {
      this.out.write(person.getId() + "\t");
      this.out.write("Home\t");
      for (Station stationHome : person.getOrderedClosestStationsHome()) {
        this.out.write(stationHome.getCoord().getX() + "\t");
        this.out.write(stationHome.getCoord().getY() + "\t");
      }
      this.out.write("Work\t");
      for (Station stationWork : person.getOrderedClosestStationsWork()) {
        this.out.write(stationWork.getCoord().getX() + "\t");
        this.out.write(stationWork.getCoord().getY() + "\t");
      }
      this.out.write(person.getAccessibilityHome() + "\t");
      this.out.write(person.getAccessibilityWork() + "\t");
      this.out.write(person.getCoordHome().getX() + "\t");
      this.out.write(person.getCoordHome().getY() + "\t");
      this.out.write(person.getCoordWork().getX() + "\t");
      this.out.write(person.getCoordWork().getY() + "\t");
      this.out.write("\n");
      this.out.flush();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}