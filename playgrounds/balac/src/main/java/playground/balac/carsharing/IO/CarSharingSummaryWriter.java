package playground.balac.carsharing.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonUtils;
import playground.balac.carsharing.router.CarSharingStation;

public class CarSharingSummaryWriter
{
  private static final Logger log = Logger.getLogger(CarSharingSummaryWriter.class);
  private FileWriter fw = null;
  private BufferedWriter out = null;
  private Person person;

  public CarSharingSummaryWriter(String outfile)
  {
    try
    {
      this.fw = new FileWriter(outfile);
      System.out.println(outfile);
      this.out = new BufferedWriter(this.fw);
      this.out.write("Pers_Id\tLicense\tCar_availability\tStart_x\tstart_y\tfromStation_x\tfromStation_y\ttoStation_x\ttoStation_y\tEnd_x\tEnd_y\tDep_Time\tArr_Time\tAct_Type_B\tAct_Type_A\n");
      this.out.flush();
    } catch (IOException e) {
      e.printStackTrace();

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

    }
  }

  public void write(Person person, Link startLink, CarSharingStation fromStation, CarSharingStation toStation, Link endLink, double departureTime, double arrivalTime, Activity actBefore, Activity actAfter)
  {
    try
    {
      this.out.write(person.getId() + "\t");
      this.out.write(PersonUtils.getLicense(person) + "\t");
      this.out.write(PersonUtils.getCarAvail(person) + "\t");
      this.out.write(startLink.getCoord().getX() + "\t");
      this.out.write(startLink.getCoord().getY() + "\t");
      this.out.write(fromStation.getCoord().getX() + "\t");
      this.out.write(fromStation.getCoord().getY() + "\t");
      this.out.write(toStation.getCoord().getX() + "\t");
      this.out.write(toStation.getCoord().getY() + "\t");
      this.out.write(endLink.getCoord().getX() + "\t");
      this.out.write(endLink.getCoord().getY() + "\t");
      this.out.write(departureTime + "\t");
      this.out.write(arrivalTime + "\t");
      this.out.write(actBefore.getType() + "\t");
      this.out.write(actAfter.getType() + "\n");
      this.out.flush();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}
