package org.matsim.contrib.shared_mobility.io;

import java.util.Objects;
import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.service.SharingStation;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;

public class ImmutableSharingVehicleSpecification implements SharingVehicleSpecification {
	private final Id<SharingVehicle> id;
	private final Optional<Id<Link>> startLinkId;
	private final Optional<Id<SharingStation>> startStationId;

	ImmutableSharingVehicleSpecification(Builder builder) {
		this.id = Objects.requireNonNull(builder.id);
		this.startLinkId = Optional.ofNullable(builder.startLinkId);
		this.startStationId = Optional.ofNullable(builder.startStationId);
	}

	@Override
	public Id<SharingVehicle> getId() {
		return id;
	}

	@Override
	public Optional<Id<Link>> getStartLinkId() {
		return startLinkId;
	}

	@Override
	public Optional<Id<SharingStation>> getStartStationId() {
		return startStationId;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static final class Builder {
		private Id<SharingVehicle> id;
		private Id<Link> startLinkId;
		private Id<SharingStation> startStationId;

		private Builder() {
		}

		public Builder id(Id<SharingVehicle> val) {
			id = val;
			return this;
		}

		public Builder startLinkId(Id<Link> val) {
			startLinkId = val;
			return this;
		}

		public Builder startStationId(Id<SharingStation> val) {
			startStationId = val;
			return this;
		}

		public ImmutableSharingVehicleSpecification build() {
			return new ImmutableSharingVehicleSpecification(this);
		}
	}
}
