package org.matsim.analysis;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Message;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@DistributedEventHandler(value = DistributedMode.PARTITION, processing = ProcessingMode.DIRECT)
public class PartVolumesAnalyzer implements LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, AggregatingEventHandler<PartVolumesAnalyzer.Msg> {

	private final Map<Id<Vehicle>, String> veh2mode = new HashMap<>();
	private final Map<Id<Link>, Map<String, int[]>> link2mode = new HashMap<>();
	private final int maxSlotIndex;
	private final int timeBinSize = 3600;

	@Inject
	public PartVolumesAnalyzer(Config config, VolumesAnalyzerModule.AnalyzerRegistry registry) {
		var endTime = config.dsim().getEndTime();
		var startTime = config.dsim().getStartTime();
		this.maxSlotIndex = (int) ((endTime - startTime) / timeBinSize);
		registry.register(this);
	}

	@Override
	public Msg send() {
		return new Msg(link2mode);
	}

	@Override
	public void receive(List<Msg> messages) {
		for (var msg : messages) {
			link2mode.putAll(msg.volumes());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		var mode2volume = link2mode.computeIfAbsent(event.getLinkId(), id -> new HashMap<>());
		var mode = veh2mode.get(event.getVehicleId());
		var volumes = mode2volume.computeIfAbsent(mode, m -> new int[maxSlotIndex + 1]);
		var index = getTimeslotIndex(event.getTime());
		volumes[index]++;
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		veh2mode.put(event.getVehicleId(), event.getNetworkMode());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		veh2mode.remove(event.getVehicleId());
	}

	/**
	 * @param linkId
	 * @return Array containing the number of vehicles leaving the link <code>linkId</code> per time bin,
	 * starting with time bin 0 from 0 seconds to (timeBinSize-1)seconds.
	 */
	public int[] getVolumesForLink(final Id<Link> linkId) {
		return this.link2mode.get(linkId).values().stream()
			.reduce(new int[maxSlotIndex + 1], (result, a) -> {
				for (int i = 0; i < a.length; i++){
					result[i] += a[i];
				}
				return result;
			});
	}

	public int[] getVolumesForLink(final Id<Link> linkId, String mode) {
		Map<String, int[]> modeVolumes = this.link2mode.get(linkId);
		if (modeVolumes != null) return modeVolumes.get(mode);
		return null;
	}

	/**
	 * @return The size of the arrays returned by calls to the {@link #getVolumesForLink(Id)} and the {@link #getVolumesForLink(Id, String)}
	 * methods.
	 */
	public int getVolumesArraySize() {
		return this.maxSlotIndex + 1;
	}

	public double[] getVolumesPerHourForLink(final Id<Link> linkId) {
		if (3600.0 % this.timeBinSize != 0)
			throw new RuntimeException("Volumes per hour and per link probably not correct!");

		int[] volumesForLink = this.getVolumesForLink(linkId);
		return volumesPerHour(volumesForLink);
	}

	public double[] getVolumesPerHourForLink(final Id<Link> linkId, String mode) {
		if (3600.0 % this.timeBinSize != 0)
			throw new RuntimeException("Volumes per hour and per link probably not correct!");

		var volumesForLink = this.getVolumesForLink(linkId, mode);
		return volumesPerHour(volumesForLink);
	}

	private double[] volumesPerHour(int[] volumes) {
		if (3600.0 % this.timeBinSize != 0)
			throw new RuntimeException("Volumes per hour and per link probably not correct!");

		var slotsPerHour = 3600 / this.timeBinSize;
		var slots = (maxSlotIndex + 1) * slotsPerHour;
		double[] result = new double[slots];

		if (volumes == null) return result;

		for (int hour = 0; hour < slots; hour++) {
			double time = hour * 3600.0;
			for (int i = 0; i < slotsPerHour; i++) {
				result[hour] += result[this.getTimeslotIndex(time)];
				time += this.timeBinSize;
			}
		}
		return result;
	}

	private int getTimeslotIndex(double time) {
		var index = ((int) time / timeBinSize);
		return Math.min(index, maxSlotIndex);
	}

	@Override
	public void reset(int iteration) {
		this.link2mode.clear();
		this.veh2mode.clear();
	}

	public record Msg(Map<Id<Link>, Map<String, int[]>> volumes) implements Message {}
}
