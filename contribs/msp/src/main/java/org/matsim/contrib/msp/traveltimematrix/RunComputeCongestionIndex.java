package org.matsim.contrib.msp.traveltimematrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexCalculator;
import org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile;
import org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import picocli.CommandLine;

import java.util.Set;

/**
 * Measures the congestion index of a finished run and writes it as a csv:
 * <pre>
 *   java -cp matsim-msp.jar org.matsim.contrib.msp.traveltimematrix.RunComputeCongestionIndex \
 *       --network output_network.xml.gz --events output_events.xml.gz --shp dresden-with-buffer.shp \
 *       --output congestion_index.csv
 * </pre>
 * The result is the time dimension of {@link TimeVaryingTravelTimeMatrix} and the only thing the matrix
 * needs from a run - a few hundred numbers, so a series of experiments measures it once and then only
 * reads the csv.
 * <p>
 * The index is measured over the <b>same network the matrix will be built over</b>: the car subnetwork
 * of the study area, cut by {@code --shp}. That is deliberate and is what {@code --shp} is for here -
 * the congestion of a Germany-wide network says nothing about the congestion in Dresden, and the two
 * have to agree or the index does not describe the trips the matrix answers.
 *
 * @see CongestionIndexCalculator for what is measured
 */
@CommandLine.Command(
	name = "congestion-index",
	description = "Measures the time-of-day congestion index a time-varying travel time matrix needs.",
	mixinStandardHelpOptions = true,
	showDefaultValues = true
)
public final class RunComputeCongestionIndex implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(RunComputeCongestionIndex.class);

	@CommandLine.Option(names = "--network", description = "Network of the run.", required = true)
	private String networkFile;

	@CommandLine.Option(names = "--events", description = "Events of the run, the congested travel times and the "
		+ "volumes are read from.", required = true)
	private String eventsFile;

	@CommandLine.Option(names = "--output", description = "The congestion index csv to write.",
		defaultValue = "congestion_index.csv")
	private String outputFile;

	@CommandLine.Option(names = "--modes", description = "Modes the index is measured for.", defaultValue = "car",
		split = ",")
	private Set<String> modes;

	@CommandLine.Option(names = "--time-bin-size", description = "Resolution of the index [s].",
		defaultValue = "900")
	private int timeBinSize;

	@CommandLine.Option(names = "--end-time", description = "End of the day the index is measured over [s].",
		defaultValue = "86400")
	private int endTime;

	@CommandLine.Option(names = "--road-classes", description = "Which road classes to measure separately: "
		+ "${COMPLETION-CANDIDATES}. NONE measures the network-wide index alone, which is all level 0 needs.",
		defaultValue = "DEFAULT_GROUPS")
	private RoadClasses roadClasses;

	@CommandLine.Option(names = "--qsim-time-step-size", description = "Time step size of the run [s], for the "
		+ "free-speed travel times the excess is measured against.", defaultValue = "1")
	private double qSimTimeStepSize;

	@CommandLine.Mixin
	private ShpOptions shp = new ShpOptions();

	/**
	 * The road classes of the command line, with the extra option of not splitting at all.
	 */
	public enum RoadClasses {
		NONE, DEFAULT_GROUPS, OSM_HIGHWAY_TYPE
	}

	public static void main(String[] args) {
		new RunComputeCongestionIndex().execute(args);
	}

	@Override
	public Integer call() {

		Network fullNetwork = NetworkUtils.readNetwork(this.networkFile);
		Network network = StudyAreaNetwork.cut(fullNetwork, studyArea(fullNetwork), this.modes);

		// the same settings the traffic dashboard of contribs/application uses, so that an index measured
		// here and one read off that dashboard are the same number
		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setCalculateLinkTravelTimes(true);
		builder.setMaxTime(this.endTime);
		builder.setTimeslice(this.timeBinSize);
		// the travel times of the modes asked for, so that the index of a car matrix is measured on cars
		// and not on the buses sharing the links
		builder.setFilterModes(true);
		builder.setAnalyzedModes(this.modes);
		TravelTimeCalculator travelTimes = builder.build();
		VolumesAnalyzer volumes = new VolumesAnalyzer(3600, this.endTime, network, true);

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(travelTimes);
		events.addHandler(volumes);
		events.initProcessing();
		EventsUtils.readEvents(events, this.eventsFile);
		events.finishProcessing();

		RoadClassification classification = switch (this.roadClasses) {
			case NONE -> RoadClassification.single();
			case DEFAULT_GROUPS -> RoadClassification.byDefaultGroups(network);
			case OSM_HIGHWAY_TYPE -> RoadClassification.byHighwayType(network);
		};

		CongestionIndexProfile profile = new CongestionIndexCalculator(network, travelTimes.getLinkTravelTimes(),
			volumes, classification, this.qSimTimeStepSize).calculate(this.timeBinSize, this.endTime);
		profile.write(this.outputFile);

		log.info("Set '{}' of the '{}' config group to {} to use this index.", "congestionIndexFile",
			TravelTimeMatrixConfigGroup.GROUP_NAME, this.outputFile);
		return 0;
	}

	/**
	 * The study area, in the coordinate reference system of the network - taken from the network itself
	 * when it carries one, so that a shape file in a different projection is still read correctly.
	 */
	private ShpOptions.Index studyArea(Network network) {
		if (!this.shp.isDefined()) {
			log.warn("No --shp given, so the index is measured over the whole network. That is only what you want "
				+ "if the network is already the study area.");
			return null;
		}
		String crs = ProjectionUtils.getCRS(network);
		return this.shp.createIndex(crs != null ? crs : this.shp.getShapeCrs(), "_");
	}
}
