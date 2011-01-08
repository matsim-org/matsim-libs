/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.mrieser.core.mobsim.features.fastQueueNetworkFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;

/**
 * Some notes about parallelization:<br />
 * Link- and Node-Activiation is handled by {@link Operator}s. Each thread has its own
 * operator, and each link and node gets an Operator assigned. The operator for nodes
 * are distributed in a round-robin way. Links are assigned to the same operator as their
 * from-node is assigned to. The cyclic barrier used to coordinate the threads takes
 * responsibility for keeping memory consistency across the threads.
 *
 * @author mrieser
 */
/*package*/ class ParallelOperator implements Operator {

	private final static Logger log = Logger.getLogger(ParallelOperator.class);

	private final int nOfThreads;
	private final Thread[] threads;
	private final SlaveOperator[] slaves;
	private final CyclicBarrier startNodesBarrier;
	private final CyclicBarrier startLinksBarrier;
	private final CyclicBarrier finishedBarrier;
	private QueueNetwork queueNetwork = null;
	private SlaveOperator master = null;

	public ParallelOperator(final int nOfThreads) {
		this.nOfThreads = Math.max(0, nOfThreads - 1); // the main thread acts also as a thread, thus -1
		this.threads = new Thread[this.nOfThreads];
		this.slaves = new SlaveOperator[this.nOfThreads];
		this.startNodesBarrier = new CyclicBarrier(this.nOfThreads + 1);
		this.startLinksBarrier = new CyclicBarrier(this.nOfThreads + 1);
		this.finishedBarrier = new CyclicBarrier(this.nOfThreads + 1);
	}

	/*package*/ void setQueueNetwork(final QueueNetwork network) {
		this.queueNetwork = network;
	}

	@Override
	public void activateNode(QueueNode node) {
		throw new RuntimeException("This method should never be called in this implementation.");
	}

	@Override
	public void activateLink(QueueLink link) {
		throw new RuntimeException("This method should never be called in this implementation.");
	}

	@Override
	public void beforeMobSim() {
		this.master = new SlaveOperator(this.startNodesBarrier, this.startLinksBarrier, this.finishedBarrier);
		for (int i = 0; i < this.nOfThreads; i++) {
			this.slaves[i] = new SlaveOperator(this.startNodesBarrier, this.startLinksBarrier, this.finishedBarrier);
		}
		int threadId = 0;
		for (QueueNode node : this.queueNetwork.getNodes().values()) {
			SlaveOperator slave = (threadId == this.slaves.length) ? this.master : this.slaves[threadId];
			node.setOperator(slave);
			for (Link link : node.node.getOutLinks().values()) {
				QueueLink qLink = this.queueNetwork.getLinks().get(link.getId());
				qLink.setOperator(slave);
			}
			threadId++;
			if (threadId > this.slaves.length) {
				threadId = 0;
			}
		}
		for (int i = 0; i < this.nOfThreads; i++) {
			this.threads[i] = new Thread(this.slaves[i], "ParallelFastQueueSim-" + i);
			this.threads[i].start();
		}
	}

	@Override
	public void doSimStep(double time) {
		this.master.setTime(time);
		for (int i = 0; i < this.nOfThreads; i++) {
			this.slaves[i].setTime(time);
		}
		try {
			this.master.handleTimeStep();
//			this.startNodesBarrier.await();
//			this.startLinksBarrier.await();
//			this.finishedBarrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterMobSim() {
		for (int i = 0; i < this.nOfThreads; i++) {
			this.slaves[i].afterMobSim();
		}
		try {
			// release the other threads
			this.startNodesBarrier.await();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (BrokenBarrierException e) {
			throw new RuntimeException(e);
		}
	}

	private static class SlaveOperator implements Operator, Runnable {

		private final LinkedList<QueueLink> activeLinks = new LinkedList<QueueLink>();
		private final LinkedList<QueueNode> activeNodes = new LinkedList<QueueNode>();
		private final List<QueueLink> linksToActivate = new ArrayList<QueueLink>(100);
		private final Collection<QueueNode> nodesToActivate = new ConcurrentLinkedQueue<QueueNode>();

		private final CyclicBarrier startNodesBarrier;
		private final CyclicBarrier startLinksBarrier;
		private final CyclicBarrier finishedBarrier;

		private volatile boolean finished = false;
		private volatile double time = -1;

		public SlaveOperator(final CyclicBarrier startNodesBarrier, final CyclicBarrier startLinksBarrier, final CyclicBarrier finishedBarrier) {
			this.startNodesBarrier = startNodesBarrier;
			this.startLinksBarrier = startLinksBarrier;
			this.finishedBarrier = finishedBarrier;
		}

		@Override
		public final void activateNode(QueueNode node) {
			this.nodesToActivate.add(node);
		}

		@Override
		public final void activateLink(QueueLink link) {
			this.linksToActivate.add(link);
		}

		@Override
		public void beforeMobSim() {
		}

		@Override
		public final void doSimStep(double time) {
			throw new RuntimeException("This method should never be called in this implementation.");
		}

		@Override
		public void afterMobSim() {
			this.finished = true;
		}

		public final void setTime(final double time) {
			this.time = time;
		}

		@Override
		public void run() {
			try {
				boolean finished = false;
				while (!finished) {
					finished = handleTimeStep();
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} catch (BrokenBarrierException e) {
				throw new RuntimeException(e);
			}
			finally {
				Gbl.printCurrentThreadCpuTime();
			}
		}

		private boolean handleTimeStep() throws InterruptedException, BrokenBarrierException {
			this.startNodesBarrier.await();
			if (this.finished) {
				return true;
			}
			moveNodes();
			this.startLinksBarrier.await();
			moveLinks();
			this.finishedBarrier.await();
			return false;
		}

		private void moveNodes() {
			this.activeNodes.addAll(this.nodesToActivate);
			this.nodesToActivate.clear();
			if (time % 3600 == 0 && log.isDebugEnabled()) {
				log.debug(Thread.currentThread().getName() + " # active nodes = " + this.activeNodes.size());
			}
			ListIterator<QueueNode> simNodes = this.activeNodes.listIterator();
			while (simNodes.hasNext()) {
				QueueNode node = simNodes.next();
				node.moveNode(time);
				if (!node.isActive()) {
					simNodes.remove();
				}
			}
		}

		private void moveLinks() {
			this.activeLinks.addAll(this.linksToActivate);
			this.linksToActivate.clear();
			if (time % 3600 == 0 && log.isDebugEnabled()) {
				log.debug(Thread.currentThread().getName() + " # active links = " + this.activeLinks.size());
			}
			ListIterator<QueueLink> simLinks = this.activeLinks.listIterator();
			while (simLinks.hasNext()) {
				QueueLink link = simLinks.next();
				synchronized (link) {
					link.doSimStep(time);
				}
				if (!link.isActive()) {
					simLinks.remove();
				}
			}
		}
	}

}
