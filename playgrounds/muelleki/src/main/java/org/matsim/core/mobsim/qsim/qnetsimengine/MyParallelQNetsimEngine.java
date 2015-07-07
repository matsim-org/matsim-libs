//package org.matsim.core.mobsim.qsim.qnetsimengine;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.network.Link;
//import org.matsim.core.gbl.MatsimRandom;
//import org.matsim.core.mobsim.qsim.QSim;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MyParallelQNetsimEngine extends QNetsimEngine {
//	// yy I think we could consider moving the node-based rnd num gen also into the sequential version. kai, jun'10
//	// yy Until this is realized, the Random object is added as customizable objecte to the QNode. cdober, nov'10
//
//	final private static Logger log = Logger.getLogger(MyParallelQNetsimEngine.class);
//
//	private static final int TASKS_PER_THREAD = 20;
//
//	private final int numOfThreads;
//	private final int numOfTasks;
//
//	private MyQSimEngineRunner[] engines;
//	private ArrayList<Callable<Object>> moveNodesCallables, moveLinksCallables;
//
//	private QNode[][] parallelNodesArrays;
//	private List<List<QNode>> parallelNodesLists;
//	private List<List<QLinkInternalI>> parallelSimLinksLists;
//
//	private LinkReActivator linkReActivator;
//
//	private NodeReActivator nodeReActivator;
//
//	private final ExecutorService es;
//
//
//	MyParallelQNetsimEngine(final QSim sim) {
//		super(sim);
//		// (DepartureHander does not need to be added here since it is added in the "super" c'tor)
//
//		this.numOfThreads = this.getMobsim().getScenario().getConfig().qsim().getNumberOfThreads();
//		this.numOfTasks = this.numOfThreads * TASKS_PER_THREAD;
//
//		es = Executors.newFixedThreadPool(numOfThreads, new QSimEngineThreadFactory());
//	}
//
//	@Override
//	public void onPrepareSim() {
//		super.onPrepareSim();
//		initQSimEngineThreads();
//	}
//
//	/**
//	 * Implements one simulation step, called from simulation framework
//	 * @param time The current time in the simulation.
//	 */
//	@Override
//	public void doSimStep(final double time) {
//		run(time);
//
//		this.printSimLog(time);
//	}
//
//	/*
//	 * The Threads are waiting at the startBarrier.
//	 * We trigger them by reaching this Barrier. Now the
//	 * Threads will start moving the Nodes and Links. We wait
//	 * until all of them reach the endBarrier to move
//	 * on. We should not have any Problems with Race Conditions
//	 * because even if the Threads would be faster than this
//	 * Thread, means the reach the endBarrier before
//	 * this Method does, it should work anyway.
//	 */
//	private void run(double time) {
//		try {
//			// set current Time
//			for (MyQSimEngineRunner engine : this.engines) {
//				engine.setTime(time);
//			}
//
//			es.invokeAll(moveNodesCallables);
//
//			linkReActivator.run();
//
//			es.invokeAll(moveLinksCallables);
//
//			nodeReActivator.run();
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
//	}
//
//	@Override
//	public int getNumberOfSimulatedLinks() {
//
//		int numLinks = 0;
//
//		for (MyQSimEngineRunner engine : this.engines) {
//			numLinks = numLinks + engine.getNumberOfSimulatedLinks();
//		}
//
//		return numLinks;
//	}
//
//	@Override
//	public int getNumberOfSimulatedNodes() {
//
//		int numNodes = 0;
//
//		for (MyQSimEngineRunner engine : this.engines) {
//			numNodes = numNodes + engine.getNumberOfSimulatedNodes();
//		}
//
//		return numNodes;
//	}
//
//	private void initQSimEngineThreads() {
//		createNodesLists();
//		createLinkLists();
//
//		if (false) {
//			createNodesArray();
//			// if we use arrays, we don't need the lists anymore
//			this.parallelNodesLists = null;
//		}
//
//		this.engines = new MyQSimEngineRunner[numOfTasks] ;
//		this.moveNodesCallables = new ArrayList<Callable<Object>>(numOfTasks);
//		this.moveLinksCallables = new ArrayList<Callable<Object>>(numOfTasks);
//		this.linkReActivator = new LinkReActivator(this.engines);
//		this.nodeReActivator = new NodeReActivator(this.engines);
//
//		// setup threads
//		for (int i = 0; i < numOfTasks; i++) {
//			MyQSimEngineRunner engine = new MyQSimEngineRunner(false, false);
//
//			if (false) {
//				engine.setQNodeArray(this.parallelNodesArrays[i]);
//			} else {
//				engine.setQNodeList(this.parallelNodesLists.get(i));
//			}
//
//			engine.setLinks(this.parallelSimLinksLists.get(i));
//
//			this.engines[i] = engine;
//			this.moveNodesCallables.add(engine.getMoveNodesCallable());
//			this.moveLinksCallables.add(engine.getMoveLinksCallable());
//		}
//
//		/*
//		 *  Assign every Link and Node to an Activator. By doing so, the
//		 *  activateNode(...) and activateLink(...) methods in this class
//		 *  should become obsolete.
//		 */
//		assignNetElementActivators();
//	}
//
//
//	/*
//	 * Create equal sized Nodes Lists.
//	 */
//	private void createNodesLists() {
//		parallelNodesLists = new ArrayList<List<QNode>>(numOfTasks);
//		for (int i = 0; i < this.numOfTasks; i++) {
//			parallelNodesLists.add(new ArrayList<QNode>());
//		}
//
//		int roundRobin = 0;
//		for (QNode node : network.getNetsimNodes().values()) {
//			node.getCustomAttributes().put(Random.class.getName(), MatsimRandom.getLocalInstance());
//			parallelNodesLists.get(roundRobin % this.numOfTasks).add(node);
//			roundRobin++;
//		}
//	}
//
//	/*
//	 * Create Nodes Array
//	 */
//	private void createNodesArray() {
//		/*
//		 * Now we create Arrays out of our Lists because iterating over them
//		 * is much faster.
//		 */
//		this.parallelNodesArrays = new QNode[this.numOfTasks][];
//		for (int i = 0; i < parallelNodesLists.size(); i++) {
//			List<QNode> list = parallelNodesLists.get(i);
//
//			QNode[] array = new QNode[list.size()];
//			list.toArray(array);
//			this.parallelNodesArrays[i] = array;
//		}
//	}
//
//	/*
//	 * Create the Lists of QueueLinks that are handled on parallel Threads.
//	 */
//	private void createLinkLists() {
//		this.parallelSimLinksLists = new ArrayList<List<QLinkInternalI>>(numOfTasks);
//
//		for (int i = 0; i < this.numOfTasks; i++) {
//			this.parallelSimLinksLists.add(new ArrayList<QLinkInternalI>());
//		}
//	}
//
//	/*
//	 * Within the MoveThreads Links are only activated when a Vehicle is moved
//	 * over a Node which is processed by that Thread. So we can assign each QLink
//	 * to the Thread that handles its InNode.
//	 */
//	private void assignNetElementActivators() {
//		int thread = 0;
//		if (false) {
//			for (QNode[] array : parallelNodesArrays) {
//				for (QNode node : array) {
//					node.setNetElementActivator(this.engines[thread]);
//
//					// set activator for links
//					for (Link outLink : node.getNode().getOutLinks().values()) {
//						AbstractQLink qLink = (AbstractQLink) network.getNetsimLink(outLink.getId());
//						// (must be of this type to work.  kai, feb'12)
//
//						// removing qsim as "person in the middle".  not fully sure if this is the same in the parallel impl.  kai, oct'10
//						qLink.setNetElementActivator(this.engines[thread]);
//					}
//				}
//				thread++;
//			}
//		} else {
//			for (List<QNode> list : parallelNodesLists) {
//				for (QNode node : list) {
//					// set activator for nodes
//					node.setNetElementActivator(this.engines[thread]);
//					// set activator for links
//					for (Link outLink : node.getNode().getOutLinks().values()) {
//						AbstractQLink qLink = (AbstractQLink) network.getNetsimLink(outLink.getId());
//						// (must be of this type to work.  kai, feb'12)
//
//						// removing qsim as "person in the middle".  not fully sure if this is the same in the parallel impl.  kai, oct'10
//						qLink.setNetElementActivator(this.engines[thread]);
//					}
//				}
//				thread++;
//			}
//		}
//	}
//
//	/*
//	 * We do the load balancing between the Threads using some kind
//	 * of round robin.
//	 *
//	 * Additionally we should check from time to time whether the load
//	 * is really still balanced. This is not guaranteed due to the fact
//	 * that some Links get deactivated while others don't. If the number
//	 * of Links is high enough statistically the difference should not
//	 * be to significant.
//	 */
//	/*package*/ static class LinkReActivator implements Runnable {
//		private final MyQSimEngineRunner[] runners;
//
//		public LinkReActivator(MyQSimEngineRunner[] engines) {
//			this.runners = engines;
//		}
//
//		@Override
//		public void run() {
//			/*
//			 * Each Thread contains a List of Links to activate.
//			 */
//			for (MyQSimEngineRunner runner : this.runners) {
//				/*
//				 * We do not redistribute the Links - they will be processed
//				 * by the same thread during the whole simulation.
//				 */
//				runner.activateLinks();
//			}
//		}
//	}
//
//	/*package*/ static class NodeReActivator implements Runnable {
//		private final MyQSimEngineRunner[] runners;
//
//		public NodeReActivator(MyQSimEngineRunner[] engines) {
//			this.runners = engines;
//		}
//
//		@Override
//		public void run() {
//			/*
//			 * Each Thread contains a List of Links to activate.
//			 */
//			for (MyQSimEngineRunner runner : this.runners) {
//				/*
//				 * We do not redistribute the Nodes - they will be processed
//				 * by the same thread during the whole simulation.
//				 */
//				runner.activateNodes();
//			}
//		}
//	}
//}
