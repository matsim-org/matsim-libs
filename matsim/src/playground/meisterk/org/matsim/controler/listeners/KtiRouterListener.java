package playground.meisterk.org.matsim.controler.listeners;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.Matrix;
import org.matsim.visum.VisumMatrixReader;
import org.matsim.world.MatsimWorldReader;
import org.matsim.world.World;
import org.xml.sax.SAXException;

import playground.marcel.kti.router.SwissHaltestellen;

public class KtiRouterListener implements StartupListener {

	private Matrix ptTravelTimes = null;
	private SwissHaltestellen haltestellen = null;
	private World localWorld=null;

	private static final Logger log = Logger.getLogger(playground.meisterk.org.matsim.run.ktiYear3.Controler.class);

	public void notifyStartup(StartupEvent event) {

		boolean usePlansCalcRouteKti = Boolean.parseBoolean(Gbl.getConfig().getModule("kti").getValue("usePlansCalcRouteKti"));
		if (usePlansCalcRouteKti) {
			this.prepareKTIRouter(event.getControler());
		}
		
	}

	public void prepareKTIRouter(Controler c) {

		// municipality layer vom world file
		this.localWorld = new World();
		String worldFilename = Gbl.getConfig().getModule("kti").getValue("worldInputFilename");
		try {
			new MatsimWorldReader(localWorld).parse(worldFilename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		log.info("Reading traveltime matrix...");
		VisumMatrixReader reader = new VisumMatrixReader("pt_traveltime", localWorld.getLayer("municipality"));
		reader.readFile(Gbl.getConfig().getModule("kti").getValue("pt_traveltime_matrix_filename"));
		log.info("Reading traveltime matrix...done.");
		this.ptTravelTimes = Matrices.getSingleton().getMatrix("pt_traveltime");

		log.info("Reading haltestellen...");
		this.haltestellen = new SwissHaltestellen(c.getNetwork());
		try {
			haltestellen.readFile(Gbl.getConfig().getModule("kti").getValue("pt_haltestellen_filename"));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Reading haltestellen...done.");

	}

	public Matrix getPtTravelTimes() {
		return ptTravelTimes;
	}

	public SwissHaltestellen getHaltestellen() {
		return haltestellen;
	}

	public World getLocalWorld() {
		return localWorld;
	}

}
