package ch.sbb.matsim.contrib.railsim.events;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrackState;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasLinkId;
import org.matsim.api.core.v01.network.Link;

import java.util.Map;

/**
 * Event thrown when the {@link ch.sbb.matsim.contrib.railsim.qsimengine.TrackState} of a {@link Link} changes.
 */
public class RailsimLinkStateChangeEvent extends Event implements HasLinkId {

	public static final String EVENT_TYPE = "railsimLinkStateChangeEvent";

	private final Id<Link> linkId;
	private final TrackState state;
	private final int track;

	public RailsimLinkStateChangeEvent(double time, Id<Link> linkId, TrackState state, int track) {
		super(time);
		this.linkId = linkId;
		this.state = state;
		this.track = track;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Link> getLinkId() {
		return linkId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put("state", this.state.toString());
		attr.put("track", String.valueOf(track));
		return attr;
	}

}
