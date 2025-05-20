package org.matsim.contrib.drt.extension.operations.operationFacilities;

import org.apache.commons.lang.math.IntRange;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;

import java.util.*;

/**
 * @author nkuehnel / MOIA
 */
public class OperationFacilityImpl implements OperationFacility {

    private final Id<OperationFacility> id;
    private final Id<Link> linkId;
    private final Coord coord;
    private final int capacity;
    private final List<Id<Charger>> chargers;
    private final OperationFacilityType type;

    private final CapacityManager capacityManager;

    public OperationFacilityImpl(Id<OperationFacility> id, Id<Link> linkId, Coord coord, int capacity,
                                 List<Id<Charger>> chargers, OperationFacilityType type, double qSimEndTime) {
        this.id = id;
        this.linkId = linkId;
        this.coord = coord;
        this.capacity = capacity;
        this.chargers = Collections.unmodifiableList(chargers);
        this.type = type;
        this.capacityManager = new CapacityManager(capacity, qSimEndTime);
    }

    @Override
    public Id<OperationFacility> getId() {
        return id;
    }

    @Override
    public Id<Link> getLinkId() {
        return linkId;
    }

    @Override
    public Coord getCoord() {
        return coord;
    }

    @Override
    public Map<String, Object> getCustomAttributes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean hasCapacity(IntRange range) {
        return capacityManager.hasCapacity(range);
    }

    @Override
    public boolean registerOrUpdateShiftBreak(Id<DvrpVehicle> vehicleId, IntRange timeRange) {
        return capacityManager.registerShiftBreak(vehicleId, timeRange);
    }

    @Override
    public boolean registerOrUpdateParkingOutOfShift(Id<DvrpVehicle> vehicleId, IntRange timeRange) {
        return capacityManager.registerParkingOutOfShift(vehicleId, timeRange);

    }

    @Override
    public boolean deregisterShiftBreak(Id<DvrpVehicle> vehicleId) {
        return capacityManager.releaseShiftBreak(vehicleId);
    }

    @Override
    public boolean deregisterParkingOutOfShift(Id<DvrpVehicle> vehicleId) {
        return capacityManager.releaseParkingOutOfShift(vehicleId);

    }

    @Override
    public List<Id<Charger>> getChargers() {
        return chargers;
    }

    @Override
    public OperationFacilityType getType() {
        return type;
    }

    @Override
    public Set<Id<DvrpVehicle>> getShiftBreakVehicles() {
        return Collections.unmodifiableSet(capacityManager.breakReservations.keySet());
    }

    @Override
    public Set<Id<DvrpVehicle>> getParkedOutOfShiftVehicles() {
        return Collections.unmodifiableSet(capacityManager.parkReservations.keySet());
    }


    private static class CapacityManager {
        private final int capacity;
        private final int horizon;         // total number of seconds we support

        // counts[t] = how many active reservations cover second t
        private final int[] counts;
        // fullSeconds[t] = true iff counts[t] >= capacity
        private final BitSet fullSeconds;

        private final Map<Id<DvrpVehicle>, IntRange> breakReservations = new HashMap<>();
        private final Map<Id<DvrpVehicle>, IntRange> parkReservations = new HashMap<>();

        public CapacityManager(int capacity, double maxTime) {
            if (capacity < 1) {
                throw new IllegalArgumentException("capacity must be at least 1");
            }
            this.capacity = capacity;
            this.horizon = (int) maxTime;
            this.counts = new int[horizon];
            this.fullSeconds = new BitSet(horizon);
        }

        private boolean hasCapacity(IntRange timeWindow) {
            int from = Math.max(0, timeWindow.getMinimumInteger());
            int to = Math.min(horizon - 1, timeWindow.getMaximumInteger());
            int firstFull = fullSeconds.nextSetBit(from);
            return firstFull < 0 || firstFull > to;
        }

        private boolean register(IntRange timeRange) {

            if (!hasCapacity(timeRange)) {
                return false;
            }
            int from = Math.max(0, timeRange.getMinimumInteger());
            int to = Math.min(horizon - 1, timeRange.getMaximumInteger());

            // update counts & fullSeconds in one pass
            for (int t = from; t <= to; t++) {
                int c = ++counts[t];
                if (c == capacity) {
                    // we hit the limit. mark this bit as “full”
                    fullSeconds.set(t);
                }
            }
            return true;
        }

        private boolean release(IntRange timeRange) {
            if (timeRange == null) {
                return false;
            }
            int from = Math.max(0, timeRange.getMinimumInteger());
            int to = Math.min(horizon - 1, timeRange.getMaximumInteger());
            for (int t = from; t <= to; t++) {
                int c = --counts[t];
                if (c < capacity) {
                    fullSeconds.clear(t);
                }
            }
            return true;
        }

        private boolean releaseShiftBreak(Id<DvrpVehicle> vehicleId) {
            IntRange timeRange = breakReservations.remove(vehicleId);
            return release(timeRange);
        }

        private boolean releaseParkingOutOfShift(Id<DvrpVehicle> vehicleId) {
            IntRange timeRange = parkReservations.remove(vehicleId);
            return release(timeRange);
        }

        public boolean registerShiftBreak(Id<DvrpVehicle> vehicleId, IntRange timeRange) {
            boolean update = breakReservations.containsKey(vehicleId);
            if(update) {
                releaseShiftBreak(vehicleId);
            }
            boolean registered = register(timeRange);
            if(registered) {
                breakReservations.put(vehicleId, timeRange);
            }
            return registered;
        }

        public boolean registerParkingOutOfShift(Id<DvrpVehicle> vehicleId, IntRange timeRange) {
            boolean update = parkReservations.containsKey(vehicleId);
            if(update) {
                releaseParkingOutOfShift(vehicleId);
            }
            boolean registered = register(timeRange);
            if(registered) {
                parkReservations.put(vehicleId, timeRange);
            }
            return registered;
        }
    }
}
