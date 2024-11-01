package org.matsim.dsim.simulation.net;

import lombok.Getter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.dsim.simulation.SimStepMessaging;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Queue;

import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;

public interface SimLink {

    enum LinkPosition {QStart, QEnd, Buffer}

    Id<Link> getId();

    Id<Node> getToNode();

    double getMaxFlowCapacity();

	boolean isAccepting(LinkPosition position, double now);

    boolean isOffering();

    SimVehicle peekFirstVehicle();

    SimVehicle popVehicle();

    void pushVehicle(SimVehicle vehicle, LinkPosition position, double now);

    void addLeaveHandler(OnLeaveQueue onLeaveQueue);

    boolean doSimStep(SimStepMessaging messaging, double now);

    enum OnLeaveQueueInstruction {MoveToBuffer, RemoveVehicle, BlockQueue}

    @FunctionalInterface
    interface OnLeaveQueue {
        OnLeaveQueueInstruction apply(SimVehicle vehicle, SimLink link, double now);

        default OnLeaveQueue compose(OnLeaveQueue before) {
            return (SimVehicle v, SimLink l, double n) -> {
                var result = before.apply(v, l, n);
                if (result == OnLeaveQueueInstruction.MoveToBuffer) {
                    return this.apply(v, l, n);
                }
                return result;
            };
        }

        static OnLeaveQueue defaultHandler() {
            return (_, _, _) -> OnLeaveQueueInstruction.MoveToBuffer;
        }
    }


    static SimLink create(Link link, int part) {
		var defaultQsimConfig = ConfigUtils.createConfig().qsim();
		return create(link, (_, _, _) -> OnLeaveQueueInstruction.MoveToBuffer, defaultQsimConfig, 7.5, part);
    }

	static SimLink create(Link link, OnLeaveQueue onLeaveQueue, QSimConfigGroup config, double effectiveCellSize, int part) {
        var fromPart = getPartition(link.getFromNode());
        var toPart = getPartition(link.getToNode());
		var outflowCapacity = FlowCapacity.createOutflowCapacity(link);
        if (fromPart == toPart) {
			var q = SimQueue.create(link, config, effectiveCellSize);
			return new LocalLink(link, q, outflowCapacity, onLeaveQueue);
        } else if (toPart == part) {
			var q = SimQueue.create(link, config, effectiveCellSize);
			var localLink = new LocalLink(link, q, outflowCapacity, onLeaveQueue);
            return new SplitInLink(localLink, fromPart);
        } else {
			var inflowCapacity = FlowCapacity.createInflowCapacity(link, config, effectiveCellSize);
			var storageCapacity = new SimpleStorageCapacity(link, effectiveCellSize);
			return new SplitOutLink(link, storageCapacity, inflowCapacity);
        }
    }

    private static int getPartition(Node node) {
        var attr = node.getAttributes().getAttribute(PARTITION_ATTR_KEY);
        if (attr == null) {
            throw new RuntimeException("Missing attribute " + PARTITION_ATTR_KEY + " on node: " + node.getId() + ". All nodes must have a partition attribute.");
        }

        return (int) attr;
    }

    /// Implementations of SimLink
    ///
    /// Decided to have these three in one file, as Local, SplitIn, and SplitOut are tightly coupled. This way the
    /// different implementations can access each others private fields. Other representations such as pt-links can
    /// then be implemented somewhere else, as they should (ideally) only interact with the interface methods.
    class LocalLink implements SimLink {

        @Getter
        private final Id<Link> id;
        @Getter
        private final Id<Node> toNode;

        private OnLeaveQueue onLeaveQueue;

        private final SimQueue q;
        private final SimBuffer buffer;
        private final double length;
        private final double freespeed;


		LocalLink(Link link, SimQueue q, FlowCapacity outflowCapacity, OnLeaveQueue defaultOnLeaveQueue) {
            id = link.getId();
            this.q = q;
            length = link.getLength();
            freespeed = link.getFreespeed();
			buffer = new SimBuffer(outflowCapacity);
            toNode = link.getToNode().getId();
            this.onLeaveQueue = defaultOnLeaveQueue;
        }

        @Override
		public boolean isAccepting(LinkPosition position, double now) {
            return switch (position) {
				case QStart, QEnd -> q.isAccepting(position, now);
				case Buffer -> buffer.isAvailable(now);
            };
        }

        @Override
        public boolean isOffering() {
            return buffer.peek() != null;
        }

        @Override
        public SimVehicle peekFirstVehicle() {
            var vehicle = buffer.peek();
            if (vehicle == null) {
                return q.peek();
            } else {
                return vehicle;
            }
        }

        @Override
        public double getMaxFlowCapacity() {
            return buffer.getMaxFlowCapacity();
        }

        @Override
        public SimVehicle popVehicle() {
            try {
                return buffer.pollFirst();
            } catch (NoSuchElementException e) {
                throw new RuntimeException("The simulation attempted to take a vehicle from the buffer of a link "
                        + this.id + ", but the buffer was empty. Call 'isOffering' first.");
            }
        }

        @Override
        public void pushVehicle(SimVehicle vehicle, LinkPosition position, double now) {

            // calculate the speed directly. This should be done with a link speed calculator later
            var distanceToTravel = switch (position) {
                case QStart -> length;
                case QEnd, Buffer -> 0.;
            };
            var speed = Math.min(freespeed, vehicle.getMaxV());
            var duration = distanceToTravel / speed;
            var earliestExitTime = now + duration;
            vehicle.setEarliestExitTime(earliestExitTime);

			switch (position) {
				case QStart, QEnd -> q.add(vehicle, position);
				case Buffer -> buffer.add(vehicle, now);
			}
        }

        @Override
        public boolean doSimStep(SimStepMessaging messaging, double now) {

            while (!q.isEmpty()) {

                // get a reference to the first vehicle in the queue and check whether it is time to exit.
                var headVehicle = q.peek();
                if (headVehicle.getEarliestExitTime() > now) break;

                var leaveResult = onLeaveQueue.apply(headVehicle, this, now);

                // the vehicle should keep moving through the network. Move it to the
                // buffer, so that it can be processed in the next intersection update
                if (leaveResult.equals(OnLeaveQueueInstruction.MoveToBuffer)) {
					if (!buffer.isAvailable(now)) break;
                    moveVehicle(now);
                }
                // the vehicle was removed by some handler. For example, the vehicle has arrived.
                // remove the vehicle from the queue
                else if (leaveResult.equals(OnLeaveQueueInstruction.RemoveVehicle)) {
					q.poll(now);
                }
                // if the result is block, don't do anything. We assume that the handler has set a new exit time
            }
			return !q.isEmpty();
        }

        @Override
        public void addLeaveHandler(OnLeaveQueue onLeaveQueue) {
            this.onLeaveQueue = this.onLeaveQueue.compose(onLeaveQueue);
        }

        private void moveVehicle(double now) {
			var vehicle = q.poll(now);
            buffer.add(vehicle, now);
        }
    }

    class SplitOutLink implements SimLink {
        @Getter
        private final Id<Link> id;
        @Getter
        private final int toPart;

		private final Queue<SimVehicle> q = new ArrayDeque<>();
		// deliberately use implementation and NOT the interface, since the out link mirrors the downstream storage
		// capacity. This is done best with the SimpleStorageCapacity implementation
		private final SimpleStorageCapacity storageCapacity;
		private final FlowCapacity inflowCapacity;

		SplitOutLink(Link link, SimpleStorageCapacity storageCapacity, FlowCapacity inflowCapacity) {
            id = link.getId();
            this.toPart = (int) link.getToNode().getAttributes().getAttribute(PARTITION_ATTR_KEY);
			this.storageCapacity = storageCapacity;
			this.inflowCapacity = inflowCapacity;
        }

        @Override
        public Id<Node> getToNode() {
            throw new RuntimeException("Split out links don't which node they are pointing to.");
        }

        @Override
		public boolean isAccepting(LinkPosition position, double now) {
            if (LinkPosition.QStart == position) {
				storageCapacity.update(now);
				inflowCapacity.update(now);
				return storageCapacity.isAvailable() && inflowCapacity.isAvailable();
            }
            throw new IllegalArgumentException("Split out links can only accept vehicles at the start of the link. The end of the link is managed by the other partition.");
        }

        @Override
        public boolean isOffering() {
            // split out link don't offer vehicles. However, this method is called from ActiveLinks. So don't throw an
            // exception, but return false.
            return false;
        }

        @Override
        public SimVehicle peekFirstVehicle() {
            throw new RuntimeException("SplitOutLinks cannot peek vehicles, as they only accept vehicles to be sent downstream");
        }

        @Override
        public double getMaxFlowCapacity() {
            throw new RuntimeException("SplitOutLinks should not be queried for flow capacity");
        }

        @Override
        public SimVehicle popVehicle() {
            throw new RuntimeException("Not yet implemented");
        }

        @Override
        public void pushVehicle(SimVehicle vehicle, LinkPosition position, double now) {
            if (LinkPosition.QStart != position)
                throw new IllegalArgumentException("Split out links can only push vehicles at the start of the link. The end of the link is managed by the other partition.");

            assert !q.contains(vehicle);
			storageCapacity.consume(vehicle.getPce());
			inflowCapacity.consume(vehicle.getPce());
            q.add(vehicle);
        }

        @Override
        public boolean doSimStep(SimStepMessaging messaging, double now) {
            for (var vehicle : q) {
                messaging.collectVehicle(vehicle);
            }
            q.clear();
            // mark ourselves as inactive. If the upstream node pushes vehicles onto the link
            // it'll be marked as active again.
            return false;
        }

        @Override
        public void addLeaveHandler(OnLeaveQueue onLeaveQueue) {
            throw new RuntimeException("Split out links don't handle vehicles leaving");
        }

        public void applyCapacityUpdate(double released, double consumed) {
			storageCapacity.consume(consumed);
			storageCapacity.release(released, 0);
        }
    }

    class SplitInLink implements SimLink {

        @Getter
        private final int fromPart;
        private final LocalLink localLink;

        private double consumedStorageCap = 0;

        SplitInLink(LocalLink localLink, int fromPart) {
            this.localLink = localLink;
            this.fromPart = fromPart;
        }

        @Override
        public Id<Link> getId() {
            return localLink.getId();
        }

        @Override
        public Id<Node> getToNode() {
            return localLink.getToNode();
        }

        @Override
		public boolean isAccepting(LinkPosition position, double now) {
			return localLink.isAccepting(position, now);
        }

        @Override
        public boolean isOffering() {
            return localLink.isOffering();
        }

        @Override
        public SimVehicle peekFirstVehicle() {
            return localLink.peekFirstVehicle();
        }

        @Override
        public double getMaxFlowCapacity() {
            return localLink.getMaxFlowCapacity();
        }

        @Override
        public SimVehicle popVehicle() {
			return localLink.popVehicle();
        }

        @Override
        public void pushVehicle(SimVehicle vehicle, LinkPosition position, double now) {

            localLink.pushVehicle(vehicle, position, now);

            // keep track of consumed storage capacity, when vehicles are added at this end of the split link
            if (LinkPosition.QEnd == position) {
                consumedStorageCap += vehicle.getPce();
            }
        }

        @Override
        public boolean doSimStep(SimStepMessaging messaging, double now) {

            // report capacity changes to the upstream partition
			// during doSimStep, vehicles may leave the network, and if we operate in kinematicWaves mode
			// backwardstravelling holes may reach the upstream end of a link, so that capacity becomes
			// available.
			var occupiedBeforeSimStep = localLink.q.getOccupied();
			var localLinkActive = localLink.doSimStep(messaging, now);
			var storageReleased = localLink.q.isAccepting(LinkPosition.QStart, now);
			var occupiedAfterSimStep = localLink.q.getOccupied();
			var diffOccupied = occupiedBeforeSimStep - occupiedAfterSimStep;

			if (diffOccupied > 0 || consumedStorageCap > 0) {
				messaging.collectStorageCapacityUpdate(getId(), diffOccupied, consumedStorageCap, fromPart);
                consumedStorageCap = 0;
            }
			return localLinkActive || !storageReleased;
        }

        @Override
        public void addLeaveHandler(OnLeaveQueue onLeaveQueue) {
            localLink.addLeaveHandler(onLeaveQueue);
        }
    }
}
