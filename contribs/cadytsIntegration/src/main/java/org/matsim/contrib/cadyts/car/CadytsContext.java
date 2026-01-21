package org.matsim.contrib.cadyts.car;

import cadyts.calibrators.analytical.AnalyticalCalibrator;
import cadyts.supply.SimResults;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.general.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.counts.Counts;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import static org.matsim.contrib.cadyts.general.CadytsBuilderImpl.*;

public class CadytsContext implements CadytsContextI<Link>, StartupListener, IterationEndsListener, BeforeMobsimListener {

	private final static Logger log = LogManager.getLogger(CadytsContext.class);
	private final static String LINKOFFSET_FILENAME = "linkCostOffsets.xml";
	private static final String FLOWANALYSIS_FILENAME = "flowAnalysis.txt";

	private final double countsScaleFactor;
	private final Counts<Link> calibrationCounts;
	private final boolean writeAnalysisFile;

	private AnalyticalCalibrator<Link> calibrator;
	private PlansTranslatorBasedOnEvents plansTranslator;
	private SimResults<Link> simResults;
	private Scenario scenario;
	private EventsManager eventsManager;
	private OutputDirectoryHierarchy controlerIO;

	private PcuVolumesAnalyzer pcuVolumesAnalyzer;

	@Inject CadytsContext( Config config, Scenario scenario, @Named(CadytsCarModule.CALIBRATION) Counts<Link> calibrationCounts,
						   EventsManager eventsManager, OutputDirectoryHierarchy controlerIO) {
		this.scenario = scenario;
		this.calibrationCounts = calibrationCounts;
		this.eventsManager = eventsManager;
		this.controlerIO = controlerIO;
		this.countsScaleFactor = config.counts().getCountsScaleFactor();

		CadytsConfigGroup cadytsConfig = ConfigUtils.addOrGetModule(config, CadytsConfigGroup.class);
		cadytsConfig.setWriteAnalysisFile(true);

		if ( cadytsConfig.getCalibratedLinks().isEmpty() ){
			Set<String> countedLinks = new TreeSet<>();
			for( Id<Link> id : this.calibrationCounts.getCounts().keySet() ){
				countedLinks.add( id.toString() );
			}
			cadytsConfig.setCalibratedLinks( countedLinks );
		}
		this.writeAnalysisFile = cadytsConfig.isWriteAnalysisFile();
	}

	@Override
	public PlansTranslator<Link> getPlansTranslator() {
		return this.plansTranslator;
	}

	@Override
	public double getAgentWeight(Person person) {
		// Look up the vehicle for the person and return its PCU
		if (scenario.getVehicles() != null) {
			// Assuming standard MATSim mapping where vehicleId == personId
			Vehicle v = scenario.getVehicles().getVehicles().get(Id.create(person.getId(), Vehicle.class));
			if (v != null && v.getType() != null) {
				return v.getType().getPcuEquivalents();
			}
		}
		return 1.0;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// Initialize the PCU analyzer
		this.pcuVolumesAnalyzer = new PcuVolumesAnalyzer(scenario.getVehicles(), 3600, 30 * 3600);
		this.eventsManager.addHandler(this.pcuVolumesAnalyzer);

		this.simResults = new SimResultsContainerImpl(this.pcuVolumesAnalyzer, this.countsScaleFactor);
		this.plansTranslator = new PlansTranslatorBasedOnEvents(scenario);
		this.eventsManager.addHandler(plansTranslator);

		this.calibrator = buildCalibratorAndAddMeasurements(scenario.getConfig(), this.calibrationCounts , new LinkLookUp(scenario), Link.class );
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			this.calibrator.addToDemand(plansTranslator.getCadytsPlan(person.getSelectedPlan()));
		}
	}

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {
		if (this.writeAnalysisFile) {
			String analysisFilepath = null;
			if (isActiveInThisIteration(event.getIteration(), scenario.getConfig())) {
				analysisFilepath = controlerIO.getIterationFilename(event.getIteration(), FLOWANALYSIS_FILENAME);
			}
			this.calibrator.setFlowAnalysisFile(analysisFilepath);
		}

		this.calibrator.afterNetworkLoading(this.simResults);

		String filename = controlerIO.getIterationFilename(event.getIteration(), LINKOFFSET_FILENAME);
		try {
			new CadytsCostOffsetsXMLFileIO<>(new LinkLookUp(scenario), Link.class)
				.write(filename, this.calibrator.getLinkCostOffsets());
		} catch (IOException e) {
			log.error("Could not write link cost offsets!", e);
		}
	}

	@Override
	public AnalyticalCalibrator<Link> getCalibrator() {
		return this.calibrator;
	}

	private static boolean isActiveInThisIteration(final int iter, final Config config) {
		return (iter > 0 && iter % config.counts().getWriteCountsInterval() == 0);
	}
}
