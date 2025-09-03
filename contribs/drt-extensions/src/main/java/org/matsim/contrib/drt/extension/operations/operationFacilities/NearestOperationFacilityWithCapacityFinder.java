package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.common.util.DistanceUtils;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author nkuehnel / MOIA
 */
public class NearestOperationFacilityWithCapacityFinder implements OperationFacilityFinder {

    private final OperationFacilities operationFacilities;

    public NearestOperationFacilityWithCapacityFinder(OperationFacilities operationFacilities) {
        this.operationFacilities = operationFacilities;
    }

    @Override
    public Optional<OperationFacility> findFacility(Coord coord, double fromInclusive, double toInclusive, Set<OperationFacilityType> types) {
        Predicate<OperationFacility> filter = typefilter(types);
        return operationFacilities.getFacilities().values().stream()
                .filter(filter)
                .filter(opFa -> opFa.hasCapacity(fromInclusive, toInclusive))
                .min(Comparator.comparing(
                        f -> DistanceUtils.calculateSquaredDistance(coord, f.getCoord())));
    }

    @Override
    public Optional<OperationFacility> findFacility(Coord coord, double fromInclusive, Set<OperationFacilityType> types) {
        Predicate<OperationFacility> filter = typefilter(types);
        return operationFacilities.getFacilities().values().stream()
                .filter(filter)
                .filter(opFa -> opFa.hasCapacity(fromInclusive))
                .min(Comparator.comparing(
                        f -> DistanceUtils.calculateSquaredDistance(coord, f.getCoord())));
    }

    private static Predicate<OperationFacility> typefilter(Set<OperationFacilityType> types) {
        if (types == null || types.isEmpty()) {
            return f -> true;
        }
        return f -> types.contains(f.getType());
    }
}
