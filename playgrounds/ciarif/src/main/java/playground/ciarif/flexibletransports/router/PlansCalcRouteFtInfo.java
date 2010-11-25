package playground.ciarif.flexibletransports.router;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Network;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import playground.balmermi.world.World;
import playground.balmermi.world.MatsimWorldReader;
import org.xml.sax.SAXException;
import playground.ciarif.flexibletransports.config.FtConfigGroup;
import playground.meisterk.kti.router.SwissHaltestellen;

public class PlansCalcRouteFtInfo
{
  private CarSharingStations carStations = null;
  private final FtConfigGroup ftConfigGroup;
  private Matrix ptTravelTimes = null;
  private SwissHaltestellen haltestellen = null;
  private World localWorld;

  private static final Logger log = Logger.getLogger(PlansCalcRouteFtInfo.class);

  public PlansCalcRouteFtInfo(FtConfigGroup ftConfigGroup)
  {
    this.ftConfigGroup = ftConfigGroup;
  }

  public void prepare(Network network)
  {
    log.info("config group= " + this.ftConfigGroup);

    if (!(this.ftConfigGroup.isUsePlansCalcRouteFT())) {
      log.error("The FT module is missing.");
    }

    ScenarioImpl localScenario = new ScenarioImpl();
    //this.localWorld = localScenario.getWorld();
    try {
      new MatsimWorldReader(localScenario, this.localWorld).parse(this.ftConfigGroup.getWorldInputFilename());
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    log.info("Reading car stations...");
    this.carStations = new CarSharingStations(network);
    try {
      this.carStations.readFile(this.ftConfigGroup.getCarSharingStationsFilename());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("Reading car stations...done.");

    log.info("Reading traveltime matrix...");
    Matrices matrices = new Matrices();
    this.ptTravelTimes = matrices.createMatrix("pt_traveltime", null);
    VisumMatrixReader reader = new VisumMatrixReader(this.ptTravelTimes);
    reader.readFile(this.ftConfigGroup.getPtTraveltimeMatrixFilename());
    log.info("Reading traveltime matrix...done.");

    log.info("Reading haltestellen...");
    this.haltestellen = new SwissHaltestellen(network);
    try {
      this.haltestellen.readFile(this.ftConfigGroup.getPtHaltestellenFilename());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    log.info("Reading haltestellen...done.");
  }

  public CarSharingStations getCarStations()
  {
    return this.carStations;
  }

  public World getLocalWorld() {
    return this.localWorld;
  }

  public FtConfigGroup getFtConfigGroup() {
    return this.ftConfigGroup;
  }

  public Matrix getPtTravelTimes() {
    return this.ptTravelTimes;
  }

  public SwissHaltestellen getHaltestellen() {
    return this.haltestellen;
  }
}
