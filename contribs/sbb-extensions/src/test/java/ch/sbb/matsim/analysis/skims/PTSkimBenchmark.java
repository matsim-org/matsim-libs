package ch.sbb.matsim.analysis.skims;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Standalone benchmark for the PT skim matrix computation (Raptor one-to-all).
 *
 * <p>Configure paths via system properties:
 * <pre>{@code
 *   -Dnetwork.path=... -Dschedule.path=... -Dzone.coordinates.path=...
 * }</pre>
 * or relies on default paths relative to the workspace.
 *
 * <p>To keep runtime short, only a single 1-hour time window is computed and the zone
 * set can be limited via {@code -DmaxZones=100}.
 *
 * <p>Example invocation:
 * <pre>{@code
 * cd matsim-libs && mvn -pl contribs/sbb-extensions exec:java \
 *   -Dexec.mainClass=ch.sbb.matsim.analysis.skims.PTSkimBenchmark \
 *   -Dexec.classpathScope=test \
 *   -DmaxZones=100
 * }</pre>
 */
public class PTSkimBenchmark {

	private static final String DEFAULT_NETWORK = "network.xml.zst";
	private static final String DEFAULT_SCHEDULE = "schedule.xml.zst";
	private static final String DEFAULT_ZONE_COORDS = "zone_coordinates.csv";

	public static void main(String[] args) throws IOException {
		String networkPath = System.getProperty("network.path", DEFAULT_NETWORK);
		String schedulePath = System.getProperty("schedule.path", DEFAULT_SCHEDULE);
		String zoneCoordinatesPath = System.getProperty("zone.coordinates.path", DEFAULT_ZONE_COORDS);
		int maxZones = Integer.parseInt(System.getProperty("maxZones", "100"));
		int numberOfThreads = Integer.parseInt(System.getProperty("threads", String.valueOf(Runtime.getRuntime().availableProcessors())));

		System.out.println("=== PT Skim Benchmark ===");
		System.out.println("  network:     " + networkPath);
		System.out.println("  schedule:    " + schedulePath);
		System.out.println("  zones:       " + zoneCoordinatesPath);
		System.out.println("  maxZones:    " + maxZones);
		System.out.println("  threads:     " + numberOfThreads);
		System.out.println();

		// --- Load network + schedule ---
		long t0 = System.currentTimeMillis();
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);
		new TransitScheduleReader(scenario).readFile(schedulePath);
		long loadMs = System.currentTimeMillis() - t0;
		System.out.println("Loaded network + schedule in " + loadMs + " ms");

		// --- Prepare Raptor data ---
		t0 = System.currentTimeMillis();
		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);
		raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
		SwissRailRaptorData raptorData = SwissRailRaptorData.create(
				scenario.getTransitSchedule(), scenario.getTransitVehicles(),
				raptorConfig, scenario.getNetwork(), null);
		long prepMs = System.currentTimeMillis() - t0;
		System.out.println("Raptor data preparation: " + prepMs + " ms");

		// --- Load zone coordinates (subset) ---
		t0 = System.currentTimeMillis();
		Map<String, Coord[]> coordsPerZone = loadZoneCoordinates(zoneCoordinatesPath, maxZones);
		long zoneMs = System.currentTimeMillis() - t0;
		System.out.println("Loaded " + coordsPerZone.size() + " zones (" + countCoords(coordsPerZone) + " coords) in " + zoneMs + " ms");
		System.out.println();

		// --- Benchmark: single time window 12:00-13:00 ---
		double startTime = Time.parseTime("12:00:00");
		double endTime = Time.parseTime("13:00:00");
		double stepSize = 120; // seconds between departure probes
		RaptorParameters raptorParameters = RaptorUtils.createParameters(config);

		System.out.println("Starting PT skim computation: " + Time.writeTime(startTime) + " - " + Time.writeTime(endTime)
				+ ", step=" + (int) stepSize + "s, " + coordsPerZone.size() + " zones");

		long benchStart = System.currentTimeMillis();
		PTSkimMatrices.PtIndicators<String> result = PTSkimMatrices.calculateSkimMatrices(
				raptorData, coordsPerZone, startTime, endTime, stepSize,
				raptorParameters, numberOfThreads,
				(line, route) -> "rail".equals(route.getTransportMode()),
				new PTSkimMatrices.CoordAggregator() {});
		long benchMs = System.currentTimeMillis() - benchStart;

		// --- Report ---
		int zones = coordsPerZone.size();
		long odPairs = (long) zones * zones;
		double odPerSec = odPairs / (benchMs / 1000.0);

		System.out.println();
		System.out.println("=== Results ===");
		System.out.println("  Zones:       " + zones);
		System.out.println("  OD pairs:    " + odPairs);
		System.out.println("  Wall-clock:  " + benchMs + " ms (" + String.format("%.1f", benchMs / 1000.0) + " s)");
		System.out.println("  Throughput:  " + String.format("%.0f", odPerSec) + " OD/s");
		System.out.println("  Threads:     " + numberOfThreads);
		System.out.println();

		// prevent dead-code elimination
		System.out.println("  (checksum: adaptionTime[0,0]=" + result.adaptionTimeMatrix.get(
				coordsPerZone.keySet().iterator().next(), coordsPerZone.keySet().iterator().next()) + ")");
	}

	private static Map<String, Coord[]> loadZoneCoordinates(String filename, int maxZones) throws IOException {
		Map<String, java.util.List<Coord>> tmp = new HashMap<>();
		try (BufferedReader reader = Files.newBufferedReader(Path.of(filename))) {
			String header = reader.readLine(); // skip header
			String line;
			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(";");
				String zoneId = parts[0];
				if (tmp.size() >= maxZones && !tmp.containsKey(zoneId)) {
					continue; // limit zone count
				}
				double x = Double.parseDouble(parts[2]);
				double y = Double.parseDouble(parts[3]);
				tmp.computeIfAbsent(zoneId, k -> new java.util.ArrayList<>()).add(new Coord(x, y));
			}
		}
		// convert to Coord[]
		Map<String, Coord[]> result = new HashMap<>();
		for (Map.Entry<String, java.util.List<Coord>> entry : tmp.entrySet()) {
			result.put(entry.getKey(), entry.getValue().toArray(new Coord[0]));
		}
		return result;
	}

	private static int countCoords(Map<String, Coord[]> coordsPerZone) {
		int count = 0;
		for (Coord[] coords : coordsPerZone.values()) {
			count += coords.length;
		}
		return count;
	}
}
