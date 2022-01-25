package org.matsim.vis.snapshotwriters;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import static java.util.Objects.requireNonNull;

/**
 * A helper class to store information about agents (id, position, speed), mainly used to create
 * {@link SnapshotWriter snapshots}.  It also provides a way to convert graph coordinates (linkId, offset) into
 * Euclidean coordinates.  Also does some additional coordinate shifting (e.g. to the "right") to improve visualization.
 * In contrast to earlier versions of this comment, it does _not_ define a physical position of particles in the queue model;
 * that functionality needs to be provided elsewhere.
 * <p>
 * This class has two builders since different parts of the code set different things which are only loosely related. I guess
 * this could be improved...
 *
 * @author mrieser, knagel
 */
public class PositionInfo implements AgentSnapshotInfo {

    private static final double TWO_PI = 2.0 * Math.PI;
    private static final double PI_HALF = Math.PI / 2.0;

    private final Id<Person> agentId;
    private final Id<Link> linkId;
    private final Id<Vehicle> vehicleId;
    private final double easting;
    private final double northing;
    private final double colorValue;
    private final AgentState agentState;
    private final DrivingState drivingState;
    private final int user;

    private PositionInfo(Id<Person> agentId, Id<Link> linkId, Id<Vehicle> vehicleId, double easting, double northing, double colorValue, AgentState agentState, DrivingState drivingState, int user) {
        this.agentId = agentId;
        this.linkId = linkId;
        this.vehicleId = vehicleId;
        this.easting = easting;
        this.northing = northing;
        this.colorValue = colorValue;
        this.agentState = agentState;
        this.drivingState = drivingState;
        this.user = user;
    }

    @Override
    public final Id<Person> getId() {
        return this.agentId;
    }

    @Override
    public final Id<Link> getLinkId() {
        return this.linkId;
    }

    @Override
    public final Id<Vehicle> getVehicleId() {
        return vehicleId;
    }

    @Override
    public final double getEasting() {
        return this.easting;
    }

    @Override
    public final double getNorthing() {
        return this.northing;
    }

    @Override
    public final double getAzimuth() {
        throw new RuntimeException("this is deprecated. Get over it.");
    }

    @Override
    public final double getColorValueBetweenZeroAndOne() {
        return this.colorValue;
    }

    @Override
    public final AgentState getAgentState() {
        return this.agentState;
    }

    @Override
    public final DrivingState getDrivingState() {
        return this.drivingState;
    }

    @Override
    public int getUserDefined() {
        return this.user;
    }

    @Override
    public String toString() {
        return "PositionInfo; agentId: " + this.agentId.toString()
                + " easting: " + this.easting
                + " northing: " + this.northing;
    }

    public static class DirectBuilder {

        private double easting;
        private double northing;
        private Id<Person> personId;
        private double colorValue;
        private int userDefined;
        private AgentState agentState;
        private Id<Link> linkId;
        private Id<Vehicle> vehicleId;

        public DirectBuilder() {
        }

        public DirectBuilder setEasting(double easting) {
            this.easting = easting;
            return this;
        }

        public DirectBuilder setNorthing(double northing) {
            this.northing = northing;
            return this;
        }

        public DirectBuilder setPersonId(Id<Person> personId) {
            this.personId = personId;
            return this;
        }

        public DirectBuilder setColorValue(double colorValue) {
            this.colorValue = colorValue;
            return this;
        }

        public DirectBuilder setUserDefined(int userDefined) {
            this.userDefined = userDefined;
            return this;
        }

        public DirectBuilder setAgentState(AgentState agentState) {
            this.agentState = agentState;
            return this;
        }

        public DirectBuilder setLinkId(Id<Link> id) {
            this.linkId = id;
            return this;
        }

        public DirectBuilder setVehicleId(Id<Vehicle> id) {
            this.vehicleId = id;
            return this;
        }

        public AgentSnapshotInfo build() {
            return new PositionInfo(personId, linkId, vehicleId, easting, northing, colorValue, agentState, null, userDefined);
        }
    }

    public static class LinkBasedBuilder {

        private SnapshotLinkWidthCalculator linkWidthCalculator;

        private Id<Person> agentId = null;
        private Id<Link> linkId = null;
        private Id<Vehicle> vehicleId = null;
        private Coord fromCoord;
        private Coord toCoord;
        private double linkLength;
        private int lane;
        private double distanceOnLink;
        private AgentSnapshotInfo.AgentState agentState = null;
        private AgentSnapshotInfo.DrivingState drivingState = null;
        private double colorValue;
        private int user;

        public LinkBasedBuilder() {
        }

        public LinkBasedBuilder setLinkWidthCalculator(SnapshotLinkWidthCalculator linkWidthCalculator) {
            this.linkWidthCalculator = linkWidthCalculator;
            return this;
        }

        public LinkBasedBuilder setPersonId(Id<Person> personId) {
            this.agentId = personId;
            return this;
        }

        public LinkBasedBuilder setLinkId(Id<Link> linkId) {
            this.linkId = linkId;
            return this;
        }

        public LinkBasedBuilder setVehicleId(Id<Vehicle> vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public LinkBasedBuilder setAgentState(AgentSnapshotInfo.AgentState agentState) {
            this.agentState = agentState;
            return this;
        }

        public LinkBasedBuilder setDrivingState(AgentSnapshotInfo.DrivingState drivingState) {
            this.drivingState = drivingState;
            return this;
        }

        public LinkBasedBuilder setFromCoord(Coord fromCoord) {
            this.fromCoord = fromCoord;
            return this;
        }

        public LinkBasedBuilder setToCoord(Coord toCoord) {
            this.toCoord = toCoord;
            return this;
        }

        public LinkBasedBuilder setLinkLength(double linkLength) {
            this.linkLength = linkLength;
            return this;
        }

        public LinkBasedBuilder setLane(int lane) {
            this.lane = lane;
            return this;
        }

        public LinkBasedBuilder setDistanceOnLink(double distanceOnLink) {
            this.distanceOnLink = distanceOnLink;
            return this;
        }

        public LinkBasedBuilder setColorValue(double colorValue) {
            this.colorValue = colorValue;
            return this;
        }

        public LinkBasedBuilder setUser(int user) {
            this.user = user;
            return this;
        }

        public AgentSnapshotInfo build() {

            requireNonNull(this.linkWidthCalculator);
            requireNonNull(fromCoord);
            requireNonNull(toCoord);

            var theta = calculateTheta(this.fromCoord, this.toCoord);
            var euclideanLength = CoordUtils.calcEuclideanDistance(this.fromCoord, this.toCoord);
            var correction = calculateCorrection(euclideanLength, this.linkLength);
            var lanePosition = linkWidthCalculator.calculateLanePosition(lane);
            var easting = fromCoord.getX()
                    + (Math.cos(theta) * distanceOnLink * correction)
                    + (Math.sin(theta) * lanePosition);
            var northing = fromCoord.getY()
                    + Math.sin(theta) * distanceOnLink * correction
                    - Math.cos(theta) * lanePosition;

            return new PositionInfo(
                    this.agentId, this.linkId, this.vehicleId,
                    easting, northing,
                    colorValue, agentState, drivingState, user);
        }

        private double calculateTheta(Coord startCoord, Coord endCoord) {

            double dx = -startCoord.getX() + endCoord.getX();
            double dy = -startCoord.getY() + endCoord.getY();
            double theta;
            if (dx > 0) {
                theta = Math.atan(dy / dx);
            } else if (dx < 0) {
                theta = Math.PI + Math.atan(dy / dx);
            } else { // i.e. DX==0
                if (dy > 0) {
                    theta = PI_HALF;
                } else if (dy < 0) {
                    theta = -PI_HALF;
                } else { // i.e. DX==0 && DY==0
                    theta = 0.833 * Math.PI; // some default direction towards north north east
                }
            }
            if (theta < 0.0) theta += TWO_PI;

            return theta;
        }

        private double calculateCorrection(double euclideanLength, double curvedLength) {
            return curvedLength != 0 ? euclideanLength / curvedLength : 0;
        }
    }
}