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

    public @NotEmpty List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(@NotEmpty List<String> dimensions) {
        this.dimensions = dimensions;
    }

    public String getMapVehicleTypeSeats() {
        return mapVehicleTypeSeats;
    }

    public void setMapVehicleTypeSeats(String mapVehicleTypeSeats) {
        this.mapVehicleTypeSeats = mapVehicleTypeSeats;
    }

    public String getMapVehicleTypeStandingRoom() {
        return mapVehicleTypeStandingRoom;
    }

    public void setMapVehicleTypeStandingRoom(String mapVehicleTypeStandingRoom) {
        this.mapVehicleTypeStandingRoom = mapVehicleTypeStandingRoom;
    }

    public String getMapVehicleTypeVolume() {
        return mapVehicleTypeVolume;
    }

    public void setMapVehicleTypeVolume(String mapVehicleTypeVolume) {
        this.mapVehicleTypeVolume = mapVehicleTypeVolume;
    }

    public String getMapVehicleTypeWeight() {
        return mapVehicleTypeWeight;
    }

    public void setMapVehicleTypeWeight(String mapVehicleTypeWeight) {
        this.mapVehicleTypeWeight = mapVehicleTypeWeight;
    }

    public String getMapVehicleTypeOther() {
        return mapVehicleTypeOther;
    }

    public void setMapVehicleTypeOther(String mapVehicleTypeOther) {
        this.mapVehicleTypeOther = mapVehicleTypeOther;
    }

    public String getDefaultRequestDimension() {
        return defaultRequestDimension;
    }

    public void setDefaultRequestDimension(String defaultRequestDimension) {
        this.defaultRequestDimension = defaultRequestDimension;
    }

    @PositiveOrZero
    public int getAnalysisInterval() {
        return analysisInterval;
    }

    public void setAnalysisInterval(@PositiveOrZero int analysisInterval) {
        this.analysisInterval = analysisInterval;
    }

    public enum VehicleCapacitySource {
        Attributes, VehicleTypeCapacity
    }

    @Parameter
    @NotEmpty
    @Comment("the available capacity / load dimensions per vehicle / request")
    private List<String> dimensions = new LinkedList<>(Collections.singleton("passengers"));

    @Parameter
    @Comment("maps the seat capacity of the dvrp vehicle type to a specific dimension")
    private String mapVehicleTypeSeats = "passengers";

    @Parameter
    @Comment("maps the standing room capacity of the dvrp vehicle type to a specific dimension")
    private String mapVehicleTypeStandingRoom = null;

    @Parameter
    @Comment("maps the volume capacity of the dvrp vehicle type to a specific dimension")
    private String mapVehicleTypeVolume = null;

    @Parameter
    @Comment("maps the weight capacity of a the dvrp vehicle type to a specific dimension")
    private String mapVehicleTypeWeight = null;

    @Parameter
    @Comment("maps the other capacity of a the dvrp vehicle type to a specific dimension")
    private String mapVehicleTypeOther = null;

    @Parameter
    @Comment("if no other load information is given, each request obtains a unit load for the given dimension")
    private String defaultRequestDimension = "passengers";

    @Parameter
    @Comment("Defines how often to write analysis on capacities and loads of the mode")
    @PositiveOrZero
    private int analysisInterval = 0;

    @Override
    public void checkConsistency(Config config) {
        super.checkConsistency(config);

        Preconditions.checkState(getMapVehicleTypeSeats() == null || getDimensions().contains(getMapVehicleTypeSeats()),
                "the dimension configured for the vehicle type seat capacity does not exist");

        Preconditions.checkState(getMapVehicleTypeStandingRoom() == null || getDimensions().contains(getMapVehicleTypeStandingRoom()),
                "the dimension configured for the vehicle type standing room capacity does not exist");

        Preconditions.checkState(getMapVehicleTypeVolume() == null || getDimensions().contains(getMapVehicleTypeVolume()),
                "the dimension configured for the vehicle type volume capacity does not exist");

        Preconditions.checkState(getMapVehicleTypeWeight() == null || getDimensions().contains(getMapVehicleTypeWeight()),
                "the dimension configured for the vehicle type weight capacity does not exist");

        Preconditions.checkState(getMapVehicleTypeOther() == null || getDimensions().contains(getMapVehicleTypeOther()),
                "the dimension configured for the vehicle type other capacity does not exist");
    }
}
