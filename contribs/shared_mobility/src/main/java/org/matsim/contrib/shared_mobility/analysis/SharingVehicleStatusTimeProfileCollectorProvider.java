package org.matsim.contrib.shared_mobility.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.histogram.UniformHistogram;
import org.matsim.contrib.common.timeprofile.TimeProfileCharts.ChartType;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector.ProfileCalculator;
import org.matsim.contrib.common.timeprofile.TimeProfiles;
import org.matsim.contrib.shared_mobility.service.SharingUtils.SHARING_VEHICLE_STATES;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class SharingVehicleStatusTimeProfileCollectorProvider implements Provider<MobsimListener> {
	private final VehicleStateCollector vehicleStateCollector;
	private final MatsimServices matsimServices;
	private final String serviceMode;
	private final int interval = 120;

	@Inject
	public SharingVehicleStatusTimeProfileCollectorProvider(VehicleStateCollector vehicleStateCollector, MatsimServices matsimServices, String serviceMode) {
		this.vehicleStateCollector = vehicleStateCollector;
		this.matsimServices = matsimServices;
		this.serviceMode = serviceMode;
	}

	@Override
	public MobsimListener get() {
		ProfileCalculator calc = createSharingVehicleStatusTimeProfileCollector(vehicleStateCollector, serviceMode);
		TimeProfileCollector collector = new TimeProfileCollector(calc, interval, this.serviceMode + "_vehicle_status_time_profiles", matsimServices);
		collector.setChartTypes(ChartType.StackedArea);
		return collector;
	}

	public static ProfileCalculator createSharingVehicleStatusTimeProfileCollector(
			final VehicleStateCollector vehicleStateCollector, String serviceMode) {
		ImmutableList<String> header = ImmutableList.of(SHARING_VEHICLE_STATES.RESERVED.toString(),
				SHARING_VEHICLE_STATES.BOOKED.toString(), SHARING_VEHICLE_STATES.IDLE.toString());
		return TimeProfiles.createProfileCalculator(header, () -> {
			UniformHistogram histogram = new UniformHistogram(1.0, header.size());
			for (Id<SharingVehicle> vehicleId : vehicleStateCollector.getVehicleStatus().keySet()) {
					histogram.addValue((vehicleStateCollector.getVehicleStatus().get(vehicleId)).ordinal());
			}

			ImmutableMap.Builder<String, Double> builder = ImmutableMap.builder();
			for (int b = 0; b < header.size(); b++) {
				builder.put(header.get(b), (double) histogram.getCount(b));
			}
			return builder.build();
		});
	}
}