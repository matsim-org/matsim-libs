package playground.pieter.network.clustering;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;

public abstract class NodeClusteringAlgorithm {
	private String algorithmName;
	private LinkedHashMap<Id, ClusterLink> links;
	private LinkedHashMap<Id, ClusterNode> nodes;
	private TreeMap<Integer, NodeCluster> leafNodeClusters;
	// private TreeMap<Integer, NodeCluster> currentNodeClusters;
	private TreeMap<Integer, NodeCluster> rootClusters;
	protected Method internalFlowMethod;
	protected Object[] internalFlowMethodParameters;
	private ArrayList<Double> flowValues;
	protected int clusterSteps = 0;
	int outBoundClusterSize = 0;
	Logger logger;
	private Network network;
	
	public LinkedHashMap<Id, ClusterLink> getLinks() {
		return links;
	}
	
	public NodeClusteringAlgorithm(String algorithmName,Network network, String linkMethodName,
			String[] argTypes, Object[] args) {
		this.internalFlowMethod = getLinkGetMethodWithArgTypes(linkMethodName,
				argTypes);
		this.internalFlowMethodParameters = args;
		if (argTypes != null || args != null)
			logger.info("Using args " + internalFlowMethodParameters.toString());
		logger = Logger.getLogger("NodeClusterer");
		this.network = network;
		links = new LinkedHashMap<Id, ClusterLink>(network.getLinks().size());
		for (Link l : network.getLinks().values()) {
			links.put(l.getId(), new ClusterLink((LinkImpl) l));
		}
		nodes = new LinkedHashMap<Id, ClusterNode>(network.getNodes().size());
		leafNodeClusters = new TreeMap<Integer, NodeCluster>();
		int i = 0;
		for (Node n : network.getNodes().values()) {
			nodes.put(n.getId(), new ClusterNode((NodeImpl) n));
			leafNodeClusters.put(i, new NodeCluster(nodes.get(n.getId()), this,
					0, i, internalFlowMethod, internalFlowMethodParameters));
			i++;
		}
		this.algorithmName = algorithmName;
	}
	
	
	/**
	 * traverses the root cluster(s) from the top and adds them to an arraylist
	 * of clusters. needs to use arraylist because the clusters have ids based
	 * on which of their children has the largest flow, so will produce weird
	 * behaviour in a map when
	 * 
	 * @param level
	 * @return an arraylist of clusters
	 */
	public ArrayList<NodeCluster> getClustersAtLevel(int level) {
		if (rootClusters == null) {
			return null;
		}
		ArrayList<NodeCluster> outClusters = null;
		outClusters = new ArrayList<NodeCluster>();
		ArrayList<NodeCluster> tempClusters = new ArrayList<NodeCluster>();
		tempClusters.addAll(rootClusters.values());
		boolean levelFound = false;
		int currentLevel = 0;
		// first, find the highest nodecluster in the set
		for (NodeCluster nc : tempClusters) {
			if (nc.getClusterStepFormed() > currentLevel)
				currentLevel = nc.getClusterStepFormed();
		}
		while (!levelFound) {
			if (currentLevel <= level) {
				levelFound = true;
				outClusters.addAll(tempClusters);
			} else {
				ArrayList<NodeCluster> tempClusters2 = new ArrayList<NodeCluster>();
				tempClusters2.addAll(tempClusters);
				tempClusters = new ArrayList<NodeCluster>();
				for (NodeCluster nc : tempClusters2) {
//					if (nc.getClusterStepParented()==maxLevel-1)
//						outClusters.add(nc);
//					else 
					if (nc.getClusterStepFormed()<currentLevel || nc.isLeaf())
						tempClusters.add(nc);
					else
						tempClusters.addAll(nc.getChildren().values());

				}
				currentLevel--;
			}
		}

		return outClusters;
	}


	/**
	 * Procedure that is run after intialization to find nodeclusters with one
	 * inlink or one outlink; clusters these together with their upstream
	 * cluster (one inlink) or downstream cluster (one outlink)
	 */
	private TreeMap<Integer, NodeCluster> findLoopsAndLongLinks(
			TreeMap<Integer, NodeCluster> clusters) {
		logger.info("Finding loops and long links: START");
		boolean doneClustering = false;
		while (!doneClustering) {
			// TreeMap<Integer, NodeCluster> currentNCs = nodeClusterHistory
			// .pollLastEntry().getValue();
			// get a single new NodeCluster for this step
			NodeCluster newCluster = findSingleInLinkClusters(clusters);
			if (newCluster == null)
				break;
			else
				clusterSteps++;
			newCluster.freezeCluster();
			clusters = updateClusterTree(clusters, newCluster);
			updateLinksAndNodes(newCluster);
			logger.info(String
					.format("Step %05d of %05d: c1: %05d + c2: %05d = %05d, flow = %08.2f, deltaFlow = %08.2f",
							clusterSteps, this.leafNodeClusters.size(),
							newCluster.getChild1().getId(), newCluster
									.getChild2().getId(), newCluster.getId(),
							newCluster.getInternalFlow(), newCluster
									.getDeltaFlow()));
		}
		logger.info("Finding loops and long links: DONE");
		return clusters;
	}
	protected abstract NodeCluster findSingleInLinkClusters(TreeMap<Integer, NodeCluster> clusters); 

	public void run() {
		logger.info("Starting clustering algo, using the link method "
				+ internalFlowMethod.toString());
		clusterSteps = 1;

		// the node clusters are there in case we need to re-initialize
		this.flowValues = new ArrayList<Double>();
		TreeMap<Integer, NodeCluster> currentNodeClusters = new TreeMap<Integer, NodeCluster>();
		currentNodeClusters = findLoopsAndLongLinks(leafNodeClusters);
		boolean doneClustering = false;
		NodeCluster newCluster = null;
		while (!doneClustering) {
			// get a single new NodeCluster for this step
			newCluster = findNextCluster(currentNodeClusters,
					clusterSteps);
			if (newCluster == null) {
				logger.error("Procedure ended with more than one cluster.");
				break;
			}
			newCluster.freezeCluster();
			currentNodeClusters = updateClusterTree(currentNodeClusters,
					newCluster);
			updateLinksAndNodes(newCluster);
			// this.nodeClusterHistory.put(clusterStep, newNCs);
			flowValues.add(newCluster.getInternalFlow());
			logger.info(String
					.format("Step %05d of %05d: %05d + %05d = %05d, f: %08.2f, dF: %08.2f, invoc: %12d, obc: %12d",
							clusterSteps, this.leafNodeClusters.size(),
							newCluster.getChild1().getId(), newCluster
									.getChild2().getId(), newCluster.getId(),
							newCluster.getInternalFlow(), newCluster
									.getDeltaFlow(), NodeCluster
									.getInvocations(), outBoundClusterSize));
			if (currentNodeClusters.size() == 1)
				doneClustering = true;
			else
				clusterSteps++;
		}
		if (currentNodeClusters.size() > 1) {
			currentNodeClusters = findLoopsAndLongLinks(currentNodeClusters);
		}
		logger.info(String.format("number of clusters: %d",
				currentNodeClusters.size()));
		rootClusters = currentNodeClusters;
		logger.info("DONE");
	}

	protected abstract NodeCluster findNextCluster(
			TreeMap<Integer, NodeCluster> currentNodeClusters, int clusterSteps2);

	private void updateLinksAndNodes(NodeCluster newCluster) {
		for (ClusterLink l : newCluster.getInLinks().values()) {
			l.setNewRoot(newCluster, false);
			l.setToCluster(newCluster);
		}
		for (ClusterLink l : newCluster.getOutLinks().values()) {
			l.setNewRoot(newCluster, false);
			l.setFromCluster(newCluster);
		}
		for (ClusterLink l : newCluster.getInterLinks().values()) {
			l.setNewRoot(newCluster, true);
		}
		for (ClusterNode n : newCluster.getNodes().values()) {
			n.setNewRoot(newCluster);
		}
		newCluster.getChild1().setParent(newCluster);
		newCluster.getChild2().setParent(newCluster);

	}
	

	private double sumList(ArrayList<Double> flowValues2) {
		double result = 0;
		for (double d : flowValues2) {
			result += d;
		}
		return result;
	}

	private TreeMap<Integer, NodeCluster> updateClusterTree(
			TreeMap<Integer, NodeCluster> currentNCs, NodeCluster newCluster) {
		TreeMap<Integer, NodeCluster> updatedNCs = new TreeMap<Integer, NodeCluster>();
		int myId = newCluster.getChild1().getId();
		if (newCluster.getChild2().getInternalFlow() > newCluster.getChild1()
				.getInternalFlow()) {
			myId = newCluster.getChild2().getId();
		}

		// updatedNCs.putAll(currentNCs);
		currentNCs.remove(newCluster.getChild1().getId());
		currentNCs.remove(newCluster.getChild2().getId());
		newCluster.setId(myId);
		currentNCs.put(myId, newCluster);
		// System.out.println(currentNCs.size());
		return currentNCs;
	}
	

	private boolean hasSharedLinks(NodeCluster nc1, NodeCluster nc2) {
		for (ClusterLink l : nc1.getInLinks().values()) {
			if (nc2.getOutLinks().get(l.getId()) != null) {
				return true;
			}
		}
		for (ClusterLink l : nc1.getOutLinks().values()) {
			if (nc2.getInLinks().get(l.getId()) != null) {
				return true;
			}
		}
		return false;
	}
	
	public static Method getLinkGetMethodWithArgTypes(String methodName,
			String[] argTypesAsString) {
		Method linkMethod = null;
		try {
			Class<?> c = ClusterLink.class;
			// don't proceed if the caller tries anything except call a 'get'
			// method
			if (!methodName.startsWith("get"))
				throw new IllegalArgumentException(
						"Link method has to start with get!");
			Class[] argTypes = null;
			if (argTypesAsString != null) {
				argTypes = new Class[argTypesAsString.length];
				// iterate through the argument types, construct the class array
				for (int i = 0; i < argTypesAsString.length; i++) {
					argTypes[i] = Class.forName(argTypesAsString[i]);
				}
			}
			linkMethod = c.getDeclaredMethod(methodName, argTypes);
			if (!linkMethod.getReturnType().toString().toLowerCase()
					.equals("double"))
				throw new IllegalArgumentException(
						"Link method doesn't return a double!");

			// production code should handle these exceptions more gracefully
		} catch (ClassNotFoundException x) {
			x.printStackTrace();
		} catch (NoSuchMethodException x) {
			x.printStackTrace();
		} catch (IllegalArgumentException x) {
			x.printStackTrace();
		}

		return linkMethod;

	}
	
	protected HashSet<ClusterCombo> getViableClusterCombos(
			TreeMap<Integer, NodeCluster> nodeClusters) {
		HashSet<ClusterCombo> clusterCheck = new HashSet<ClusterCombo>();
		for (NodeCluster c1 : nodeClusters.values()) {
			for (NodeCluster c2 : c1.getOutBoundClusters()) {
				outBoundClusterSize++;
				// skip if this combination is already i the hashset
				clusterCheck.add(new ClusterCombo(c1.getId(), c2.getId()));
			}
		}
		// System.out.println("Clustercombos: " + clusterCheck.size());
		return clusterCheck;
	}
	public int getClusterSteps() {
		return clusterSteps;
	}
	
	
	
}
class ClusterCombo {
	private int id1;
	private int id2;
	private String comboId;

	public ClusterCombo(int id1, int id2) {
		super();
		this.id1 = id1;
		this.id2 = id2;
		if (id1 > id2)
			comboId = "" + id2 + "z" + id1;
		else
			comboId = "" + id1 + "z" + id2;
	}

	public int hashCode() {
		return comboId.hashCode();

	}

	public boolean equals(Object obj) {
		ClusterCombo cc = (ClusterCombo) obj;
		return getComboId().equals(cc.getComboId());

	}

	public String getComboId() {
		return comboId;
	}

}