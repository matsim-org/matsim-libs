package playground.pieter.network.clustering;

import java.util.HashSet;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;


public class IntraMinDeltaOutLinksTabuNCA extends NodeClusteringAlgorithm {
	private static final String ALGORITHMNAME = "IntraMinDeltaOutLinksTabuNCA";
	public static int NUMTHREADS = 4;
	private Tuple<Integer, Double> averageForThisClusterStep;
//	private ArrayList<Integer> tabuList;

	private IntraMinDeltaOutLinksTabuNCA(Network network, String linkMethodName,
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
			sum += nc.getOutLinks().size();
		}

		averageForThisClusterStep = new Tuple<>(clusterSteps,
				sum / clusters.size());

    }

	private IntraMinDeltaOutLinksTabuNCA(Network network) {
		super(ALGORITHMNAME, network);
	}

	@Override
	protected NodeCluster findSingleInLinkClusters(
			TreeMap<Integer, NodeCluster> clusters) {
		findAverageForClusterStep(clusters);
		double minDeltaOutLinksCount = Double.POSITIVE_INFINITY;
		double target = 0;
		NodeCluster outCluster = null;
		if(tabuList == null)
			tabuList = new HashSet<>();
		
		
		for (int i : clusters.keySet()) {
			NodeCluster nc = clusters.get(i);
			if (nc.getInLinks().size() == 1) {
				for (ClusterLink cl : nc.getInLinks().values()) {
					NodeCluster newCluster;
					newCluster = new NodeCluster(cl.getFromCluster(), nc,
							internalFlowMethod, internalFlowMethodParameters,
							clusterSteps, cl.getFromCluster().getId());
					if (Math.abs((double)newCluster.getOutLinkSize()-target) < minDeltaOutLinksCount) {
						minDeltaOutLinksCount = Math.abs((double)newCluster.getOutLinkSize()-target) ;
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
		double minDeltaOutLinksCount = Double.POSITIVE_INFINITY;
		double target = 0;
		HashSet<ClusterCombo> viableClusterCombos = getViableClusterCombos(currentNodeClusters,true);

		for (ClusterCombo cc : viableClusterCombos) {
			String[] ccs = cc.getComboId().split("z");
			NodeCluster nc1 = null;
			NodeCluster nc2 = null;
			int i = Integer.parseInt(ccs[0]);
			int j = Integer.parseInt(ccs[1]);

				nc1 = currentNodeClusters.get(i);
				nc2 = currentNodeClusters.get(j);
				

			
			NodeCluster newCluster = new NodeCluster(nc1,
					nc2, this.internalFlowMethod,
					internalFlowMethodParameters, clusterStep, 0);

//			double flowChange = nc.getInternalFlow()
//					- nc.getChild1().getInternalFlow()
//					- nc.getChild2().getInternalFlow();

			if (Math.abs((double)newCluster.getOutLinkSize()-target) < minDeltaOutLinksCount) {
				minDeltaOutLinksCount = Math.abs((double)newCluster.getOutLinkSize()-target) ;
				outCluster = newCluster;
			}

		}

		if(outCluster != null){
			tabuList.add(outCluster.getChild1().getId());
			tabuList.add(outCluster.getChild2().getId());
		}
		
		return outCluster;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName = "f:/TEMP/singmindeltaoutlinkstabu.txt";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(scenario.getNetwork());
		// nwr.readFile(args[0]);
//		 nwr.readFile("f:/matsimWorkspace/playgrounds/pieter/data/zurich/horni/network.xml.gz");
//		nwr.readFile("f:/matsimWorkspace/matsim/examples/siouxfalls/network-wo-dummy-node.xml");
//		nwr.readFile("f:/TEMP/network.xml");
		 nwr.readFile("data/singaporev1/network/planningNetwork_CLEAN.xml");
		 IntraMinDeltaOutLinksTabuNCA nca = new IntraMinDeltaOutLinksTabuNCA(scenario.getNetwork(),
				"getCapacity", null, null);
		// ncr.run("getCapacity", new String[] { "java.lang.Double" },
		// new Object[] { new Double(3600) });
		nca.run();
		new ClusterWriter().writeClusters(fileName, nca);
		IntraMinDeltaOutLinksTabuNCA nca2 = new IntraMinDeltaOutLinksTabuNCA(
				scenario.getNetwork());

		new ClusterReader().readClusters(fileName, scenario.getNetwork(), nca2);
		for (int i = 0; i <= nca.getClusterSteps(); i++) {
//			nca.logger.info(nca.getClustersAtLevel(i).size()
//					+ ": largest = "
//					+ nca.getLargestCluster(nca.getClustersAtLevel(i))
//							.getOutLinks().size());
			nca2.logger.info(nca2.getClustersAtLevel(i).size()
					+ ": largest = "
					+ nca2.getLargestCluster(nca2.getClustersAtLevel(i))
							.getOutLinks().size());
		}
	}
	
	HashSet<ClusterCombo> getViableClusterCombos(
            TreeMap<Integer, NodeCluster> nodeClusters, boolean tabu){
		HashSet<ClusterCombo> clusterCheck = new HashSet<>();
		if(!tabu){
			return getViableClusterCombos(nodeClusters);
		}
		for (NodeCluster c1 : nodeClusters.values()) {
			if(tabuList.contains(c1.getId()))
					continue;
			for (NodeCluster c2 : c1.getOutBoundClusters()) {
				if(tabuList.contains(c2.getId()))
					continue;
				outBoundClusterSize++;
				// skip if this combination is already in the hashset
				clusterCheck.add(new ClusterCombo(c1.getId(), c2.getId()));
			}
		}
		// System.out.println("Clustercombos: " + clusterCheck.size());
		if(clusterCheck.size() == 0 ){//all tabu
			tabuList = new HashSet<>();
			System.err.println("All tabu, cleaning up");
			return getViableClusterCombos(nodeClusters);
		}
		return clusterCheck;
	}
}
