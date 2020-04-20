package playground.vsp.andreas.osmBB.hafasOSMMerger;

import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
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

	private MutableScenario hafasScenario;
	private Config hafasConfig;

	private MutableScenario osmScenario;
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
		for (Entry<Id<TransitLine>, TransitLine> transitLineEntry : this.osmScenario.getTransitSchedule().getTransitLines().entrySet()) {
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

		this.hafasScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.hafasConfig = this.hafasScenario.getConfig();
		this.hafasConfig.transit().setUseTransit(true);
		this.hafasConfig.network().setInputFile(this.osmNetworkFile);
		ScenarioUtils.loadScenario(this.hafasScenario);

		this.osmScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.osmConfig = this.osmScenario.getConfig();
		this.osmConfig.transit().setUseTransit(true);
		this.osmConfig.network().setInputFile(this.osmNetworkFile);
		ScenarioUtils.loadScenario(this.osmScenario);

		new TransitScheduleReaderV1(this.hafasScenario).readFile(hafasTransitSchedule);
		new TransitScheduleReaderV1(this.osmScenario).readFile(osmTransitSchedule);
	}
}
