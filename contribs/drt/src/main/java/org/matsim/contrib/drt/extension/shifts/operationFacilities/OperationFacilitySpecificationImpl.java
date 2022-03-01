package org.matsim.contrib.drt.extension.shifts.operationFacilities;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilitySpecificationImpl implements OperationFacilitySpecification {

	private final Id<OperationFacility> id;
	private final Id<Link> linkId;
	private final Coord coord;
	private final int capacity;
	private final List<Id<Charger>> chargerIds;
	private final OperationFacilityType type;

	private OperationFacilitySpecificationImpl(Builder builder) {
		this.id = builder.id;
		this.linkId = builder.linkId;
		this.coord = builder.coord;
		this.capacity = builder.capacity;
		this.chargerIds = builder.chargerIds == null ? Collections.EMPTY_LIST : builder.chargerIds;
		this.type = builder.type;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Builder newBuilder(OperationFacilitySpecificationImpl copy) {
		Builder builder = new Builder();
		builder.id = copy.id;
		builder.linkId = copy.linkId;
		builder.coord = copy.coord;
		builder.capacity = copy.capacity;
		builder.chargerIds = copy.chargerIds;
		builder.type = copy.type;
		return builder;
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

	@Override
	public List<Id<Charger>> getChargers() {
		return chargerIds;
	}

	@Override
	public OperationFacilityType getType() {
		return type;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Coord getCoord() {
		return coord;
	}

	@Override
	public Id<OperationFacility> getId() {
		return id;
	}

	public static final class Builder {

		private Id<OperationFacility> id;
		private Id<Link> linkId;
		private Coord coord;
		private int capacity;
		private List<Id<Charger>> chargerIds;
		private OperationFacilityType type;

		private Builder() {
		}

		public Builder id(Id<OperationFacility> val) {
			id = val;
			return this;
		}

		public Builder linkId(Id<Link> val) {
			linkId = val;
			return this;
		}

		public Builder coord(Coord val) {
			coord = val;
			return this;
		}

		public Builder capacity(int val) {
			capacity = val;
			return this;
		}

		public Builder addChargerId(Id<Charger> val) {
			if(chargerIds == null) {
				chargerIds = new ArrayList<>();
			}
			chargerIds.add(val);
			return this;
		}

		public Builder type(OperationFacilityType val) {
			type = val;
			return this;
		}

		public OperationFacilitySpecificationImpl build() {
			return new OperationFacilitySpecificationImpl(this);
		}
	}
}
