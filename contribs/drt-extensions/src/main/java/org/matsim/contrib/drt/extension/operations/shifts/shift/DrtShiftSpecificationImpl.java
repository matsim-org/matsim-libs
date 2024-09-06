package org.matsim.contrib.drt.extension.operations.shifts.shift;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.extension.operations.operationFacilities.OperationFacility;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public class DrtShiftSpecificationImpl implements DrtShiftSpecification {

	private final Id<DrtShift> id;
	private final double start;
	private final double end;
	private final DrtShiftBreakSpecification shiftBreak;
	private final Id<OperationFacility> operationFacilityId;
	private final Id<DvrpVehicle> designatedVehicleId;

	private DrtShiftSpecificationImpl(Builder builder) {
		this.id = builder.id;
		this.start = builder.start;
		this.end = builder.end;
		this.shiftBreak = builder.shiftBreak;
		this.operationFacilityId = builder.operationFacilityId;
		this.designatedVehicleId = builder.designatedVehicleId;
	}

	@Override
	public double getStartTime() {
		return start;
	}

	@Override
	public double getEndTime() {
		return end;
	}

	@Override
	public Optional<DrtShiftBreakSpecification> getBreak() {
		return Optional.ofNullable(shiftBreak);
	}

	@Override
	public Optional<Id<OperationFacility>> getOperationFacilityId() {
		return Optional.ofNullable(operationFacilityId);
	}

	@Override
	public Optional<Id<DvrpVehicle>> getDesignatedVehicleId() {
		return Optional.ofNullable(designatedVehicleId);
	}

	@Override
	public Id<DrtShift> getId() {
		return id;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(DrtShiftSpecificationImpl copy) {
		Builder builder = new Builder();
		builder.id = copy.getId();
		builder.start = copy.getStartTime();
		builder.end = copy.getEndTime();
		builder.shiftBreak = copy.getBreak().orElse(null);
		builder.operationFacilityId = copy.getOperationFacilityId().orElse(null);
		return builder;
	}

	public static final class Builder {
		private Id<DrtShift> id;
		private double start;
		private double end;
		private DrtShiftBreakSpecification shiftBreak;
		private Id<OperationFacility> operationFacilityId;
		public Id<DvrpVehicle> designatedVehicleId;

		private Builder() {
		}

		public Builder id(Id<DrtShift> val) {
			id = val;
			return this;
		}

		public Builder start(double val) {
			start = val;
			return this;
		}

		public Builder end(double val) {
			end = val;
			return this;
		}

		public Builder shiftBreak(DrtShiftBreakSpecification val) {
			shiftBreak = val;
			return this;
		}

		public Builder operationFacility(Id<OperationFacility> operationFacilityId) {
			this.operationFacilityId = operationFacilityId;
			return this;
		}
		public Builder designatedVehicle(Id<DvrpVehicle> designatedVehicleId) {
			this.designatedVehicleId = designatedVehicleId;
			return this;
		}

		public DrtShiftSpecificationImpl build() {
			return new DrtShiftSpecificationImpl(this);
		}
	}
}
