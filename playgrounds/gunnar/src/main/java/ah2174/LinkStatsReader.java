package ah2174;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.scenario.ScenarioUtils;

import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

public class LinkStatsReader implements TabularFileHandler {

	// -------------------- CONSTANTS --------------------

	private final String LINKID_HEADER = "LINK";

	private final String LENGTH_HEADER = "LENGTH";

	private final String FREESPEAD_HEADER = "FREESPEED";

	private final String CAPACITY_HEADER = "CAPACITY";

	private final String FLOW_HEADER(final int hour) {
		return "HRS" + hour + "-" + (hour + 1) + "avg";
	}

	private final String TRAVELTIME_HEADER(final int hour) {
		return "TRAVELTIME" + hour + "-" + (hour + 1) + "avg";
	}

	// -------------------- MEMBERS --------------------

	private final String configFileName;

	private final String linkstatsFileName;

	private final String matlabFileName;

	private final double flowSamplingRate;

	private Network network;

	private Map<String, Integer> header2column = null;

	private PrintWriter matlabWriter = null;

	private int start_h;

	private int end_h;

	// -------------------- CONSTRUCTION --------------------

	public LinkStatsReader(final String configFileName,
			final String linkStatsFileName, final String matlabFileName,
			final double flowSamplingRate, final int start_h, final int end_h) {
		this.configFileName = configFileName;
		this.linkstatsFileName = linkStatsFileName;
		this.matlabFileName = matlabFileName;
		this.flowSamplingRate = flowSamplingRate;
		this.start_h = start_h;
		this.end_h = end_h;
	}

	// -------------------- IMPLEMENTATION --------------------

	public static String newUnzippedLinkStatsFile(
			final String zippedLinkStatsFile) throws IOException {
		final File tmp = File.createTempFile("linkstats", "tmp");
		FileInputStream instream = new FileInputStream(zippedLinkStatsFile);
		GZIPInputStream ginstream = new GZIPInputStream(instream);
		FileOutputStream outstream = new FileOutputStream(tmp);
		byte[] buf = new byte[1024];
		int len;
		while ((len = ginstream.read(buf)) > 0) {
			outstream.write(buf, 0, len);
		}
		ginstream.close();
		outstream.close();
		return tmp.getAbsolutePath();
	}

	private Network newNetwork(final String configFileName) {
		Config config = ConfigUtils.loadConfig(configFileName);
		final String networkName = config.getParam("network",
				"inputNetworkFile");
		Scenario scenario = ScenarioUtils.createScenario(config);
		NetworkReaderMatsimV1 reader = new NetworkReaderMatsimV1(scenario);
		reader.parse(networkName);
		return scenario.getNetwork();
	}

	public void run() throws IOException {
		this.network = newNetwork(this.configFileName);
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		parser.parse(this.linkstatsFileName, this);
	}

	// --------------- IMPLEMENTATION OF TabularFileHandler ---------------

	@Override
	public void startDocument() {
		this.header2column = null;
		try {
			this.matlabWriter = new PrintWriter(this.matlabFileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String preprocess(final String line) {
		return line;
	}

	@Override
	public void startRow(final String[] row) {
		if (this.header2column == null) {
			/*
			 * process header row
			 */
			this.header2column = new LinkedHashMap<String, Integer>();
			for (int column = 0; column < row.length; column++) {
				this.header2column.put(row[column], column);
			}
		} else {
			/*
			 * process data row
			 */
			try {
				this.matlabWriter.print(Integer.parseInt(row[this.header2column
						.get(this.LINKID_HEADER)]));
				this.matlabWriter.print(",");

				// TODO Saleem, I made this untested change to make it compile.
				// ORIGINAL:
				// final Link link = this.network.getLinks().get(
				// new IdImpl(row[this.header2column
				// .get(this.LINKID_HEADER)]));
				// NEW:
				final Link link = this.network
						.getLinks()
						.get(Id.create(
								row[this.header2column.get(this.LINKID_HEADER)],
								Link.class));
				// ------------------------------------------------------------

				this.matlabWriter.print(link.getCoord().getX());
				this.matlabWriter.print(",");
				this.matlabWriter.print(link.getCoord().getY());
				this.matlabWriter.print(",");

				this.matlabWriter.print(row[this.header2column
						.get(this.CAPACITY_HEADER)]); // in vehicles/hour
				this.matlabWriter.print(",");

				final double vMax_m_s = Double
						.parseDouble(row[this.header2column
								.get(this.FREESPEAD_HEADER)]);
				final double length_m = Double
						.parseDouble(row[this.header2column
								.get(this.LENGTH_HEADER)]);
				this.matlabWriter.print(length_m / vMax_m_s); // in seconds
				this.matlabWriter.print(",");

				double flowSum_veh = 0;
				double ttSum_s = 0;
				for (int h = this.start_h; h < this.end_h; h++) {
					final double flow_veh = Double
							.parseDouble(row[this.header2column.get(this
									.FLOW_HEADER(h))])
							/ this.flowSamplingRate;
					flowSum_veh += flow_veh;
					ttSum_s += flow_veh
							* Double.parseDouble(row[this.header2column
									.get(this.TRAVELTIME_HEADER(h))]);
				}
				this.matlabWriter.print(flowSum_veh); // in vehicles
				this.matlabWriter.print(",");
				this.matlabWriter.print(ttSum_s); // in seconds
				this.matlabWriter.println();
			} catch (Exception e) {
				System.err.print("SKIPPING THIS ROW: ");
				for (String col : row) {
					System.err.print(col);
					System.err.print(" ");
				}
				System.err.println();
			}
		}
	}

	@Override
	public void endDocument() {
		this.matlabWriter.flush();
		this.matlabWriter.close();
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		System.out.println("STARTED: " + LinkStatsReader.class.getSimpleName());

		final String configFile = args[0];
		final String matsimFile = args[1];
		final String matlabFile = args[2];

		try {
			final String unzippedMatsimFile = newUnzippedLinkStatsFile(matsimFile);
			(new LinkStatsReader(configFile, unzippedMatsimFile, matlabFile,
					0.01, 0, 24)).run();
			new File(unzippedMatsimFile).delete();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("DONE: " + LinkStatsReader.class.getSimpleName());
	}
}
