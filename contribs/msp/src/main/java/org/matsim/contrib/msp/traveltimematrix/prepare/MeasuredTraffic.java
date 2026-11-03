package org.matsim.contrib.msp.traveltimematrix.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import java.util.Set;

/**
 * What a finished run tells us about congestion: the congested link travel times per time bin and the
 * link volumes, both read off one events file in one pass.
 * <p>
 * It exists because two very different things need exactly this pair and have to agree on it. The
 * {@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexCalculator congestion index}
 * needs the travel times and the volumes that weight them, and the
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.RoutedTravelTimes time-dependent routing
 * reference} needs the travel times to route on. Were each of them to set up its own
 * {@link TravelTimeCalculator}, the reference would be free to use a different bin size or a different
 * mode filter than the index it is used to judge - and the resulting error would be partly an artefact
 * of that difference rather than of the approximation under test. Reading the events once is also the
 * expensive part of every command in this package.
 * <p>
 * The settings are those of the traffic dashboard of {@code contribs/application}, so that an index
 * measured here and one read off that dashboard are the same number.
 *
 * @see #readFromEvents(Network, String, Set, int, int)
 */
public final class MeasuredTraffic {

	private static final Logger log = LogManager.getLogger(MeasuredTraffic.class);

	/**
	 * The time bin size {@link VolumesAnalyzer} is hard-wired to and whose getter it does not expose;
	 * {@code TrafficStatsCalculator} of {@code contribs/application} makes the same assumption.
	 * <p>
	 * Public and defined here once, because whoever <em>reads</em> the volumes has to assume the same
	 * number as whoever created them -
	 * {@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexCalculator} does - and two
	 * copies of a number that has to agree are one copy too many.
	 * <p>
	 * Note that it is <b>not</b> the bin size of the travel times: the volumes only weight the index, so
	 * an hour is plenty for them, while the travel times carry the shape of the peak and get the finer
	 * bin.
	 */
	public static final int VOLUME_BIN_SIZE = 3600;

	private final TravelTime travelTimes;
	private final VolumesAnalyzer volumes;

	private MeasuredTraffic(TravelTime travelTimes, VolumesAnalyzer volumes) {
		this.travelTimes = travelTimes;
		this.volumes = volumes;
	}

	/**
	 * Reads an events file and measures both quantities over the given network.
	 *
	 * @param network the links to measure over - the study area subnetwork, see {@link StudyAreaNetwork}.
	 * Events of a link outside it are ignored, which is how a Germany-wide run yields the traffic of one
	 * city.
	 * @param eventsFile the events of the run
	 * @param modes the modes the <b>travel times</b> are measured for, usually {@link TransportMode#car}.
	 * The volumes stay unfiltered on purpose: a bus in the queue is part of why the cars are slow, so it
	 * belongs in the weight even when its travel time does not belong in the index.
	 * @param timeBinSize resolution of the travel times [s]
	 * @param endTime end of the day to measure [s]
	 */
	public static MeasuredTraffic readFromEvents(Network network, String eventsFile, Set<String> modes,
		int timeBinSize, int endTime) {

		TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(network);
		builder.setCalculateLinkTravelTimes(true);
		builder.setMaxTime(endTime);
		builder.setTimeslice(timeBinSize);
		// the travel times of the modes asked for, so that the congestion a car matrix is scaled by is
		// measured on cars and not on the buses sharing the links
		builder.setFilterModes(true);
		builder.setAnalyzedModes(modes);
		TravelTimeCalculator travelTimeCalculator = builder.build();

		VolumesAnalyzer volumes = new VolumesAnalyzer(VOLUME_BIN_SIZE, endTime, network, true);

		log.info("Reading {} to measure {} travel times in bins of {} s and link volumes over {} links.", eventsFile,
			modes, timeBinSize, network.getLinks().size());

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(travelTimeCalculator);
		events.addHandler(volumes);
		events.initProcessing();
		EventsUtils.readEvents(events, eventsFile);
		events.finishProcessing();

		return new MeasuredTraffic(travelTimeCalculator.getLinkTravelTimes(), volumes);
	}

	/**
	 * The two quantities as they are, for a test or an analysis that measured them some other way.
	 */
	public static MeasuredTraffic of(TravelTime travelTimes, VolumesAnalyzer volumes) {
		return new MeasuredTraffic(travelTimes, volumes);
	}

	/**
	 * @return the congested link travel times of the run. Time-dependent, so it is also what a
	 * time-dependent router routes on.
	 */
	public TravelTime getTravelTimes() {
		return this.travelTimes;
	}

	/**
	 * @return the link volumes of the run, in bins of an hour, which weight the congestion index
	 */
	public VolumesAnalyzer getVolumes() {
		return this.volumes;
	}
}
