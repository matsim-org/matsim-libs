package org.matsim.api.core.v01.messages;


import it.unimi.dsi.fastutil.ints.IntSet;
import org.matsim.api.core.v01.Message;

import java.util.Objects;

/**
 * Holds messages types that need to be sent to a particular compute node.
 */
public class EventRegistry implements Message {

	private final int rank;
	private final IntSet eventTypes;

	private final double syncStep;

	EventRegistry(int rank, IntSet eventTypes, double syncStep) {
		this.rank = rank;
		this.eventTypes = eventTypes;
		this.syncStep = syncStep;
	}

	public static EventRegistryBuilder builder() {
		return new EventRegistryBuilder();
	}

	public int getRank() {
		return this.rank;
	}

	public IntSet getEventTypes() {
		return this.eventTypes;
	}

	public double getSyncStep() {
		return this.syncStep;
	}

	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof EventRegistry other)) return false;
		if (!other.canEqual(this)) return false;
		if (this.getRank() != other.getRank()) return false;
		final Object this$eventTypes = this.getEventTypes();
		final Object other$eventTypes = other.getEventTypes();
		if (!Objects.equals(this$eventTypes, other$eventTypes)) return false;
		return Double.compare(this.getSyncStep(), other.getSyncStep()) == 0;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof EventRegistry;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		result = result * PRIME + this.getRank();
		final Object $eventTypes = this.getEventTypes();
		result = result * PRIME + ($eventTypes == null ? 43 : $eventTypes.hashCode());
		final long $syncStep = Double.doubleToLongBits(this.getSyncStep());
		result = result * PRIME + (int) ($syncStep >>> 32 ^ $syncStep);
		return result;
	}

	public String toString() {
		return "EventRegistry(rank=" + this.getRank() + ", eventTypes=" + this.getEventTypes() + ", syncStep=" + this.getSyncStep() + ")";
	}

	public static class EventRegistryBuilder {
		private int rank;
		private IntSet eventTypes;
		private double syncStep;

		EventRegistryBuilder() {
		}

		public EventRegistryBuilder rank(int rank) {
			this.rank = rank;
			return this;
		}

		public EventRegistryBuilder eventTypes(IntSet eventTypes) {
			this.eventTypes = eventTypes;
			return this;
		}

		public EventRegistryBuilder syncStep(double syncStep) {
			this.syncStep = syncStep;
			return this;
		}

		public EventRegistry build() {
			return new EventRegistry(this.rank, this.eventTypes, this.syncStep);
		}

		public String toString() {
			return "EventRegistry.EventRegistryBuilder(rank=" + this.rank + ", eventTypes=" + this.eventTypes + ", syncStep=" + this.syncStep + ")";
		}
	}
}
