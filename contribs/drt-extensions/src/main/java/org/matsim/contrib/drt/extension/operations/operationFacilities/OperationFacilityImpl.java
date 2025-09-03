package org.matsim.contrib.drt.extension.operations.operationFacilities;

import com.google.common.collect.ImmutableSet;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.ev.infrastructure.Charger;
import org.matsim.core.gbl.Gbl;

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
        this.capacityManager = new CapacityManager(id, capacity, qSimEndTime);
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
    public boolean hasCapacity(double startInclusive, double endExclusive) {
        return capacityManager.hasCapacity((int) startInclusive, (int) endExclusive);
    }

    @Override
    public boolean hasCapacity(double startInclusive) {
        return capacityManager.hasCapacity((int) startInclusive);
    }

    @Override
    public void checkIn(Registration facilityRegistration, double time) {
        capacityManager.checkIn(facilityRegistration);
    }

    @Override
    public void checkOut(Registration facilityRegistration, double time) {
        capacityManager.checkOut(facilityRegistration, time);
    }

    @Override
    public Optional<Registration> registerVehicle(Id<DvrpVehicle> vehicleId, double startInclusive, double endExclusive) {
        return capacityManager.registerVehicle(vehicleId, (int) startInclusive, (int) endExclusive);
    }

    @Override
    public Optional<Registration> registerVehicle(Id<DvrpVehicle> vehicleId, double startInclusive) {
        return capacityManager.registerVehicle(vehicleId, (int) startInclusive);
    }

    @Override
    public boolean deregisterVehicle(Id<Registration> registrationId) {
        return capacityManager.deregister(registrationId);
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
    public Set<Id<DvrpVehicle>> getCheckedInVehicles() {
        return ImmutableSet.copyOf(capacityManager.checkedIn);
    }

    static class CapacityManager {
        private final int capacity;
        private final int horizon;

        // counts[t] = how many active reservations cover second t (t in [0, horizon))
        private final int[] counts;
        // fullSeconds[t] = true iff counts[t] >= capacity for second t
        private final BitSet fullSeconds;

        private final Map<Id<Registration>, Registration> registrations = new HashMap<>();
        private final Id<OperationFacility> opFaId;
        private final Set<Id<DvrpVehicle>> checkedIn = new LinkedHashSet<>();

        private int counter = 0;

        public CapacityManager(Id<OperationFacility> opFaId, int capacity, double maxTime) {
            this.opFaId = opFaId;
            if (capacity < 1) throw new IllegalArgumentException("capacity must be at least 1");
            this.capacity = capacity;
            this.horizon = (int) maxTime;
            this.counts = new int[horizon];
            this.fullSeconds = new BitSet(horizon);
        }

        private static int clamp(int v, int lo, int hi) {
            return Math.max(lo, Math.min(hi, v));
        }

        /** Half-open: query capacity on [from, to). */
        private boolean hasCapacity(int from, int to) {
            from = clamp(from, 0, horizon);
            to   = clamp(to,   0, horizon);
            if (from > to) {
                throw new IllegalArgumentException("from " + from + " > to " + to);
            } else if (from == to) {
                return true;
            }
            int firstFull = fullSeconds.nextSetBit(from);
            return firstFull < 0 || firstFull >= to;
        }

        /** Half-open: query capacity on [from, horizon). */
        private boolean hasCapacity(int from) {
            return hasCapacity(from, horizon);
        }

        boolean deregister(Id<Registration> registrationId) {
            Registration reg = registrations.get(registrationId);
            if (reg == null) return false;
            // Cancel/no-show or finalize: free the entire remaining reservation [from, to)
            // release from the registration start
            return closeRegistration(reg, (int) reg.fromInclusive());
        }

        public void checkIn(Registration registration) {
            if (!registrations.containsKey(registration.registrationId())) {
                throw new IllegalStateException("checkIn on inactive registration: " + registration.registrationId());
            }
            if (checkedIn.size() >= capacity && !checkedIn.contains(registration.vehicleId())) {
                throw new IllegalStateException("Physical capacity exceeded");
            }
            checkedIn.add(registration.vehicleId());
        }

        public void checkOut(Registration registration, double time) {
            // Early/normal leave: free the tail [now, to)
            closeRegistration(registration, clamp((int) time, 0, horizon));
        }

        /** Register [start, horizon). */
        public Optional<Registration> registerVehicle(Id<DvrpVehicle> vehicleId, int startInclusive) {
            return registerVehicle(vehicleId, startInclusive, horizon);
        }

        /** Register [from, to) half-open. */
        Optional<Registration> registerVehicle(Id<DvrpVehicle> vehicleId, int from, int to) {
            from = clamp(from, 0, horizon);
            to   = clamp(to,   0, horizon);
            if (from >= to) {
                return Optional.empty();
            }

            if (!hasCapacity(from, to)) {
                return Optional.empty();
            }

            // update counts & fullSeconds in one pass for [from, to)
            for (int t = from; t < to; t++) {
                int c = ++counts[t];
                if (c == capacity) {
                    // we hit the limit at this exact second
                    fullSeconds.set(t);
                }
            }
            Registration registration = new Registration(
                    Id.create(vehicleId + "_" + counter, Registration.class),
                    opFaId,
                    vehicleId,
                    from,
                    to
            );
            counter++;
            registrations.put(registration.registrationId(), registration);
            return Optional.of(registration);
        }

        /**
         * Remove reg, free the remaining reserved window from releaseFrom..reg.to (half-open), and clear presence.
         * Idempotent: if already removed, it's a no-op (presence is still cleared).
         */
        private boolean closeRegistration(Registration reg, int releaseFrom) {
            if (!registrations.containsKey(reg.registrationId())) {
                checkedIn.remove(reg.vehicleId());
                return false;
            }

            registrations.remove(reg.registrationId());

            int from = (int) Math.max(reg.fromInclusive(), clamp(releaseFrom, 0, horizon));
            int to   = clamp((int) reg.toExclusive(), 0, horizon);

            if (from < to) {
                for (int t = from; t < to; t++) {
                    Gbl.assertIf(counts[t] > 0); // dev guard: catch double-free
                    int c = --counts[t];
                    if (c < capacity) fullSeconds.clear(t);
                }
            }

            checkedIn.remove(reg.vehicleId());
            return true;
        }
    }
}
