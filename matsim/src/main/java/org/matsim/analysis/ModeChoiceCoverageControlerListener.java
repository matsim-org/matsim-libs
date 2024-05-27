package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.utils.charts.XYLineChart;
import org.matsim.core.utils.io.IOUtils;

import jakarta.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Calculates mode choice coverage at the end of each iteration, based on the main mode identifier of a trip chain.
 * Mode choice coverage percentage of trips have used a certain mode at least once (or 5x, 10x, …) in previous iterations.
 *
 * @author jakobrehmann
 */
public class ModeChoiceCoverageControlerListener implements StartupListener, IterationEndsListener,
        ShutdownListener {

    private final static Logger log = LogManager.getLogger(ModeStatsControlerListener.class);


    private final Map<Integer, BufferedWriter> modeOutMap = new HashMap<>();

    private final Population population;
    private final String modeFileName;
    private final boolean createPNG;
    private final ControllerConfigGroup controllerConfigGroup;
    private final MainModeIdentifier mainModeIdentifier;

    private int minIteration = 0;
    private int firstIteration = -1;

    private final Integer[] limits = new Integer[]{1, 5, 10};

    private final Map<Integer, Map<String, Map<Integer, Double>>> modeCCHistory = new HashMap<>();
    //            Map<Iter   , Map<Mode  , Map<Limit  , Pct   >>>
    private final Map<Id<Person>, Map<Integer, Map<String, Integer>>> modesUsedPerPersonTrip = new LinkedHashMap<>();
    //            Map<Person    , Map<Trip # , Map<Mode  , Count  >>>
    private static final String FILENAME_MODESTATS = "modeChoiceCoverage";

    // Keep all modes encountered so far in a sorted set to ensure output is written for modes sorted by mode.
    private final Set<String> modes;


    @Inject
    ModeChoiceCoverageControlerListener(ControllerConfigGroup controllerConfigGroup, Population population1, OutputDirectoryHierarchy controlerIO,
																				ScoringConfigGroup scoreConfig, AnalysisMainModeIdentifier mainModeIdentifier) {

        this.controllerConfigGroup = controllerConfigGroup;
        this.population = population1;
        this.modeFileName = controlerIO.getOutputFilename(FILENAME_MODESTATS);
        //		this.createPNG = controlerConfigGroup.isCreateGraphs();
        this.createPNG = true;

        this.modes = new TreeSet<>();
        //		this.modes.addAll(scoreConfig.getAllModes());

        this.mainModeIdentifier = mainModeIdentifier;
    }

    @Override
    public void notifyStartup(final StartupEvent event) {
        this.minIteration = controllerConfigGroup.getFirstIteration();
    }

    @Override
    public void notifyIterationEnds(final IterationEndsEvent event) {

        if (firstIteration < 0) {
            firstIteration = event.getIteration();
        }

        /*
         *  modesUsedPerPersonTrip: for each person-trip, how many times (iterations) was each mode used. The following code adds the
         * 	mode information from the current iteration to the modesUsedPerPersonTrip.
         */

        updateModesUsedPerPerson();

		/*
		 *	Looks through modesUsedPerPersonTrip at each person-trip. How many of those person trips have used each mode more than the
		 *  predefined limits.
		 */
		int totalPersonTripCount = 0;
		Map<Integer, Map<String, Double>> modeCountCurrentIteration = new TreeMap<>();
		//Map<Limit, Map<Mode  , TotalTripCount >>

		for (Map<Integer, Map<String, Integer>> mapForPerson : modesUsedPerPersonTrip.values()) {
			//Map<Trip # , Map<Mode  , Count  >>
			for (Map<String, Integer> mapForPersonTrip : mapForPerson.values()) {
				//Map<Mode  , Count >
				totalPersonTripCount++;
				for (String mode : mapForPersonTrip.keySet()) {
					Integer realCount = mapForPersonTrip.get(mode);
					for (Integer limit : limits) {
						Map<String, Double> modeCountMap = modeCountCurrentIteration.computeIfAbsent(limit, k -> new TreeMap<>());
						Double modeCount = modeCountMap.computeIfAbsent(mode, k -> 0.);
						if (realCount >= limit) {
							modeCount++;
						}
						modeCountMap.put(mode, modeCount);
						modeCountCurrentIteration.put(limit, modeCountMap);
					}
				}
			}
		}


		//		for testing purposes: if there are any trips, do analysis. If not, it is probably a test or a faulty / empty population. -sme0524
		if (!modeCountCurrentIteration.isEmpty()) {
			// Calculates mcc share for each mode in current iteration, and updates modeCCHistory accordingly
			for (Integer limit : limits) {
				Map<String, Double> modeCnt = modeCountCurrentIteration.get(limit);
				this.modes.addAll(modeCnt.keySet()); // potentially adds new modes to setthat just showed up in current iter
				Map<String, Map<Integer, Double>> modeIterationShareMap = modeCCHistory.computeIfAbsent(limit, k -> new HashMap<>());
				for (String mode : modes) {
					Double cnt = modeCnt.get(mode);
					double share = 0.;
					if (cnt != null) {
						share = cnt / totalPersonTripCount;
					}

					log.info("-- mode choice coverage (" + limit + "x) of mode " + mode + " = " + share);

					Map<Integer, Double> iterationShareMap = modeIterationShareMap.get(mode);

					// If this is the first iteration where the mode shows up, add zeros to all previous iterations in history
					if (iterationShareMap == null) {
						iterationShareMap = new TreeMap<>();
						for (int iter = firstIteration; iter < event.getIteration(); iter++) {
							iterationShareMap.put(iter, 0.0);
						}
						modeIterationShareMap.put(mode, iterationShareMap);
					}

					iterationShareMap.put(event.getIteration(), share);
				}
			}


			// Print MCC Stats to output file
			for (Integer limit : limits) {
				Map<String, Map<Integer, Double>> modeIterationShareMap = modeCCHistory.get(limit);

				BufferedWriter modeOut = IOUtils.getBufferedWriter(this.modeFileName + limit + "x.txt");
				try {
					modeOut.write("Iteration");
					for (String mode : modes) {
						modeOut.write("\t" + mode);
					}
					modeOut.write("\n");
					for (int iter = firstIteration; iter <= event.getIteration(); iter++) {
						modeOut.write(String.valueOf(iter));
						for (String mode : modes) {
							modeOut.write("\t" + modeIterationShareMap.get(mode).get(iter));
						}
						modeOut.write("\n");
					}

					modeOut.flush();
					modeOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			// Produce Graphs
			if (this.createPNG && event.getIteration() > this.minIteration) {
				produceGraphs();
			}

		} else {
			log.warn("There are no trips conducted by the analyzed population. This should only be the case for tests. If you are running a simulation run, " +
				" this should not happen. Check your population.");
		}
    }

    private void updateModesUsedPerPerson() {
        for (Person person : this.population.getPersons().values()) {

            Map<Integer, Map<String, Integer>> mapForPerson = modesUsedPerPersonTrip.computeIfAbsent(person.getId(), v -> new LinkedHashMap<>());

            Plan plan = person.getSelectedPlan();
            Integer tripNumber = 0;
            for (Trip trip : TripStructureUtils.getTrips(plan)) {

                tripNumber++;
                String mode = this.mainModeIdentifier.identifyMainMode(trip.getTripElements());

                Map<String, Integer> mapForPersonTrip = mapForPerson.computeIfAbsent(tripNumber, v -> new HashMap<>());

                Integer modeCount = mapForPersonTrip.getOrDefault(mode, 0);
                mapForPersonTrip.put(mode, modeCount + 1);

            }
        }
    }

    private void produceGraphs() {
        for (Integer limit : limits) {
            XYLineChart chart = new XYLineChart("Mode Choice Coverage (Mode Used >= " + limit + "x per trip)", "iteration", "mode choice coverage");
            for (Entry<String, Map<Integer, Double>> entry : modeCCHistory.get(limit).entrySet()) {
                String mode = entry.getKey();
                Map<Integer, Double> history = entry.getValue();
                chart.addSeries(mode, history);
            }
            chart.addMatsimLogo();
            chart.saveAsPng(this.modeFileName + limit + "x" + ".png", 800, 600);
        }
    }

    @Override
    public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {

        //        for (BufferedWriter modeOut : modeOutMap.values()) {
        //            try {
        //                modeOut.close();
        //            } catch (IOException e) {
        //                e.printStackTrace();
        //            }
        //        }
    }

    public final Map<Integer, Map<String, Map<Integer, Double>>> getModeChoiceCoverageHistory() {
        return Collections.unmodifiableMap(modeCCHistory);
    }
}
