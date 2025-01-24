package org.matsim.contrib.ev.strategic.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.contrib.ev.infrastructure.ChargerSpecification;
import org.matsim.contrib.ev.infrastructure.ChargingInfrastructureSpecification;
import org.matsim.contrib.ev.strategic.StrategicChargingUtils;
import org.matsim.contrib.ev.strategic.scoring.ChargingPlanScoring;
import org.matsim.contrib.ev.withinday.analysis.WithinDayChargingAnalysisHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.util.concurrent.AtomicDouble;

/**
 * Analysis class that writes out high-level indicators for chargers. The
 * analysis is performed based on a the sevc:analysisTypes attribute of the
 * chargers. All chargers having the same anaylsis type are aggregated in the
 * analysis (on the number of users, consumed kWh, ...).
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargerTypeAnalysisListener
		implements IterationStartsListener, IterationEndsListener, PersonMoneyEventHandler {
	static public final String OUTPUT_PATH = "sevc_analysis.csv";
	static public final String ANALYSIS_TYPE_CHARGER_ATTRIBUTE = "sevc:analysisTypes";

	private final EventsManager eventsManager;
	private final WithinDayChargingAnalysisHandler handler;
	private final String outputPath;

	private final IdMap<Charger, Set<String>> analysisMap = new IdMap<>(Charger.class);
	private final Set<String> analysisTypes = new HashSet<>();

	public ChargerTypeAnalysisListener(OutputDirectoryHierarchy outputHierarchy,
			ChargingInfrastructureSpecification infrastructure, WithinDayChargingAnalysisHandler handler,
			EventsManager eventsManager) {
		this.outputPath = outputHierarchy.getOutputFilename(OUTPUT_PATH);
		this.handler = handler;
		this.eventsManager = eventsManager;

		for (ChargerSpecification charger : infrastructure.getChargerSpecifications().values()) {
			String rawTypes = (String) charger.getAttributes().getAttribute(ANALYSIS_TYPE_CHARGER_ATTRIBUTE);
			Set<String> analysisTypes = new HashSet<>();

			if (rawTypes != null) {
				for (String analysisType : rawTypes.split(",")) {
					analysisTypes.add(analysisType.trim());
					this.analysisTypes.add(analysisType.trim());
				}
			}

			analysisMap.put(charger.getId(), analysisTypes);
		}
	}

	static private final List<String> HEADER = Arrays.asList( //
			"iteration", //
			"analysis_type", //
			"successful_attempts", //
			"failed_attempts", //
			"energy_kWh", //
			"charging_duration_min", //
			"idle_duration_min", //
			"wait_duration_min", //
			"revenue", //
			"users");

	private class AnalysisItem {
		AtomicInteger successfulAttempts = new AtomicInteger();
		AtomicInteger failedAttempts = new AtomicInteger();
		AtomicDouble energy_kWh = new AtomicDouble();
		AtomicDouble chargingDuration_min = new AtomicDouble();
		AtomicDouble idleDuration_min = new AtomicDouble();
		AtomicDouble waitDuration_min = new AtomicDouble();
		AtomicDouble revenue = new AtomicDouble();
	}

	private IdMap<Charger, AtomicDouble> revenueTracker = new IdMap<>(Charger.class);

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		eventsManager.addHandler(this);
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if (event.getPurpose().equals(ChargingPlanScoring.MONEY_EVENT_PURPOSE)) {
			Id<Charger> chargerId = Id.create(event.getTransactionPartner(), Charger.class);
			revenueTracker.computeIfAbsent(chargerId, id -> new AtomicDouble()).addAndGet(event.getAmount());
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		eventsManager.removeHandler(this);

		try {
			boolean writeHeader = !(new File(outputPath).exists());
			BufferedWriter writer = IOUtils.getAppendingBufferedWriter(outputPath);

			if (writeHeader) {
				writer.write(String.join(";", HEADER) + "\n");
			}

			Map<String, AnalysisItem> analysisItems = new HashMap<>();
			Map<String, IdSet<Person>> users = new HashMap<>();

			for (String analysisType : analysisTypes) {
				analysisItems.put(analysisType, new AnalysisItem());
				users.put(analysisType, new IdSet<>(Person.class));
			}

			for (var attempt : handler.getChargingAttemptItems()) {
				for (String analysisType : analysisMap.get(attempt.chargerId())) {
					var item = analysisItems.get(analysisType);

					if (attempt.successful()) {
						item.successfulAttempts.incrementAndGet();
						users.get(analysisType).add(attempt.personId());

						double chargingDuration_min = attempt.chargingEndTime() - attempt.chargingStartTime();
						chargingDuration_min /= 60.0;

						double idleDuration_min = attempt.endTime() - attempt.chargingEndTime();
						idleDuration_min /= 60.0;

						double waitDuration_min = attempt.queueingEndTime() - attempt.queueingStartTime();
						waitDuration_min /= 60.0;

						if (Double.isFinite(chargingDuration_min)) {
							item.chargingDuration_min.addAndGet(chargingDuration_min);
						}

						if (Double.isFinite(idleDuration_min)) {
							item.idleDuration_min.addAndGet(idleDuration_min);
						}

						if (Double.isFinite(waitDuration_min)) {
							item.waitDuration_min.addAndGet(waitDuration_min);
						}

						item.energy_kWh.addAndGet(attempt.energy_kWh());
					} else {
						item.failedAttempts.incrementAndGet();
					}
				}
			}

			for (var entry : revenueTracker.entrySet()) {
				for (String analysisType : analysisMap.get(entry.getKey())) {
					var item = analysisItems.get(analysisType);
					item.revenue.addAndGet(entry.getValue().get());
				}
			}

			revenueTracker.clear();

			for (String analysisType : analysisTypes) {
				int usersValue = users.get(analysisType).size();
				var item = analysisItems.get(analysisType);

				writer.write(String.join(";", new String[] {
						String.valueOf(event.getIteration()),
						analysisType,
						String.valueOf(item.successfulAttempts.get()), //
						String.valueOf(item.failedAttempts.get()), //
						String.valueOf(item.energy_kWh.get()),
						String.valueOf(item.chargingDuration_min.get()), //
						String.valueOf(item.idleDuration_min.get()), //
						String.valueOf(item.waitDuration_min.get()), //
						String.valueOf(item.revenue.get()), //
						String.valueOf(usersValue) //
				}) + "\n");

			}

			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Adds an analysis type to a charger
	 */
	static public void addAnalysisType(ChargerSpecification charger, String analysisType) {
		Set<String> analysisTypes = StrategicChargingUtils.readList(charger, ANALYSIS_TYPE_CHARGER_ATTRIBUTE);
		analysisTypes.add(analysisType);
		StrategicChargingUtils.writeList(charger, ANALYSIS_TYPE_CHARGER_ATTRIBUTE, analysisTypes);
	}

	/**
	 * Returns the list of analysis types of a charger
	 */
	static public Set<String> getAnalysisTypes(ChargerSpecification charger) {
		return StrategicChargingUtils.readList(charger, ANALYSIS_TYPE_CHARGER_ATTRIBUTE);
	}
}
