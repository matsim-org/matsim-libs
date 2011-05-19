package playground.andreas.osmBB.hafasOSMMerger;

import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

public class SimpleHafasOsmMerger {

	private static Logger log = Logger.getLogger(SimpleHafasOsmMerger.class);

//	static final String hafasAlldat = "D:/Berlin/BVG/berlin-bvg09/urdaten/BVG-Fahrplan_2008/Daten/1_Mo-Do/";
	private final String hafasTransitSchedule = "e:/_out/ts/hafas_transitSchedule.xml";
	private final String osmTransitSchedule = "e:/_out/ts/osm_transitSchedule.xml";
	private final String osmNetworkFile = "e:/_out/ts/transit-network_bln.xml";

	private final String transitScheduleOutFile = "e:/_out/ts/outSchedule.xml";

	private ScenarioImpl hafasScenario;
	private Config hafasConfig;

	private ScenarioImpl osmScenario;
	private Config osmConfig;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SimpleHafasOsmMerger merger = new SimpleHafasOsmMerger();
		merger.readTransitSchedules();
		merger.addRouteProfilToRoutes();


		merger.writeFinalSchedule();
	}






	/**
	 * Add arrivalOffset and departureOffset
	 */
	private void addRouteProfilToRoutes() {
		log.info("Adding arrivalOffsets and departureOffsets to transit lines.");
		for (Entry<Id, TransitLine> transitLineEntry : this.osmScenario.getTransitSchedule().getTransitLines().entrySet()) {
			int numberOfRoutesProcessed = 0;

			for (TransitRoute transitRoute : transitLineEntry.getValue().getRoutes().values()) {


				for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
//					transitRouteStop.
				}

			}

			log.info("Added " + transitLineEntry.getKey() + " with " + numberOfRoutesProcessed + " routes");
		}
	}



	private void writeFinalSchedule() {
		TransitScheduleWriter writer = new TransitScheduleWriter(this.osmScenario.getTransitSchedule());
		writer.writeFile(this.transitScheduleOutFile);
	}

	private void readTransitSchedules() {

		this.hafasScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.hafasConfig = this.hafasScenario.getConfig();
		this.hafasConfig.scenario().setUseTransit(true);
		this.hafasConfig.scenario().setUseVehicles(true);
		this.hafasConfig.network().setInputFile(this.osmNetworkFile);
		ScenarioLoaderImpl hafasLoader = new ScenarioLoaderImpl(this.hafasScenario);
		hafasLoader.loadScenario();

		this.osmScenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.osmConfig = this.osmScenario.getConfig();
		this.osmConfig.scenario().setUseTransit(true);
		this.osmConfig.scenario().setUseVehicles(true);
		this.osmConfig.network().setInputFile(this.osmNetworkFile);
		ScenarioLoaderImpl osmLoader = new ScenarioLoaderImpl(this.osmScenario);
		osmLoader.loadScenario();

		new TransitScheduleReaderV1(this.hafasScenario.getTransitSchedule(), this.hafasScenario.getNetwork(), this.hafasScenario).readFile(hafasTransitSchedule);
		new TransitScheduleReaderV1(this.osmScenario.getTransitSchedule(), this.osmScenario.getNetwork(), this.osmScenario).readFile(osmTransitSchedule);
	}
}
