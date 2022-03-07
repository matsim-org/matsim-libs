/* *********************************************************************** *
 * project: org.matsim.*
 * QNetsimEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.inject.Inject;

import mpi.MPI;
import mpi.MPIException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.utils.io.IOUtils;

/**
 * Coordinates the movement of vehicles on the links and the nodes.
 * Split Up the old {@code QNetsimEngineRunner} which was implementing
 * 2 different approaches parallel.
 *
 * @author droeder@Senozon after
 * 
 * @author mrieser
 * @author dgrether
 * @author dstrippgen
 */
final class QNetsimEngineWithThreadpool extends AbstractQNetsimEngine<QNetsimEngineRunnerForThreadpool> {

	private static Logger log = LogManager.getLogger(QNetsimEngineWithThreadpool.class);
	private final int numOfRunners;
	private ExecutorService pool;
	
	public QNetsimEngineWithThreadpool(final QSim sim) {
		this(sim, null);
	}

	@Inject
	public QNetsimEngineWithThreadpool(final QSim sim, QNetworkFactory netsimNetworkFactory) {
		super(sim, netsimNetworkFactory);
		this.numOfRunners = this.numOfThreads;
	}

	@Override
	public void finishMultiThreading() {
		this.pool.shutdown();
	}

	protected void run(double time) {
		// yy Acceleration options to try out (kai, jan'15):

		// (a) Try to do without barriers.  With our 
		// message-based experiments a decade ago, it was better to let each runner decide locally when to proceed.  For intuition, imagine that
		// one runner is slowest on the links, and some other runner slowest on the nodes.  With the barriers, this cannot overlap.
		// With message passing, this was achieved by waiting for all necessary messages.  Here, it could (for example) be achieved with runner-local
		// clocks:
		// for ( all runners that own incoming links to my nodes ) { // (*)
		//    wait until runner.getTime() == myTime ;
		// }
		// processLocalNodes() ;
		// mytime += 0.5 ;
		// for ( all runners that own toNodes of my links ) { // (**)
		//    wait until runner.getTime() == myTime ;
		// }
		// processLocalLinks() ;
		// myTime += 0.5 ;

		// (b) Do deliberate domain decomposition rather than round robin (fewer runners to wait for at (*) and (**)).

		// (c) One thread that is much faster than all others is much more efficient than one thread that is much slower than all others. 
		// So make sure that no thread sticks out in terms of slowness.  Difficult to achieve, though.  A decade back, we used a "typical" run
		// as input for the domain decomposition under (b).

		// set current Time
		for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
			engine.setTime(time);
		}

		try {
			for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
				((QNetsimEngineRunnerForThreadpool) engine).setMovingNodes(true);
			}
			for (Future<Boolean> future : pool.invokeAll(this.getQnetsimEngineRunner())) {
				future.get();
			}
			try {
				sendReceiveAvailableBufferSpaces();
			} catch (MPIException e) {
				throw new RuntimeException(e);
			}
			for (AbstractQNetsimEngineRunner engine : this.getQnetsimEngineRunner()) {
				((QNetsimEngineRunnerForThreadpool) engine).setMovingNodes(false);
			}
			for (Future<Boolean> future : pool.invokeAll(this.getQnetsimEngineRunner())) {
				future.get();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private void sendReceiveVehicles() throws MPIException {
		final int me = MPI.COMM_WORLD.getRank();
		;

		Map<Integer, Map<String, List<QVehicle>>> vehicleInfo = new HashMap<>();

		boolean interesting = false;

		// collect, for each neighbor, the info that we need to send there:
		for (QLinkI link : this.network.getNetsimLinks().values()) {
			int fromNodeCpn = (int) link.getLink().getFromNode().getAttributes().getAttribute(QSim.CPN_ATTRIBUTE);
			if (fromNodeCpn == me) {
				int toNodeCpn = (int) link.getLink().getToNode().getAttributes().getAttribute(QSim.CPN_ATTRIBUTE);
				if (toNodeCpn != me) {
					final Map<String, List<QVehicle>> vehicleInfoForCpn = vehicleInfo.computeIfAbsent(toNodeCpn, k -> new HashMap<>());

					final List<QVehicle> vehicleInfoForLink = vehicleInfoForCpn.computeIfAbsent(link.getLink().getId().toString(), k -> new ArrayList<>());
					// yy could be moved to (*)

					final QLaneI lane = link.getAcceptingQLane();
					Gbl.assertIf(lane instanceof QueueWithBuffer);

					// send all vehicles from buffer to other cpn:
					while (!lane.isNotOfferingVehicle()) {
						// (*) (see above)
						final QVehicle veh = lane.popFirstVehicle();
						vehicleInfoForLink.add(veh);
						interesting = true;
						/*
						 * Design thoughts at this point:
						 * () Serializing the full QVehicle and all objects inside is would be quite a challenge.
						 * () With respect to passengers, we cannot rely on their type.  Might, e.g., be reactive agents.
						 */
					}

				}
			}
		}

		// send the above consolidated info to the neighbors:
		for (Map.Entry<Integer, Map<String, List<QVehicle>>> entry : vehicleInfo.entrySet()) {
			final Integer dest = entry.getKey();
			final Map<String, List<QVehicle>> obj = entry.getValue();
			sndObjectToNeighbor(dest, obj);
			if (interesting) {
				log.info("SND veh to cpn=" + dest + ": " + obj);
			}
		}

		// receive the corresponding info from my neighbors:
		for (Integer cpn : vehicleInfo.keySet()) {
			// (these are my neighbors)

			Map<String, List<QVehicle>> obj = (Map<String, List<QVehicle>>) rcvObjectFromNeighbor(cpn);
			log.debug("RCV veh on cpn=" + me + ": " + obj);
			for (Map.Entry<String, List<QVehicle>> entry : obj.entrySet()) {
				Id<Link> linkId = Id.createLinkId(entry.getKey());
				final QLinkI qLink = this.network.getNetsimLink(linkId);
				QLaneI qLane = qLink.getAcceptingQLane();
				Gbl.assertIf(qLane instanceof QueueWithBuffer);

				for (QVehicle veh : entry.getValue()) {
					log.info("RCV veh on cpn=" + me + ":" + veh);
					qLane.addFromWait(veh);
				}
			}
		}
	}

	private void sendReceiveAvailableBufferSpaces() throws MPIException {
		final int me = MPI.COMM_WORLD.getRank();
		;

		Map<Integer, Map<String, Double>> linkInfo = new HashMap<>();

		// We cut before the lanes split up.  The lanes info is thus not necessary.  kai, aug'18

		// collect, for each neighbor, the info that we need to send there:
		for (QLinkI link : this.network.getNetsimLinks().values()) {
			int toNodeCpn = (int) link.getLink().getToNode().getAttributes().getAttribute(QSim.CPN_ATTRIBUTE);
			if (toNodeCpn == me) {
				int fromNodeCpn = (int) link.getLink().getFromNode().getAttributes().getAttribute(QSim.CPN_ATTRIBUTE);
				if (fromNodeCpn != me) {
					final Map<String, Double> linkInfoForCpn = linkInfo.computeIfAbsent(fromNodeCpn, k -> new HashMap<>());

					final QLaneI lane = link.getAcceptingQLane();
					;
					Gbl.assertIf(lane instanceof QueueWithBuffer);
					final double flowCapForThisTimeStep = ((QueueWithBuffer) lane).getRemainingBufferStorageCapacity();
					// (The convention is that the upstream queue can move vehicles into this one until
					// it becomes negative.  kai, aug'18)

					linkInfoForCpn.put(link.getLink().getId().toString(), flowCapForThisTimeStep);
					//				log.debug( "cpn=" + me + "; link=" + link.getLink().getId()  + ": SND " + flowCapForThisTimeStep ) ;
				}
			}
		}

		// send the above consolidated info to the neighbors:
		for (Map.Entry<Integer, Map<String, Double>> entry : linkInfo.entrySet()) {
			final Integer dest = entry.getKey();
			final Map<String, Double> obj = entry.getValue();
			sndObjectToNeighbor(dest, obj);
			log.info("SND to cpn=" + dest + ": " + obj);
		}

		// receive the corresponding info from my neighbors:
		for (Integer cpn : linkInfo.keySet()) {
			// (these are my neighbors)

			Map<String, Double> obj = (Map<String, Double>) rcvObjectFromNeighbor(cpn);
			log.debug("RCV on cpn=" + me + ": " + obj);
			for (Map.Entry<String, Double> entry : obj.entrySet()) {
				Id<Link> linkId = Id.createLinkId(entry.getKey());
				final QLinkI qLink = this.network.getNetsimLink(linkId);
				QLaneI qLane = qLink.getAcceptingQLane();
				Gbl.assertIf(qLane instanceof QueueWithBuffer);
				final Double value = entry.getValue();
				((QueueWithBuffer) qLane).setRemainingBufferStorageCapacity(value);
//				log.debug( "cpn=" + me + "; link=" + linkId + ": RCV " + value ) ;
			}
		}
	}

	private Object rcvObjectFromNeighbor(final Integer cpn) throws MPIException {
		// https://stackoverflow.com/questions/14200699/send-objects-with-mpj-express
		var sizeBuffer = MPI.newIntBuffer(1);
		//int[] buf_for_size = {-1};
		MPI.COMM_WORLD.iRecv(sizeBuffer, 1, MPI.INT, cpn, 16);
		log.debug("expecting message of size=" + sizeBuffer.get(0));
		// ---
		var bytes = MPI.newByteBuffer(sizeBuffer.get(0));
		//byte [] bytes = new byte[sizeBuffer.get(0)] ;
		MPI.COMM_WORLD.recv(bytes, 1, MPI.BYTE, cpn, 17);
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes.array());
			ObjectInputStream ois = new ObjectInputStream(bis);
			return ois.readObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void sndObjectToNeighbor(final Integer dest, final Object obj) throws MPIException {
		ByteArrayOutputStream baos;
		try {
			baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.flush();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		//byte[] bytes = baos.toByteArray();
		var byteArray = baos.toByteArray();
		var bytes = ByteBuffer.wrap(byteArray);
		var bufferSize = MPI.newIntBuffer(1);
		bufferSize.put(byteArray.length);
		MPI.COMM_WORLD.iSend(bufferSize, 1, MPI.INT, dest, 16);
		MPI.COMM_WORLD.iSend(bytes, 1, MPI.BYTE, dest, 17);
	}

	private static class NamedThreadFactory implements ThreadFactory {
		private int count = 0;

		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "QNetsimEngine_PooledThread_" + count++);
		}
	}

	@Override
	protected List<QNetsimEngineRunnerForThreadpool> initQSimEngineRunners() {
		List<QNetsimEngineRunnerForThreadpool> engines = new ArrayList<>();
		for (int i = 0; i < numOfRunners; i++) {
			QNetsimEngineRunnerForThreadpool engine = new QNetsimEngineRunnerForThreadpool();
			engines.add(engine);
		}
		return engines;
	}

	@Override
	protected void initMultiThreading() {
		this.pool = Executors.newFixedThreadPool(
				this.numOfThreads,
				new NamedThreadFactory());		
	}
}
