package org.matsim.contrib.util.stats;

import static java.util.Map.Entry;

import java.awt.Paint;
import java.util.Comparator;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;

import com.google.common.collect.ImmutableMap;

import one.util.streamex.EntryStream;

public class VehicleTaskProfileView implements ProfileWriter.ProfileView {

	private final VehicleTaskProfileCalculator calculator;
	private final Comparator<Task.TaskType> taskTypeComparator;
	private final Map<String, Paint> seriesPaints;

	public VehicleTaskProfileView(VehicleTaskProfileCalculator calculator, Comparator<Task.TaskType> taskTypeComparator,
			Map<Task.TaskType, Paint> taskTypePaints) {
		this.calculator = calculator;
		this.taskTypeComparator = taskTypeComparator;
		seriesPaints = EntryStream.of(taskTypePaints).mapKeys(Task.TaskType::name).toMap();
	}

	@Override
	public ImmutableMap<String, double[]> profiles() {
		// stream tasks which are not related to passenger (unoccupied vehicle)
		return calculator.getTaskProfiles()
				.entrySet()
				.stream()
				.sorted(Entry.comparingByKey(taskTypeComparator))
				.map(e -> Pair.of(e.getKey().name(), e.getValue()))
				.collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
	}

	@Override
	public Map<String, Paint> seriesPaints() {
		return seriesPaints;
	}

	@Override
	public TimeDiscretizer timeDiscretizer() {
		return calculator.getTimeDiscretizer();
	}
}
