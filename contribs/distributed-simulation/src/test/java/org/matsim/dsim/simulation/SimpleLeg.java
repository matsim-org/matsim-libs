package org.matsim.dsim.simulation;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;


@Builder(setterPrefix = "set", toBuilder = true)
@Getter
@Setter
public class SimpleLeg implements Leg {

    private String mode;
    private String routingMode;
    private Route route;

    @Builder.Default
    private OptionalTime departureTime = OptionalTime.undefined();

    @Builder.Default
    private OptionalTime travelTime = OptionalTime.undefined();

    private Attributes attributes = new AttributesImpl();

    public void setDepartureTime(double seconds) {
        departureTime = OptionalTime.defined(seconds);
    }

    @Override
    public void setDepartureTimeUndefined() {
        departureTime = OptionalTime.undefined();
    }

    @Override
    public void setTravelTime(double seconds) {
        travelTime = OptionalTime.defined(seconds);
    }

    @Override
    public void setTravelTimeUndefined() {
        travelTime =  OptionalTime.undefined();
    }
}
