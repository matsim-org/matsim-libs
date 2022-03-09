package org.matsim.contrib.shared_mobility.io;

import java.util.Objects;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.service.SharingStation;

public class ImmutableSharingStationSpecification implements SharingStationSpecification {
	private final Id<SharingStation> id;
	private final Id<Link> linkId;
	private final int capacity;

	ImmutableSharingStationSpecification(Builder builder) {
		this.id = Objects.requireNonNull(builder.id);
		this.linkId = Objects.requireNonNull(builder.linkId);
		this.capacity = Objects.requireNonNull(builder.capacity);
	}

	@Override
	public Id<SharingStation> getId() {
		return id;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static final class Builder {
		private Id<SharingStation> id;
		private Id<Link> linkId;
		private Integer capacity;

		private Builder() {
		}

		public Builder id(Id<SharingStation> val) {
			id = val;
			return this;
		}

		public Builder linkId(Id<Link> val) {
			linkId = val;
			return this;
		}

		public Builder capacity(int val) {
			capacity = val;
			return this;
		}

		public ImmutableSharingStationSpecification build() {
			return new ImmutableSharingStationSpecification(this);
		}
	}
}
