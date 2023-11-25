package org.matsim.contrib.drt.extension.operations.operationFacilities;

import java.util.Comparator;
import java.util.function.Predicate;
import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.common.util.DistanceUtils;

/**
 * @author nkuehnel / MOIA
 */
public class NearestOperationFacilityWithCapacityFinder implements OperationFacilityFinder {

  private final OperationFacilities operationFacilities;

  private static final Predicate<OperationFacility> hubPredicate =
      facility -> OperationFacilityType.hub.equals(facility.getType());
  private static final Predicate<OperationFacility> inFieldPredicate =
      facility -> OperationFacilityType.inField.equals(facility.getType());

  public NearestOperationFacilityWithCapacityFinder(OperationFacilities operationFacilities) {
    this.operationFacilities = operationFacilities;
  }

  @Override
  public OperationFacility findFacilityOfType(Coord coord, OperationFacilityType type) {
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
        .filter(OperationFacility::hasCapacity)
        .min(Comparator.comparing(f -> DistanceUtils.calculateSquaredDistance(coord, f.getCoord())))
        .orElse(null);
  }

  @Override
  public OperationFacility findFacility(Coord coord) {
    return operationFacilities.getDrtOperationFacilities().values().stream()
        .filter(OperationFacility::hasCapacity)
        .min(Comparator.comparing(f -> DistanceUtils.calculateSquaredDistance(coord, f.getCoord())))
        .orElse(null);
  }
}
