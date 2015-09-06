package playground.balac.carsharing.preprocess.membership.IO;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;

import org.matsim.core.population.PersonUtils;
import playground.balac.carsharing.data.FlexTransPersonImpl;


public class MembershipSummaryWriter
{
  private static final Logger log = Logger.getLogger(MembershipSummaryWriter.class);
  private FileWriter fw = null;
  private BufferedWriter out = null;
  private Person person;

  public MembershipSummaryWriter(String outfile)
  {
    try
    {
      this.fw = new FileWriter(outfile);
      System.out.println(outfile);
      this.out = new BufferedWriter(this.fw);
      this.out.write("Pers_Id\tLicense\tCar_availability\tAge\tAccessHome\tAccessWork\tDensityHome\tTravelcards\tHomeCoordX\tHomeCoordY\tHomeSwissCoordX\tHomeSwissCoordY\n");
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

  public void write(FlexTransPersonImpl person)
  {
    try
    {
      this.out.write(person.getId() + "\t");
      this.out.write(PersonUtils.getLicense(person) + "\t");
      this.out.write(PersonUtils.getCarAvail(person) + "\t");
      this.out.write(PersonUtils.getAge(person) + "\t");
      this.out.write(person.getAccessHome() + "\t");
      this.out.write(person.getAccessWork() + "\t");
      this.out.write(person.getDensityHome() + "\t");
      this.out.write(PersonUtils.getTravelcards(person) + "\t");
      this.out.write(person.getHomeCoord().getX() + "\t");
      this.out.write(person.getHomeCoord().getY() + "\t");
      this.out.write(person.getHomeSwissCoord().getX() + "\t");
      this.out.write(person.getHomeSwissCoord().getY() + "\t");
      this.out.write(person.getHomeSwissCoord().getY() + "\t");
      this.out.write("\n");
      this.out.flush();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}