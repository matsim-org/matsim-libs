package org.matsim.dsim.simulation;

import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

@Builder(setterPrefix = "set")
@Data
@Setter
public class SimpleActivity implements Activity {

    @Builder.Default
    private OptionalTime maximumDuration = OptionalTime.undefined();

    @Builder.Default
    private OptionalTime startTime = OptionalTime.undefined();

    @Builder.Default
    private OptionalTime endTime = OptionalTime.undefined();

    private String type;
    private Coord coord;
    private Id<Link> linkId;
    private Id<ActivityFacility> facilityId;

    private final Attributes attributes = new AttributesImpl();

    @Override
    public void setStartTime(double seconds) {
        this.startTime = OptionalTime.defined(seconds);
    }

    @Override
    public void setStartTimeUndefined() {
        this.startTime = OptionalTime.undefined();
    }

    @Override
    public void setEndTime(double seconds) {
        this.endTime = OptionalTime.defined(seconds);
    }

    @Override
    public void setEndTimeUndefined() {
        this.endTime = OptionalTime.undefined();
    }

    @Override
    public void setMaximumDurationUndefined() {
        this.maximumDuration = OptionalTime.undefined();
    }

    @Override
    public void setMaximumDuration(double seconds) {
        this.maximumDuration = OptionalTime.defined(seconds);
    }
}
