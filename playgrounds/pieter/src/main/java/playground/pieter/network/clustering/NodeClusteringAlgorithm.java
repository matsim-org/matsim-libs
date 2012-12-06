package playground.pieter.network.clustering;

import java.io.File;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.math.util.MathUtils;
import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.ActivityFacilityImpl;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.facilities.OpeningTime;
import org.matsim.core.facilities.OpeningTime.DayType;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import others.sergioo.util.dataBase.DataBaseAdmin;
import others.sergioo.util.dataBase.NoConnectionException;

public class NodeClusteringAlgorithm {

	private Scenario scenario;
	private Network network;
	private LinkedHashMap<Id, ClusterLink> links;
	private LinkedHashMap<Id, ClusterNode> nodes;
	// the treemap gets filled with all the NodeClusters at that point as the
	// algorithm proceeds,
	// and they are numbered using the stepnumber
	// private TreeMap<Integer, TreeMap<Integer, NodeCluster>>
	// nodeClusterHistory;
	private TreeMap<Integer, NodeCluster> leafNodeClusters;
	// private TreeMap<Integer, NodeCluster> currentNodeClusters;
	private NodeCluster rootCluster;
	private Method internalFlowMethod;
	private Object[] internalFlowMethodParameters;
	private ArrayList<Double> flowValues;
	private int clusterSteps = 0;
	int outBoundClusterSize = 0;
	Logger logger;

	public LinkedHashMap<Id, ClusterLink> getLinks() {
		return links;
	}

	public NodeClusteringAlgorithm(Scenario scenario, String linkMethodName,
			String[] argTypes, Object[] args) {
		this.internalFlowMethod = getLinkGetMethodWithArgTypes(linkMethodName,
				argTypes);
		this.internalFlowMethodParameters = args;
		if (argTypes != null || args != null)
			logger.info("Using args " + internalFlowMethodParameters.toString());
		logger = Logger.getLogger("NodeClusterer");
		this.scenario = scenario;
		this.network = scenario.getNetwork();
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

	}

	/**
	 * Procedure that is run after intialization to find nodeclusters with one
	 * inlink or one outlink; clusters these together with their upstream
	 * cluster (one inlink) or downstream cluster (one outlink)
	 */
	private TreeMap<Integer, NodeCluster> findLoopsAndLongLinksIntraMAX(
			TreeMap<Integer, NodeCluster> clusters) {
		logger.info("Finding loops and long links: START");
		boolean doneClustering = false;
		while (!doneClustering) {
			// TreeMap<Integer, NodeCluster> currentNCs = nodeClusterHistory
			// .pollLastEntry().getValue();
			// get a single new NodeCluster for this step
			NodeCluster newCluster = findSingleInLinkClustersIntraMAX(clusters);
			if (newCluster == null)
				break;
			else
				clusterSteps++;
			newCluster.removeInterLinksFromOtherLinkMaps();
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

	/**
	 * simple procedure that finds the first cluster with a single inlink or
	 * outlink, and forms a new cluster with the link's upstream or downstream
	 * cluster
	 * 
	 * @param currentNCs
	 * @return
	 */
	private NodeCluster findSingleInLinkClustersIntraMAX(
			TreeMap<Integer, NodeCluster> currentNCs) {
		double maxFlow = 0;
		NodeCluster outCluster = null;
		for (int i : currentNCs.keySet()) {
			NodeCluster nc = currentNCs.get(i);
			if (nc.getInLinks().size() == 1) {
				for (ClusterLink cl : nc.getInLinks().values()) {
					NodeCluster newCluster;
					newCluster = new NodeCluster(cl.getFromCluster(), nc,
							internalFlowMethod, internalFlowMethodParameters,
							0, cl.getFromCluster().getId());
					if (newCluster.getDeltaFlow() > maxFlow) {
						maxFlow = newCluster.getDeltaFlow();
						outCluster = newCluster;
					}
				}
			}
		}
		return outCluster;
	}

	public void runIntraMAX() {
		clusterSteps = 0;

		// the node clusters are there in case we need to re-initialize
		this.flowValues = new ArrayList<Double>();
		// using treemaps because order is important when evaluating this
		// structure later
		// TreeMap<Integer, NodeCluster> leaves = new TreeMap<Integer,
		// NodeCluster>();
		// long l = 0;
		// for (NodeCluster nc : this.leafNodeClusters.values()) {
		//
		// }
		// nodeClusterHistory.put(0l, leafNodeClusters);
		TreeMap<Integer, NodeCluster> currentNodeClusters = new TreeMap<Integer, NodeCluster>();
		// currentNodeClusters.putAll(leafNodeClusters);
		currentNodeClusters = findLoopsAndLongLinksIntraMAX(leafNodeClusters);
		boolean doneClustering = false;
		logger.info("Starting clustering algo, using the link method "
				+ internalFlowMethod.toString());
		NodeCluster newCluster = null;
		while (!doneClustering) {
			// get a single new NodeCluster for this step
			newCluster = findNextIntraMAXClusterV3(currentNodeClusters,
					clusterSteps);
			if (newCluster == null)
				break;
			newCluster.removeInterLinksFromOtherLinkMaps();
			currentNodeClusters = updateClusterTree(currentNodeClusters,
					newCluster);
			updateLinksAndNodes(newCluster);
			// this.nodeClusterHistory.put(clusterStep, newNCs);
			flowValues.add(newCluster.getInternalFlow());
			if (currentNodeClusters.size() == 1)
				doneClustering = true;
			else
				clusterSteps++;
			// logger.info("Found a cluster with value "
			// + flowValues.get(flowValues.size() - 1));
			// logger.info("Total value is " + sumList(this.flowValues));
			logger.info(String
					.format("Step %05d of %05d: %05d + %05d = %05d, f: %08.2f, dF: %08.2f, invoc: %12d, obc: %12d",
							clusterSteps, this.leafNodeClusters.size(),
							newCluster.getChild1().getId(), newCluster
									.getChild2().getId(), newCluster.getId(),
							newCluster.getInternalFlow(), newCluster
									.getDeltaFlow(), NodeCluster
									.getInvocations(), outBoundClusterSize));
		}
		rootCluster = newCluster;

	}

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

	private void writeClustersToSQL(String tableName, DataBaseAdmin dba)
			throws SQLException, NoConnectionException {
		// dba.executeStatement(String.format("DROP TABLE IF EXISTS %s cascade;",
		// tableName));
		// dba.executeStatement(String.format("CREATE TABLE %s("
		// + "linkid VARCHAR(45)," + "clusterid int," + "clusterstep int,"
		// + "flow REAL" +
		//
		// ")", tableName));
		//
		// System.out.println("Filling the table");
		// int modfactor = 1;
		// int counter = 0;
		// int lineCounter = 0;
		// int batchSize = 1000;
		// StringBuilder sb = new StringBuilder();
		// CopyManager cpManager = ((PGConnection) dba.getConnection())
		// .getCopyAPI();
		// PushbackReader reader = new PushbackReader(new StringReader(""),
		// 100000000);
		//
		// for (ClusterLink l : this.links.values()) {
		// if (l.isInterLink())
		// l.isInterLink();
		// for (NodeCluster nc : l.getParentClusterArray()) {
		//
		// String sqlInserter = "\"%s\",%d,%d,%f\n";
		// sb.append(String.format(sqlInserter, l.getId(), nc.getId(),
		// nc.getClusterStep(), nc.getInternalFlow()));
		// lineCounter++;
		// }
		// if (lineCounter % batchSize == 0) {
		// try {
		// reader.unread(sb.toString().toCharArray());
		// cpManager.copyIn("COPY " + tableName
		// + " FROM STDIN WITH CSV", reader);
		// sb.delete(0, sb.length());
		// } catch (IOException e) {
		//
		// e.printStackTrace();
		// }
		// }
		// try {
		// reader.unread(sb.toString().toCharArray());
		// cpManager.copyIn("COPY " + tableName + " FROM STDIN WITH CSV",
		// reader);
		// sb.delete(0, sb.length());
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// counter++;
		// if (counter >= modfactor && counter % modfactor == 0) {
		// System.out.println("Processed STEP no " + counter);
		// modfactor = counter;
		// }
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

	/**
	 * iterates through the treemap of current node clusters, and finds the two
	 * unique permutations that optimize the objective function for this step
	 * 
	 * @param currentNCs
	 * @param clusterStep
	 * @return a nodecluster with its two child clusters
	 */
	private NodeCluster findNextIntraMAXCluster(
			TreeMap<Integer, NodeCluster> currentNCs, int clusterStep) {

		NodeCluster bestCluster = null;
		double bestValue = 0;
		// want to create new clusters that are identified with a key greater
		// than the last;
		// this has the added advantage that we know how many evaluations we've
		// done, by looking at the last one
		int counter = currentNCs.lastKey() + 1;
		// logger.info("checking " + MathUtils.factorial(currentNCs.size()));
		// the treemap's keyset runs in ascending order
		for (int i : currentNCs.keySet()) {
			for (int j : currentNCs.keySet()) {
				if (j <= i)
					continue;
				if (!hasSharedLinks(currentNCs.get(i), currentNCs.get(j)))
					continue;
				NodeCluster nc = new NodeCluster(currentNCs.get(i),
						currentNCs.get(j), this.internalFlowMethod,
						internalFlowMethodParameters, clusterStep, 0);
				double flowChange = nc.getInternalFlow()
						- nc.getChild1().getInternalFlow()
						- nc.getChild2().getInternalFlow();
				// logger.info("c1: "+nc.getChild1().getId()+" c2: "+nc.getChild2().getId()+" Flowchange: "+
				// flowChange);
				// logger.info("c1: "+nc.getChild1().getInternalFlow()+" c2: "+nc.getChild2().getInternalFlow()+" nc: "+
				// nc.getInternalFlow());
				if (flowChange > bestValue) {
					bestValue = flowChange;
					bestCluster = nc;
				}
				// counter++;
				// if (counter % 1000 == 0)
				// logger.info("Checked " + counter);
			}
		}
		// logger.info("Found the best cluster, with value " + bestValue);

		return bestCluster;

	}

	private NodeCluster findNextIntraMAXClusterV3(
			TreeMap<Integer, NodeCluster> currentNCs, int clusterStep) {

		NodeCluster bestCluster = null;
		double bestValue = 0;
		// want to create new clusters that are identified with a key greater
		// than the last;
		// this has the added advantage that we know how many evaluations we've
		// done, by looking at the last one
		int counter = currentNCs.lastKey() + 1;
		// logger.info("checking " + MathUtils.factorial(currentNCs.size()));
		// the treemap's keyset runs in ascending order
		HashSet<ClusterCombo> viableClusterCombos = getViableClusterCombos(currentNCs);
		for (ClusterCombo cc : viableClusterCombos) {
			String[] ccs = cc.getComboId().split("z");

			int i = Integer.parseInt(ccs[0]);
			int j = Integer.parseInt(ccs[1]);
			NodeCluster nc = new NodeCluster(currentNCs.get(i),
					currentNCs.get(j), this.internalFlowMethod,
					internalFlowMethodParameters, clusterStep, 0);
			double flowChange = nc.getInternalFlow()
					- nc.getChild1().getInternalFlow()
					- nc.getChild2().getInternalFlow();
			// logger.info("c1: "+nc.getChild1().getId()+" c2: "+nc.getChild2().getId()+" Flowchange: "+
			// flowChange);
			// logger.info("c1: "+nc.getChild1().getInternalFlow()+" c2: "+nc.getChild2().getInternalFlow()+" nc: "+
			// nc.getInternalFlow());
			if (flowChange > bestValue) {
				bestValue = flowChange;
				bestCluster = nc;
			}
			// counter++;
			// if (counter % 1000 == 0)
			// logger.info("Checked " + counter);

		}
		// logger.info("Found the best cluster, with value " + bestValue);

		return bestCluster;

	}

	private NodeCluster findNextIntraMAXClusterV2(
			TreeMap<Integer, NodeCluster> currentNCs, int clusterStep) {

		NodeCluster bestCluster = null;
		double bestValue = 0;
		// want to create new clusters that are identified with a key greater
		// than the last;
		// this has the added advantage that we know how many evaluations we've
		// done, by looking at the last one
		int counter = currentNCs.lastKey() + 1;
		// logger.info("checking " + MathUtils.factorial(currentNCs.size()));
		// the treemap's keyset runs in ascending order
		HashSet<ClusterCombo> clusterCheck = new HashSet<ClusterCombo>();
		for (NodeCluster currentCluster : currentNCs.values()) {
			for (NodeCluster outCluster : currentCluster.getOutBoundClusters()) {
				outBoundClusterSize++;
				// skip if this combination is already i the hashset
				if (clusterCheck.add(new ClusterCombo(currentCluster.getId(),
						outCluster.getId()))) {
					NodeCluster nc = new NodeCluster(currentCluster,
							outCluster, this.internalFlowMethod,
							internalFlowMethodParameters, clusterStep, 0);
					double flowChange = nc.getDeltaFlow();
					// logger.info("c1: "+nc.getChild1().getId()+" c2: "+nc.getChild2().getId()+" Flowchange: "+
					// flowChange);
					// logger.info("c1: "+nc.getChild1().getInternalFlow()+" c2: "+nc.getChild2().getInternalFlow()+" nc: "+
					// nc.getInternalFlow());
					if (flowChange > bestValue) {
						bestValue = flowChange;
						bestCluster = nc;
					}
					// counter++;
					// if (counter % 1000 == 0)
					// logger.info("Checked " + counter);
				}
			}
		}
		// logger.info("Found the best cluster, with value " + bestValue);

		return bestCluster;

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

	// private List<Link> getSharedLinks(NodeCluster nc1, NodeCluster nc2) {
	// ArrayList<Link> sharedLinks = new ArrayList<Link>();
	// for (ClusterLink l : nc1.getInLinks().values()) {
	// if (nc2.getOutLinks().get(l.getId()) != null) {
	// sharedLinks;
	// }
	// }
	// for (ClusterLink l : nc1.getOutLinks().values()) {
	// if (nc2.getInLinks().get(l.getId()) != null) {
	// return true;
	// }
	// }
	// return false;
	// }

	/**
	 * returns the specified method, with the given argument types
	 * 
	 * @param methodName
	 * @param argTypesAsString
	 * @return the method
	 */
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

	private HashSet<ClusterCombo> getViableClusterCombos(
			TreeMap<Integer, NodeCluster> nodeClusters) {
		HashSet<ClusterCombo> clusterCheck = new HashSet<ClusterCombo>();
		for (NodeCluster c1 : nodeClusters.values()) {
			for (NodeCluster c2 : c1.getOutBoundClusters()) {
				outBoundClusterSize++;
				// skip if this combination is already i the hashset
				clusterCheck.add(new ClusterCombo(c1.getId(), c2.getId()));
			}
		}
//		System.out.println("Clustercombos: " + clusterCheck.size());
		return clusterCheck;
	}

	public static void main(String[] args) throws SQLException,
			NoConnectionException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, IOException {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(scenario);
		// nwr.readFile(args[0]);
		nwr.readFile("F:/TEMP/network.xml");
		// nwr.readFile("f:/matsimWorkspace/matsim/examples/siouxfalls/network-wo-dummy-node.xml");
		// nwr.readFile("data/singaporev1/network/planningNetwork_CLEAN.xml");
		NodeClusteringAlgorithm ncr = new NodeClusteringAlgorithm(scenario,
				"getCapacity", null, null);
		// ncr.run("getCapacity", new String[] { "java.lang.Double" },
		// new Object[] { new Double(3600) });
		ncr.runIntraMAX();
//		DataBaseAdmin dba = new DataBaseAdmin(new File(
//				"data/matsim2postgres.properties"));
//		ncr.writeClustersToSQL(args[1], dba);
	}

	public int getClusterSteps() {
		return clusterSteps;
	}
}

class InFlowCompare implements Comparator<NodeCluster> {

	@Override
	public int compare(NodeCluster o1, NodeCluster o2) {
		// TODO Auto-generated method stub
		return Double.compare(o1.getInFlowSum(), o2.getInFlowSum());
	}

}

class OutFlowCompare implements Comparator<NodeCluster> {

	@Override
	public int compare(NodeCluster o1, NodeCluster o2) {
		// TODO Auto-generated method stub
		return Double.compare(o1.getOutFlowSum(), o2.getOutFlowSum());
	}

}

/**
 * @author fouriep Class used to check if a particular combo has been checked by
 *         the algorithm yet
 */
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
