package gunnar.tryout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.matrices.Matrices;
import org.matsim.matrices.MatricesWriter;
import org.matsim.matrices.Matrix;

import patryk.popgen2.PopulationParser;
import patryk.popgen2.SelectZones;
import patryk.popgen2.Zone;

public class TryOutZoneWriting {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final String networkFile = "./data/network/network_v12_utan_forbifart.xml";
		final String populationFile = "./data/synthetic_population/agentData.csv";
		final String zonesBoundaryShape = "./data/shapes/limit_EPSG3857.shp";

		final Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);

		final ArrayList<String> coveredZones;
		{
			PopulationParser parser = new PopulationParser();
			parser.read(populationFile);

			final HashMap<String, Zone> zones = parser.getZones();
			final SelectZones selectZones = new SelectZones(zones,
					zonesBoundaryShape);
			coveredZones = selectZones.getZonesInsideBoundary();
		}

		System.out.println("number of zones = " + coveredZones.size()
				* coveredZones.size());

		Matrices matrices = new Matrices();
		final Matrix work = matrices.createMatrix("WORK",
				"random work tour travel times");
		final Matrix other = matrices.createMatrix("OTHER",
				"random other tour travel times");

		final double sampleFraction = 1.0;
		final List<String> sampleZoneIds = new ArrayList<String>();
		for (String zoneId : coveredZones) {
			if (Math.random() < sampleFraction) {
				sampleZoneIds.add(zoneId);
			}
		}

		int fromZoneCnt = 0;
		for (String fromZone : sampleZoneIds) {
			System.out.println((++fromZoneCnt) + " / " + coveredZones.size());
			for (String toZone : sampleZoneIds) {
				work.createEntry(fromZone, toZone, Math.random());
				other.createEntry(fromZone, toZone, Math.random());
			}
		}

		final MatricesWriter writer = new MatricesWriter(matrices);
		writer.setIndentationString("    ");
		writer.setPrettyPrint(true);
		writer.write("testmatrix" + sampleFraction + ".xml");

		System.out.println("... DONE");
	}

}
