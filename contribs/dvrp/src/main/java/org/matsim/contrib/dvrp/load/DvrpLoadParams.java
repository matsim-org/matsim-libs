package org.matsim.contrib.dvrp.load;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Preconditions;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class DvrpLoadParams extends ReflectiveConfigGroup {
    static public final String SET_NAME = "load";

    public DvrpLoadParams() {
        super(SET_NAME);
    }

    public enum VehicleCapacitySource {
        Attributes, VehicleTypeCapacity
    }

    @Parameter
    @NotEmpty
    @Comment("the available capacity / load dimensions per vehicle / request")
    public List<String> dimensions = new LinkedList<>(Collections.singleton("passengers"));

    @Parameter
    @Comment("maps the seat capacity of the dvrp vehicle type to a specific dimension")
    public String mapVehicleTypeSeats = "passengers";

    @Parameter
    @Comment("maps the standing room capacity of the dvrp vehicle type to a specific dimension")
    public String mapVehicleTypeStandingRoom = null;

    @Parameter
    @Comment("maps the volume capacity of the dvrp vehicle type to a specific dimension")
    public String mapVehicleTypeVolume = null;

    @Parameter
    @Comment("maps the weight capacity of a the dvrp vehicle type to a specific dimension")
    public String mapVehicleTypeWeight = null;

    @Parameter
    @Comment("maps the other capacity of a the dvrp vehicle type to a specific dimension")
    public String mapVehicleTypeOther = null;

    @Parameter
    @Comment("maps the vehicle capacity when loading from dvrp fleet format to a specific dimension")
    public String mapFleetCapacity = "passengers";

    @Parameter
    @Comment("if no other load information is given, each request obtains a unit load for the given dimension")
    public String defaultRequestDimension = "passengers";

    @Parameter
    @Comment("Defines how often to write analysis on capacities and loads of the mode")
    @PositiveOrZero
    public int analysisInterval = 0;

    @Override
    public void checkConsistency(Config config) {
        super.checkConsistency(config);

        Preconditions.checkState(mapVehicleTypeSeats == null || dimensions.contains(mapVehicleTypeSeats),
                "the dimension configured for the vehicle type seat capacity does not exist");

        Preconditions.checkState(mapVehicleTypeStandingRoom == null || dimensions.contains(mapVehicleTypeStandingRoom),
                "the dimension configured for the vehicle type standing room capacity does not exist");

        Preconditions.checkState(mapVehicleTypeVolume == null || dimensions.contains(mapVehicleTypeVolume),
                "the dimension configured for the vehicle type volume capacity does not exist");

        Preconditions.checkState(mapVehicleTypeWeight == null || dimensions.contains(mapVehicleTypeWeight),
                "the dimension configured for the vehicle type weight capacity does not exist");

        Preconditions.checkState(mapVehicleTypeOther == null || dimensions.contains(mapVehicleTypeOther),
                "the dimension configured for the vehicle type other capacity does not exist");

        Preconditions.checkState(mapFleetCapacity == null || dimensions.contains(mapFleetCapacity),
                "the dimension configured for the dvrp fleet vehicle capacity does not exist");
    }
}
