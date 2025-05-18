package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.apache.commons.lang.math.IntRange;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.common.util.DistanceUtils;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author nkuehnel / MOIA
 */
public class NearestOperationFacilityWithCapacityFinder implements OperationFacilityFinder {

    private final OperationFacilities operationFacilities;

    private final static Predicate<OperationFacility> hubPredicate = facility -> OperationFacilityType.hub.equals(facility.getType());
    private final static Predicate<OperationFacility> inFieldPredicate = facility -> OperationFacilityType.inField.equals(facility.getType());

    public NearestOperationFacilityWithCapacityFinder(OperationFacilities operationFacilities) {
        this.operationFacilities = operationFacilities;
    }

    @Override
    public Optional<OperationFacility> findFacilityOfType(Coord coord, OperationFacilityType type, IntRange timeRange) {
        Predicate<? super OperationFacility> filter;
        switch (type) {
            case hub:
                filter = hubPredicate;
                break;
            case inField:
                filter = inFieldPredicate;
                break;
            default:
                throw new IllegalArgumentException("Unknown operation facility type!");
        }
        return operationFacilities.getDrtOperationFacilities().values().stream()
                .filter(filter)
                .filter(opFa -> opFa.hasCapacity(timeRange))
                .min(Comparator.comparing(
                        f -> DistanceUtils.calculateSquaredDistance(coord, f.getCoord())));
    }

    @Override
    public Optional<OperationFacility> findFacility(Coord coord, IntRange timeRange) {
        return operationFacilities.getDrtOperationFacilities().values().stream()
                .filter(opFa -> opFa.hasCapacity(timeRange))
                .min(Comparator.comparing(
                        f -> DistanceUtils.calculateSquaredDistance(coord, f.getCoord())));

    }
}
