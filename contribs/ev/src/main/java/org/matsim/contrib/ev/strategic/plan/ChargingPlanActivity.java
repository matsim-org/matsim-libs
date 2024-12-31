package org.matsim.contrib.ev.strategic.plan;

import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.ev.infrastructure.Charger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Preconditions;

/**
 * This class represents the individual charging activities that are to be
 * implemented throughout a day. A charging activity can either be leg-based in
 * which case the leg along which the agent intends to charge is saved.
 * Alternatively, a charging activity can be activity-based in which case the
 * activity sequence during which the vehicle is charged is given.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ChargingPlanActivity {
	@JsonProperty
	private int startActivityIndex = -1;

	@JsonProperty
	private int endActivityIndex = -1;

	@JsonProperty
	private int followingActivityIndex = -1;

	@JsonProperty
	private double duration = 0.0;

	@JsonProperty
	@JsonSerialize(using = IdSerializer.class)
	@JsonDeserialize(using = IdDeserializer.class)
	private Id<Charger> chargerId;

	ChargingPlanActivity() {
	}

	public ChargingPlanActivity(int startActivityIndex, int endActivityIndex, Id<Charger> chargerId) {
		Preconditions.checkArgument(startActivityIndex >= 0);
		Preconditions.checkArgument(endActivityIndex >= startActivityIndex);
		Preconditions.checkNotNull(chargerId);

		this.startActivityIndex = startActivityIndex;
		this.endActivityIndex = endActivityIndex;
		this.chargerId = chargerId;
	}

	public ChargingPlanActivity(int followingActivityIndex, double duration, Id<Charger> chargerId) {
		Preconditions.checkArgument(followingActivityIndex >= 0);
		Preconditions.checkArgument(duration > 0.0);
		Preconditions.checkNotNull(chargerId);

		this.followingActivityIndex = followingActivityIndex;
		this.duration = duration;
		this.chargerId = chargerId;
	}

	public int getStartActivityIndex() {
		return startActivityIndex;
	}

	public int getEndActivityIndex() {
		return endActivityIndex;
	}

	public int getFollowingActivityIndex() {
		return followingActivityIndex;
	}

	public double getDuration() {
		return duration;
	}

	public Id<Charger> getChargerId() {
		return chargerId;
	}

	// Convenience accessors

	@JsonIgnore
	public boolean isEnroute() {
		return followingActivityIndex >= 0;
	}

	// (de)serialization

	static public class IdSerializer extends JsonSerializer<Id<Charger>> {
		@Override
		public void serialize(Id<Charger> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeString(value.toString());
		}
	}

	static public class IdDeserializer extends JsonDeserializer<Id<Charger>> {
		@Override
		public Id<Charger> deserialize(JsonParser p, DeserializationContext context)
				throws IOException, JacksonException {
			return Id.create(p.readValueAs(String.class), Charger.class);
		}
	}

	// Copy

	ChargingPlanActivity createCopy() {
		ChargingPlanActivity copy = new ChargingPlanActivity();

		copy.startActivityIndex = startActivityIndex;
		copy.endActivityIndex = endActivityIndex;
		copy.followingActivityIndex = followingActivityIndex;
		copy.duration = duration;
		copy.chargerId = chargerId;

		return copy;
	}
}
