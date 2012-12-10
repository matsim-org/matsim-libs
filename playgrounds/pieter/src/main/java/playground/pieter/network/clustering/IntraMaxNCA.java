package playground.pieter.network.clustering;

import java.util.HashSet;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

public class IntraMaxNCA extends NodeClusteringAlgorithm {
	public IntraMaxNCA(String algorithmName,Network network, String linkMethodName,
			String[] argTypes, Object[] args){
		super(algorithmName,network,linkMethodName,argTypes,args);
	}

	@Override
	protected NodeCluster findSingleInLinkClusters(
			TreeMap<Integer, NodeCluster> clusters) {
		double maxFlow = 0;
		NodeCluster outCluster = null;
		for (int i : clusters.keySet()) {
			NodeCluster nc = clusters.get(i);
			if (nc.getInLinks().size() == 1) {
				for (ClusterLink cl : nc.getInLinks().values()) {
					NodeCluster newCluster;
					newCluster = new NodeCluster(cl.getFromCluster(), nc,
							internalFlowMethod, internalFlowMethodParameters,
							clusterSteps, cl.getFromCluster().getId());
					if (newCluster.getDeltaFlow() > maxFlow) {
						maxFlow = newCluster.getDeltaFlow();
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
		NodeCluster bestCluster = null;
		double bestValue = 0;
		HashSet<ClusterCombo> viableClusterCombos = getViableClusterCombos(currentNodeClusters);
		for (ClusterCombo cc : viableClusterCombos) {
			String[] ccs = cc.getComboId().split("z");

			int i = Integer.parseInt(ccs[0]);
			int j = Integer.parseInt(ccs[1]);
			NodeCluster nc = new NodeCluster(currentNodeClusters.get(i),
					currentNodeClusters.get(j), this.internalFlowMethod,
					internalFlowMethodParameters, clusterStep, 0);
			double flowChange = nc.getInternalFlow()
					- nc.getChild1().getInternalFlow()
					- nc.getChild2().getInternalFlow();

			if (flowChange > bestValue) {
				bestValue = flowChange;
				bestCluster = nc;
			}


		}

		return bestCluster;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(scenario);
		// nwr.readFile(args[0]);
//		 nwr.readFile("F:/TEMP/smallnet.xml");
		nwr.readFile("f:/matsimWorkspace/matsim/examples/siouxfalls/network-wo-dummy-node.xml");
		// nwr.readFile("data/singaporev1/network/planningNetwork_CLEAN.xml");
		IntraMaxNCA nca = new IntraMaxNCA("Intramax",scenario.getNetwork(),
				"getCapacityTimesLength", null, null);
		// ncr.run("getCapacity", new String[] { "java.lang.Double" },
		// new Object[] { new Double(3600) });
		nca.run();
		System.out.println(nca.getClustersAtLevel(2));

	}

}
