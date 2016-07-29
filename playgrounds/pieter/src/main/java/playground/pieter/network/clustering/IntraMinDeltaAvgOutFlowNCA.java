package playground.pieter.network.clustering;

import java.util.HashSet;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;


public class IntraMinDeltaAvgOutFlowNCA extends NodeClusteringAlgorithm {
	private static final String ALGORITHMNAME = "IntraMinDeltaAvgOutFlowNCA";
	public static int NUMTHREADS = 4;
	private Tuple<Integer, Double> averageForThisClusterStep;

	private IntraMinDeltaAvgOutFlowNCA(Network network, String linkMethodName,
                                       String[] argTypes, Object[] args) {
		super(ALGORITHMNAME, network, linkMethodName, argTypes, args);
		averageForThisClusterStep = new Tuple<>(
				Integer.MIN_VALUE, Double.NaN);
	}

	private void findAverageForClusterStep(
			TreeMap<Integer, NodeCluster> clusters) {
		if (averageForThisClusterStep.getFirst() == clusterSteps)
			return;

		double sum = 0;
		for (NodeCluster nc : clusters.values()) {
			sum += nc.getOutFlowSum();
		}

		averageForThisClusterStep = new Tuple<>(clusterSteps,
				sum / clusters.size());

    }

	private IntraMinDeltaAvgOutFlowNCA(Network network) {
		super(ALGORITHMNAME, network);
	}

	@Override
	protected NodeCluster findSingleInLinkClusters(
			TreeMap<Integer, NodeCluster> clusters) {
		findAverageForClusterStep(clusters);
		double minDelta = Double.POSITIVE_INFINITY;
		double target = averageForThisClusterStep.getSecond();
		NodeCluster outCluster = null;
		for (int i : clusters.keySet()) {
			NodeCluster nc = clusters.get(i);
			if (nc.getInLinks().size() == 1) {
				for (ClusterLink cl : nc.getInLinks().values()) {
					NodeCluster newCluster;
					newCluster = new NodeCluster(cl.getFromCluster(), nc,
							internalFlowMethod, internalFlowMethodParameters,
							clusterSteps, cl.getFromCluster().getId());
					if (Math.abs(newCluster.getOutFlowSum() -target) < minDelta) {
						minDelta = Math.abs(newCluster.getOutFlowSum() -target) ;
						outCluster = newCluster;
					}
				}
			}
		}
		return outCluster;
	}

	@Override
	protected NodeCluster findNextCluster(
			TreeMap<Integer, NodeCluster> currentNodeClusters, int clusterStep) {
		NodeCluster outCluster = null;
		double minDelta = Double.POSITIVE_INFINITY;
		double target = averageForThisClusterStep.getSecond();
		HashSet<ClusterCombo> viableClusterCombos = getViableClusterCombos(currentNodeClusters);
		for (ClusterCombo cc : viableClusterCombos) {
			String[] ccs = cc.getComboId().split("z");

			int i = Integer.parseInt(ccs[0]);
			int j = Integer.parseInt(ccs[1]);
			NodeCluster newCluster = new NodeCluster(currentNodeClusters.get(i),
					currentNodeClusters.get(j), this.internalFlowMethod,
					internalFlowMethodParameters, clusterStep, 0);
//			double flowChange = nc.getInternalFlow()
//					- nc.getChild1().getInternalFlow()
//					- nc.getChild2().getInternalFlow();

			if (Math.abs(newCluster.getOutFlowSum() -target) < minDelta) {
				minDelta = Math.abs(newCluster.getOutFlowSum() -target) ;
				outCluster = newCluster;
			}

		}

		return outCluster;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName = "f:/TEMP/singmindeltaAVGoutflow.txt";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(scenario.getNetwork());
		// nwr.readFile(args[0]);
//		 nwr.readFile("F:/TEMP/network.xml");
//		nwr.readFile("f:/matsimWorkspace/matsim/examples/siouxfalls/network-wo-dummy-node.xml");
		 nwr.readFile("data/singaporev1/network/planningNetwork_CLEAN.xml");
		 IntraMinDeltaAvgOutFlowNCA nca = new IntraMinDeltaAvgOutFlowNCA(scenario.getNetwork(),
				"getCapacity", null, null);
		// ncr.run("getCapacity", new String[] { "java.lang.Double" },
		// new Object[] { new Double(3600) });
		nca.run();
		new ClusterWriter().writeClusters(fileName, nca);
		IntraMinDeltaAvgOutFlowNCA nca2 = new IntraMinDeltaAvgOutFlowNCA(
				scenario.getNetwork());

		new ClusterReader().readClusters(fileName, scenario.getNetwork(), nca2);
		for (int i = 0; i <= nca.getClusterSteps(); i++) {
			nca.logger.info(nca.getClustersAtLevel(i).size()
					+ ": largest = "
					+ nca.getLargestCluster(nca.getClustersAtLevel(i))
							.getOutLinks().size());
			nca2.logger.info(nca2.getClustersAtLevel(i).size()
					+ ": largest = "
					+ nca2.getLargestCluster(nca2.getClustersAtLevel(i))
							.getOutLinks().size());

		}

	}

}
