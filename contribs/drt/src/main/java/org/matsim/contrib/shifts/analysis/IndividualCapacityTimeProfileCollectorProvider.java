package org.matsim.contrib.shifts.analysis;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.common.timeprofile.TimeProfileCollector;
import org.matsim.contrib.common.timeprofile.TimeProfiles;
import org.matsim.contrib.shifts.operationFacilities.OperationFacilities;
import org.matsim.contrib.shifts.operationFacilities.OperationFacilitiesUtils;
import org.matsim.contrib.shifts.operationFacilities.OperationFacility;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

/**
 * @author nkuehnel
 */
public class IndividualCapacityTimeProfileCollectorProvider implements Provider<MobsimListener> {
    private final OperationFacilities facilities;
    private final MatsimServices matsimServices;

    @Inject
    public IndividualCapacityTimeProfileCollectorProvider(Scenario scenario, MatsimServices matsimServices) {
        this.facilities = OperationFacilitiesUtils.getFacilities(scenario);
        this.matsimServices = matsimServices;
    }

    @Override
    public MobsimListener get() {
        TimeProfileCollector.ProfileCalculator calc = createIndividualCapacityCalculator(facilities);
        return new TimeProfileCollector(calc, 300, "individual_operation_facility_capacity_time_profiles", matsimServices);
    }

    public static TimeProfileCollector.ProfileCalculator createIndividualCapacityCalculator(final OperationFacilities facilities) {
        List<OperationFacility> facilitiesList = new ArrayList<>(facilities.getDrtOperationFacilities().values());
        ImmutableList<String> header = facilitiesList.stream().map(f -> f.getId() + "").collect(toImmutableList());
        return TimeProfiles.createProfileCalculator(header, () -> facilitiesList.stream()
                .collect(toImmutableMap(f -> f.getId() + "",
                        f -> (double) f.getRegisteredVehicles().size())));
    }
}
