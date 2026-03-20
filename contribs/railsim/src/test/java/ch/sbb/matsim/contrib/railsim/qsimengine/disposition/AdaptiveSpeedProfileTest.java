package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.ArrayList;
import java.util.List;

import static ch.sbb.matsim.contrib.railsim.qsimengine.disposition.AdaptiveSpeedProfile.BUFFER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdaptiveSpeedProfileTest {

    private AdaptiveSpeedProfile speedProfile;
    private TrainPosition mockPosition;
	private VehicleType mockVehicleType;

    @BeforeEach
    void setUp() {
        speedProfile = new AdaptiveSpeedProfile();
        mockPosition = mock(TrainPosition.class);
		MobsimDriverAgent mockDriver = mock(MobsimDriverAgent.class);
		MobsimVehicle mockVehicle = mock(MobsimVehicle.class);
		Vehicle mockVehicleImpl = mock(Vehicle.class);
        mockVehicleType = mock(VehicleType.class);

        when(mockPosition.getDriver()).thenReturn(mockDriver);
        when(mockDriver.getVehicle()).thenReturn(mockVehicle);
        when(mockVehicle.getVehicle()).thenReturn(mockVehicleImpl);
        when(mockVehicleImpl.getType()).thenReturn(mockVehicleType);
    }

    @Test
    void testGetTargetSpeed_WithSufficientTime() {
        // Given: 1000m route, 100s remaining time, max speed 20 m/s
        double currentTime = 100.0;
        double arrivalTime = currentTime + 100.0 + BUFFER; // 100s remaining + buffer
        double totalLength = 1000.0;
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Required speed = 1000m / 100s = 10 m/s, but minimum is 10 m/s
        assertThat(targetSpeed).isEqualTo(10.0);
    }

    @Test
    void testGetTargetSpeed_WithInsufficientTime() {
        // Given: 1000m route, 30s remaining time, max speed 20 m/s
        double currentTime = 100.0;
        double arrivalTime = currentTime + 30.0 + BUFFER; // 30s remaining + buffer
        double totalLength = 1000.0;
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Required speed = 1000m / 30s = 33.33 m/s, but minimum is 10 m/s
        assertThat(targetSpeed).isCloseTo(100d / 3,  Offset.offset(0.001));
    }

    @Test
    void testGetTargetSpeed_WithNoTimeLeft() {
        // Given: No time remaining
        double currentTime = 200.0;
        double arrivalTime = 200.0 + BUFFER; // No time remaining + buffer
        double totalLength = 1000.0;
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Should return maximum speed (infinity in this case)
        assertThat(targetSpeed).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void testGetTargetSpeed_WithNegativeTime() {
        // Given: Negative time remaining (already late)
        double currentTime = 250.0;
        double arrivalTime = 200.0 + BUFFER; // -50s remaining (late) + buffer
        double totalLength = 1000.0;
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Should return maximum speed (infinity in this case)
        assertThat(targetSpeed).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void testGetTargetSpeed_WithZeroDistance() {
        // Given: Zero distance to travel
        double currentTime = 100.0;
        double arrivalTime = currentTime + 100.0 + BUFFER; // 100s remaining + buffer
        double totalLength = 0.0;
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Should return maximum speed (infinity in this case)
        assertThat(targetSpeed).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void testGetTargetSpeed_WithMultipleLinks() {
        // Given: Multiple links with different speeds
        double currentTime = 100.0;
        double arrivalTime = currentTime + 100.0 + BUFFER; // 100s remaining + buffer

        // Create links with different speeds: 500m at 10 m/s, 500m at 30 m/s
        List<RailLink> links = new ArrayList<>();
        links.add(createRealRailLink(500.0, 10.0));
        links.add(createRealRailLink(500.0, 30.0));

        PlannedArrival plannedArrival = new PlannedArrival(arrivalTime, links);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Required speed = 1000m / 100s = 10 m/s, but minimum is 10 m/s
        assertThat(targetSpeed).isEqualTo(10.0);
    }

    @Test
    void testGetTargetSpeed_WithBufferTime() {
        // Given: Buffer time should be subtracted from arrival time
        double currentTime = 100.0;
        double arrivalTime = currentTime + 20.0 + BUFFER; // 20s remaining + buffer
        double totalLength = 1000.0;
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Effective remaining time = 20s (after buffer subtraction)
        // Required speed = 1000m / 20s = 50 m/s, but minimum is 10 m/s
        assertThat(targetSpeed).isEqualTo(50.0);
    }

    @Test
    void testGetTargetSpeed_WithExactTimeMatch() {
        // Given: Exact time to reach destination
        double currentTime = 100.0;
        double arrivalTime = currentTime + 50.0 + BUFFER; // 50s remaining + buffer
        double totalLength = 1000.0;
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Required speed = 1000m / 50s = 20 m/s
        assertThat(targetSpeed).isEqualTo(20.0);
    }

    @Test
    void testGetTargetSpeed_WithVeryShortDistance() {
        // Given: Very short distance
        double currentTime = 100.0;
        double arrivalTime = currentTime + 100.0 + BUFFER; // 100s remaining + buffer
        double totalLength = 10.0; // Very short distance
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Required speed = 10m / 100s = 0.1 m/s, but minimum is 10 m/s
        assertThat(targetSpeed).isEqualTo(10.0);
    }

    @Test
    void testGetTargetSpeed_WithMinimumSpeedRequirement() {
        // Given: Distance that would require less than 10 m/s
        double currentTime = 100.0;
        double arrivalTime = currentTime + 100.0 + BUFFER; // 100s remaining + buffer
        double totalLength = 500.0; // Would require 5 m/s
        double maxSpeed = 20.0;

        PlannedArrival plannedArrival = createPlannedArrival(arrivalTime, totalLength, maxSpeed);

        // When
        double targetSpeed = speedProfile.getTargetSpeed(currentTime, mockPosition, plannedArrival);

        // Then: Required speed = 500m / 100s = 5 m/s, but minimum is 10 m/s
        assertThat(targetSpeed).isEqualTo(10.0);
    }

    /**
     * Utility method to create a PlannedArrival with a single link.
     */
    private PlannedArrival createPlannedArrival(double arrivalTime, double totalLength, double maxSpeed) {
        List<RailLink> links = new ArrayList<>();
        links.add(createRealRailLink(totalLength, maxSpeed));
        return new PlannedArrival(arrivalTime, links);
    }

    /**
     * Utility method to create a real RailLink by passing a mock Link into the constructor.
     */
    private RailLink createRealRailLink(double length, double allowedSpeed) {
        // Create mock Link
        Link mockLink = mock(Link.class);
        Node mockNode = mock(Node.class);

        when(mockLink.getId()).thenReturn(Id.create("test_link", Link.class));
        when(mockLink.getLength()).thenReturn(length);
        when(mockLink.getFreespeed()).thenReturn(allowedSpeed);
        when(mockLink.getFromNode()).thenReturn(mockNode);
        when(mockLink.getToNode()).thenReturn(mockNode);
        when(mockLink.getAttributes()).thenReturn(new AttributesImpl());

        // Set vehicle type maximum velocity to match allowed speed
        when(mockVehicleType.getMaximumVelocity()).thenReturn(allowedSpeed);

        // Create real RailLink with mock Link and null opposite
        return new RailLink(mockLink, null);
    }
}
