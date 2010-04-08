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
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.xml.sax.SAXException;

import playground.ciarif.flexibletransports.config.FtConfigGroup;


public class PlansCalcRouteFtInfo {
	private CarSharingStations carStations = null;
	private World localWorld=null;
	private final FtConfigGroup ftConfigGroup;

	private static final Logger log = Logger.getLogger(PlansCalcRouteFtInfo.class);

	public PlansCalcRouteFtInfo(final FtConfigGroup ftConfigGroup) {
		super();
		this.ftConfigGroup = ftConfigGroup;
	}

	public void prepare(final Network network) {
		
		if (!ftConfigGroup.isUsePlansCalcRouteKti()) {
			log.error("The FT module is missing.");
		}

		// municipality layer from world file
		ScenarioImpl localScenario = new ScenarioImpl();
		this.localWorld = localScenario.getWorld();
		try {
			new MatsimWorldReader(localScenario).parse(ftConfigGroup.getWorldInputFilename());
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
			carStations.readFile(ftConfigGroup.getCarSharingStationsFilename());
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Reading car stations...done.");

	}

	public CarSharingStations getCarStations() {
		return carStations;
	}

	public World getLocalWorld() {
		return localWorld;
	}

	public FtConfigGroup getFtConfigGroup() {
		return ftConfigGroup;
	}

}
