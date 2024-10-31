package org.matsim.dsim.messages;

import lombok.Builder;
import lombok.Data;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

@Builder(setterPrefix = "set")
@Data
public class VehicleMsg {

    private final Id<Vehicle> id;
    private final PersonMsg driver;

    private final double pce;
    private final double maxV;

}
