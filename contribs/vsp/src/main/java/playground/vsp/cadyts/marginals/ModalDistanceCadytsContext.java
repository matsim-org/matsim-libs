package playground.vsp.cadyts.marginals;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsContextI;
import org.matsim.contrib.cadyts.general.PlansTranslator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ModalDistanceCadytsContext implements CadytsContextI<Id<DistanceDistribution.DistanceBin>>, StartupListener, BeforeMobsimListener, IterationEndsListener {

	private static final String LINKOFFSET_FILENAME = "linkCostOffsets" + ModalDistanceCadytsBuilder.MARGINALS + ".xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis" + ModalDistanceCadytsBuilder.MARGINALS + ".txt";
	private Logger logger = Logger.getLogger(ModalDistanceCadytsContext.class);

	@Inject
	private Config config;

	@Inject
	private Scenario scenario;

	@Inject
	private DistanceDistribution expectedDistanceDistribution;

	@Inject
	ModalDistancePlansTranslator plansTranslator;

	@Inject
	TripEventHandler tripEventHandler;

	@Inject
	private Network network;

	@Inject
	private OutputDirectoryHierarchy outputDirectoryHierarchy;

	private AnalyticalCalibrator<Id<DistanceDistribution.DistanceBin>> calibrator;

	@Override
	public AnalyticalCalibrator<Id<DistanceDistribution.DistanceBin>> getCalibrator() {
		return calibrator;
	}

	@Override
	public PlansTranslator<Id<DistanceDistribution.DistanceBin>> getPlansTranslator() {
		return plansTranslator;
	}

	@Override
	public void notifyStartup(StartupEvent event) {

		this.calibrator = new ModalDistanceCadytsBuilder()
				.setConfig(config)
				.setExpectedDistanceDistribution(expectedDistanceDistribution)
				.build();
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {

		for (Person person : scenario.getPopulation().getPersons().values()) {
			cadyts.demand.Plan<Id<DistanceDistribution.DistanceBin>> demand = plansTranslator.getCadytsPlan(person.getSelectedPlan());
			this.calibrator.addToDemand(demand);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		// copy the bins from expected distance distribution
		DistanceDistribution simulatedDistanceDistribution = expectedDistanceDistribution.copyWithEmptyBins();

		for (Map.Entry<Id<Person>, List<TripEventHandler.BeelineTrip>> entry : tripEventHandler.getTrips().entrySet()) {
			for (TripEventHandler.BeelineTrip trip : entry.getValue()) {

				double distance = calculateBeelineDistance(trip);

				// count all the trips into the right distance bin
				simulatedDistanceDistribution.increaseCountByOne(trip.getMainMode(), distance);

				// get the distance bin we've put the count into and add a 'turn' for cadyts' plan builder
				DistanceDistribution.DistanceBin bin = simulatedDistanceDistribution.getBin(trip.getMainMode(), distance);
				Plan selectedPlan = scenario.getPopulation().getPersons().get(entry.getKey()).getSelectedPlan();
				plansTranslator.addTurn(selectedPlan, bin.getId(), event.getIteration());
			}
		}

		// prepare writing out cadyts offsets
		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.GROUP_NAME, CadytsConfigGroup.class);
		if (cadytsConfig.isWriteAnalysisFile() && shouldWriteAnalysisFile(event.getIteration(), config.counts().getWriteCountsInterval())) {
			String filename = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
			this.calibrator.setFlowAnalysisFile(filename);
		}

		// afterwards call cadyts
		// as a parameter it takes a SimResults function. As our result we just return the count value of the distance
		// bin defined by the bin id. This safes us at least three classes
		this.calibrator.afterNetworkLoading((binId, lower, upper, type) ->
				simulatedDistanceDistribution.getBin(binId).getValue() * simulatedDistanceDistribution.getScalingFactor());

		String offsetFilename = outputDirectoryHierarchy.getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		try {
			// the writer extends some cadyts writer which may produce memory leaks.
			new CadytsOffsetWriter().write(offsetFilename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private double calculateBeelineDistance(TripEventHandler.BeelineTrip trip) {

		if (scenario.getActivityFacilities().getFacilities().containsKey(trip.getDepartureFacility()) ||
				scenario.getActivityFacilities().getFacilities().containsKey(trip.getArrivalFacility())
		) {
			ActivityFacility departureFacility = scenario.getActivityFacilities().getFacilities().get(trip.getDepartureFacility());
			ActivityFacility arrivalFacility = scenario.getActivityFacilities().getFacilities().get(trip.getArrivalFacility());
			return CoordUtils.calcEuclideanDistance(departureFacility.getCoord(), arrivalFacility.getCoord());
		} else {
			Link departureLink = network.getLinks().get(trip.getDepartureLink());
			Link arrivalLink = network.getLinks().get(trip.getArrivalLink());
			return CoordUtils.calcEuclideanDistance(departureLink.getToNode().getCoord(), arrivalLink.getToNode().getCoord());
		}
	}

	private boolean shouldWriteAnalysisFile(final int currentIteration, final int writeCountsInterval) {
		return currentIteration > 0 && currentIteration % writeCountsInterval == 0;
	}
}
