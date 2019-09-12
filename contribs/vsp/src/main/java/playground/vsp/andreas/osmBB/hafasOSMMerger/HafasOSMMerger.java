package playground.vsp.andreas.osmBB.hafasOSMMerger;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleImpl;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.vsp.andreas.mzilske.bvg09.HafasReader;

public class HafasOSMMerger {

	private static Logger log = Logger.getLogger(HafasOSMMerger.class);

	static final String hafasAlldat = "D:/Berlin/BVG/berlin-bvg09/urdaten/BVG-Fahrplan_2008/Daten/1_Mo-Do/";
	static final String hafasTransitSchedule = "e:/_out/ts/hafas_transitSchedule.xml";
	static final String osmTransitSchedule = "e:/_out/ts/osm_transitSchedule.xml";

	static final String osmNetworkFile = "e:/_out/ts/transit-network_bln.xml";
	static final String lineToConvert = "M10";

	MutableScenario hafasScenario;
	Config hafasConfig;

	MutableScenario osmScenario;
	Config osmConfig;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HafasOSMMerger merger = new HafasOSMMerger();
		merger.readHafas();

		merger.importHafasSchedule();

		merger.mergeSchedules();


	}

	private void mergeSchedules(){

		TransitScheduleImpl hafasSchedule = (TransitScheduleImpl) this.hafasScenario.getTransitSchedule();
		TransitSchedule osmSchedule = this.osmScenario.getTransitSchedule();

		for (Entry<Id<TransitLine>, TransitLine> osm : osmSchedule.getTransitLines().entrySet()) {

			if(osm.getKey().toString().contains(this.lineToConvert)){
				log.info("Found " + osm.getKey());

				for (TransitRoute osmRoute : osm.getValue().getRoutes().values()) {

					TransitLine hafasLine = null;;
					for (Id id : hafasSchedule.getTransitLines().keySet()) {
						if(id.toString().contains(this.lineToConvert)){
							hafasLine = hafasSchedule.getTransitLines().get(id);
							log.info("Found hafas line " + id.toString() + " for osm line " + osm.getKey());
						}
					}

					if(hafasLine != null){
						TransitRoute hafasRouteBestMatched = findRouteBestMatching(osmRoute, hafasLine.getRoutes());
					} else {
						log.info("Could not find Hafas Line for OSM line " + osm.getKey());
					}

				}


			} else {
				log.info("Ignored: " + osm.getKey());
			}









		}

		log.info("wait");


	}

	private TransitRoute findRouteBestMatching(TransitRoute route, Map<Id<TransitRoute>, TransitRoute> candidateRoutes) {

		double bestTotalScore = Double.MAX_VALUE;
		TransitRoute bestRoute = null;

		for (Entry<Id<TransitRoute>, TransitRoute> candidateRoute : candidateRoutes.entrySet()) {

			double stopDistanceSum = 0.0;

			for (int i = 0; i < Math.min(route.getStops().size(), candidateRoute.getValue().getStops().size()); i++) {
				stopDistanceSum += calculateDistanceBetweenCoords(route.getStops().get(i).getStopFacility().getCoord(), candidateRoute.getValue().getStops().get(i).getStopFacility().getCoord());
			}

			// add something for every stop missing or too much
//			stopDistanceSum += 1000 * (Math.max(route.getStops().size(), candidateRoute.getValue().getStops().size()) - Math.min(route.getStops().size(), candidateRoute.getValue().getStops().size()));

			if(bestRoute == null){
				bestRoute = candidateRoute.getValue();
			} else if(bestTotalScore / bestRoute.getStops().size() > stopDistanceSum / candidateRoute.getValue().getStops().size()){
				bestTotalScore = stopDistanceSum;
				bestRoute = candidateRoute.getValue();
			}

		}

		return bestRoute;
	}

	private double calculateDistanceBetweenCoords(Coord one, Coord two){
		return Math.sqrt(Math.pow(one.getX() - two.getX(), 2) + Math.pow(one.getY() - two.getY(), 2));
	}

	private void importHafasSchedule() {

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

	private void readHafas() {
		HafasReader.main(null);
	}




}
