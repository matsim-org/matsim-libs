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
	private final LinkedHashMap<Id, ClusterLink> links;
	private LinkedHashMap<Id, ClusterNode> nodes;
	private final TreeMap<Integer, NodeCluster> leafNodeClusters;
	// private TreeMap<Integer, NodeCluster> currentNodeClusters;
	private TreeMap<Integer, NodeCluster> rootClusters;
	Method internalFlowMethod;
	String[] internalFlowMethodParameterTypes;
	Object[] internalFlowMethodParameters;
	private final ArrayList<Double> flowValues;
	int clusterSteps = 0;
	int outBoundClusterSize = 0;
	Logger logger;
	private final Network network;
	private String algorithmName;
	private TreeMap<Integer, ArrayList> pointersToClusterLevels = null;
	HashSet<Integer> tabuList = new HashSet<>();
	public LinkedHashMap<Id, ClusterLink> getLinks() {
		return links;
	}

	NodeClusteringAlgorithm(String algorithmName, Network network,
                            String linkMethodName, String[] argTypes, Object[] args) {
		this.flowValues = new ArrayList<>();
		this.internalFlowMethodParameterTypes = argTypes;
		this.internalFlowMethod = getLinkGetMethodWithArgTypes(linkMethodName,
				argTypes);
		NodeCluster.linkMethod = internalFlowMethod;
		NodeCluster.args = args;
		this.internalFlowMethodParameters = args;
		if (argTypes != null || args != null)
			logger.info("Using args " + internalFlowMethodParameters.toString());
		logger = Logger.getLogger("NodeClusterer");
		this.network = network;
		links = new LinkedHashMap<>(network.getLinks().size());
		for (Link l : network.getLinks().values()) {
			links.put(l.getId(), new ClusterLink((LinkImpl) l));
		}
		setNodes(new LinkedHashMap<Id, ClusterNode>(network.getNodes().size()));
		leafNodeClusters = new TreeMap<>();
		int i = 0;
		for (Node n : network.getNodes().values()) {
			getNodes().put(n.getId(), new ClusterNode((NodeImpl) n));
			leafNodeClusters.put(i, new NodeCluster(getNodes().get(n.getId()),
					this, 0, i, internalFlowMethod,
					internalFlowMethodParameters));
			i++;
		}
		this.pointersToClusterLevels = new TreeMap<>();
		this.pointersToClusterLevels.put(0, new ArrayList<>(
				leafNodeClusters.values()));
		this.setAlgorithmName(algorithmName);
	}

	NodeClusteringAlgorithm(String algorithmName, Network network) {
		this.flowValues = new ArrayList<>();
		this.network = network;
		links = new LinkedHashMap<>(network.getLinks().size());
		for (Link l : network.getLinks().values()) {
			links.put(l.getId(), new ClusterLink((LinkImpl) l));
		}
		this.setNodes(new LinkedHashMap<Id, ClusterNode>(network.getNodes()
				.size()));
		leafNodeClusters = new TreeMap<>();
		this.setAlgorithmName(algorithmName);
		rootClusters = null;
		logger = Logger.getLogger("NodeClusterer");
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
    ArrayList<NodeCluster> getClustersAtLevel(int level) {
		// if(this.pointersToClusterLevels == null)
		// initializePointers();

		return pointersToClusterLevels.get(level);
	}

	private boolean initializePointers() {
		if (getRootClusters() == null) {
			return false;
		}
		pointersToClusterLevels = new TreeMap<>();
		ArrayList<NodeCluster> outClusters = null;
		outClusters = new ArrayList<>();
		ArrayList<NodeCluster> tempClusters = new ArrayList<>();
		tempClusters.addAll(getRootClusters().values());
		boolean levelFound = false;
		int currentLevel = 0;
		// first, find the highest nodecluster in the set
		for (NodeCluster nc : tempClusters) {
			if (nc.getClusterStepFormed() > currentLevel)
				currentLevel = nc.getClusterStepFormed();
		}
		while (!levelFound) {
			if (currentLevel == 0) {
				levelFound = true;
				outClusters.addAll(tempClusters);
			} else {
				ArrayList<NodeCluster> tempClusters2 = new ArrayList<>();
				tempClusters2.addAll(tempClusters);
				tempClusters = new ArrayList<>();
				for (NodeCluster nc : tempClusters2) {
					// if (nc.getClusterStepParented()==maxLevel-1)
					// outClusters.add(nc);
					// else
					if (nc.getClusterStepFormed() < currentLevel || nc.isLeaf())
						tempClusters.add(nc);
					else
						tempClusters.addAll(nc.getChildren().values());

				}
				currentLevel--;
			}
		}
		return true;
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
			newCluster.freezeCluster();
			clusters = updateClusterTree(clusters, newCluster);
			updateLinksAndNodes(newCluster);
			pointersToClusterLevels.put(clusterSteps,
					new ArrayList<>(clusters.values()));
			logger.info(String
					.format("Step %05d of %05d: c1: %05d + c2: %05d = %05d, flow = %08.2f, deltaFlow = %08.2f",
							clusterSteps, this.leafNodeClusters.size(),
							newCluster.getChild1().getId(), newCluster
									.getChild2().getId(), newCluster.getId(),
							newCluster.getInternalFlow(), newCluster
									.getDeltaFlow()));
			clusterSteps++;
		}
		logger.info("Finding loops and long links: DONE");
		return clusters;
	}

	protected abstract NodeCluster findSingleInLinkClusters(
			TreeMap<Integer, NodeCluster> clusters);

	void run() {
		logger.info("Starting clustering algo, using the link method "
				+ internalFlowMethod.toString());
		clusterSteps = 1;

		TreeMap<Integer, NodeCluster> currentNodeClusters = new TreeMap<>();
		currentNodeClusters = findLoopsAndLongLinks(leafNodeClusters);
		boolean doneClustering = false;
		NodeCluster newCluster = null;
		while (!doneClustering) {
			// get a single new NodeCluster for this step
			newCluster = findNextCluster(currentNodeClusters, clusterSteps);
			if (newCluster == null) {
				logger.error("Procedure ended with more than one cluster.");
				break;
			}
			newCluster.freezeCluster();
			currentNodeClusters = updateClusterTree(currentNodeClusters,
					newCluster);
			updateLinksAndNodes(newCluster);
			flowValues.add(newCluster.getInternalFlow());
			logger.info(String
					.format("Step %05d of %05d: %05d + %05d = %05d, f: %08.2f, dF: %08.2f, invoc: %12d, obc: %12d",
							clusterSteps, this.leafNodeClusters.size(),
							newCluster.getChild1().getId(), newCluster
									.getChild2().getId(), newCluster.getId(),
							newCluster.getInternalFlow(), newCluster
									.getDeltaFlow(), NodeCluster
									.getInvocations(), outBoundClusterSize));
			pointersToClusterLevels.put(clusterSteps,
					new ArrayList<>(currentNodeClusters.values()));
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
		setRootClusters(currentNodeClusters);
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
		TreeMap<Integer, NodeCluster> updatedNCs = new TreeMap<>();
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
		} catch (ClassNotFoundException | IllegalArgumentException | NoSuchMethodException x) {
			x.printStackTrace();
		}

        return linkMethod;

	}

	HashSet<ClusterCombo> getViableClusterCombos(
            TreeMap<Integer, NodeCluster> nodeClusters) {
		HashSet<ClusterCombo> clusterCheck = new HashSet<>();
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
	

	int getClusterSteps() {
		return clusterSteps;
	}

	public String getAlgorithmName() {
		return algorithmName;
	}

	private void setAlgorithmName(String algorithmName) {
		this.algorithmName = algorithmName;
	}

	public TreeMap<Integer, NodeCluster> getRootClusters() {
		return rootClusters;
	}

	void setRootClusters(TreeMap<Integer, NodeCluster> rootClusters) {
		this.rootClusters = rootClusters;
	}

	public LinkedHashMap<Id, ClusterNode> getNodes() {
		return nodes;
	}

	void setNodes(LinkedHashMap<Id, ClusterNode> nodes) {
		this.nodes = nodes;
	}

	public TreeMap<Integer, NodeCluster> getLeafNodeClusters() {
		return leafNodeClusters;
	}

	public Method getInternalFlowMethod() {
		return internalFlowMethod;
	}

	public void setInternalFlowMethod(Method internalFlowMethod) {
		this.internalFlowMethod = internalFlowMethod;
	}

	public String[] getInternalFlowMethodParameterTypes() {
		return internalFlowMethodParameterTypes;
	}

	public void setInternalFlowMethodParameterTypes(
			String[] internalFlowMethodParameterTypes) {
		this.internalFlowMethodParameterTypes = internalFlowMethodParameterTypes;
	}

	public Object[] getInternalFlowMethodParameters() {
		return internalFlowMethodParameters;
	}

	public void setInternalFlowMethodParameters(
			Object[] internalFlowMethodParameters) {
		this.internalFlowMethodParameters = internalFlowMethodParameters;
	}

	public void createArbitraryClusterTree(int clusterStep, int child1Id,
			int child2Id, int newId) {
		if (getRootClusters() == null) {
			setRootClusters(leafNodeClusters);
			pointersToClusterLevels = new TreeMap<>();
			pointersToClusterLevels.put(0, new ArrayList<>(
					leafNodeClusters.values()));
		}
		NodeCluster nc1 = rootClusters.get(child1Id);
		NodeCluster nc2 = rootClusters.get(child2Id);
		NodeCluster newCluster = new NodeCluster(nc1, nc2,
				this.getInternalFlowMethod(),
				this.getInternalFlowMethodParameters(), clusterStep, newId);
		newCluster.freezeCluster();
		flowValues.add(newCluster.getDeltaFlow());
		rootClusters = updateClusterTree(rootClusters, newCluster);
		updateLinksAndNodes(newCluster);
		clusterSteps = clusterStep;
		logger.info(String
				.format("Step %05d of %05d: c1: %05d + c2: %05d = %05d, flow = %08.2f, deltaFlow = %08.2f",
						clusterSteps, this.leafNodeClusters.size(), newCluster
								.getChild1().getId(), newCluster.getChild2()
								.getId(), newCluster.getId(), newCluster
								.getInternalFlow(), newCluster.getDeltaFlow()));
		pointersToClusterLevels.put(clusterSteps, new ArrayList<>(
				rootClusters.values()));
	}

	NodeCluster getLargestCluster(ArrayList<NodeCluster> clustersAtLevel) {
		double largest = 0;
		NodeCluster outCluster = null;
		for (NodeCluster nc : clustersAtLevel) {
			if (nc.getInternalFlow() > largest) {
				largest = nc.getInternalFlow();
				outCluster = nc;
			}
		}
		if(outCluster==null)
			outCluster=clustersAtLevel.get(0);
		return outCluster;
	}

	public TreeMap<Integer, ArrayList> getPointersToClusterLevels() {
		return pointersToClusterLevels;
	}

}

class ClusterCombo {
    private final String comboId;

	public ClusterCombo(int id1, int id2) {
		super();
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