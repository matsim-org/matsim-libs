package playground.pieter.network.clustering;

import java.util.HashSet;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;


public class IntraMaxNCA extends NodeClusteringAlgorithm {
	private static final String ALGORITHMNAME = "IntraMAX";
	public static int NUMTHREADS=4;
	private IntraMaxNCA(Network network, String linkMethodName,
                        String[] argTypes, Object[] args){
		super(ALGORITHMNAME,network,linkMethodName,argTypes,args);
	}
	private IntraMaxNCA(Network network){
		super(ALGORITHMNAME, network);
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
		String fileName = "f:/TEMP/siouxfalls.txt";
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		MatsimNetworkReader nwr = new MatsimNetworkReader(scenario);
		// nwr.readFile(args[0]);
//		 nwr.readFile("F:/TEMP/smallnet.xml");
		nwr.readFile("f:/matsimWorkspace/matsim/examples/siouxfalls/network-wo-dummy-node.xml");
//		 nwr.readFile("data/singaporev1/network/planningNetwork_CLEAN.xml");
		IntraMaxNCA nca = new IntraMaxNCA(scenario.getNetwork(),
				"getCapacityTimesSpeed", null, null);
		// ncr.run("getCapacity", new String[] { "java.lang.Double" },
		// new Object[] { new Double(3600) });
		nca.run();
		new ClusterWriter().writeClusters(fileName,nca);
		IntraMaxNCA nca2 = new IntraMaxNCA(scenario.getNetwork());
		
		new ClusterReader().readClusters(fileName, scenario.getNetwork(), nca2);
		for(int i=0; i<=nca.getClusterSteps();i++){
			nca.logger.info(nca.getClustersAtLevel(i).size()+": largest = "+nca.getLargestCluster(nca.getClustersAtLevel(i)).getInternalFlow() );
			nca2.logger.info(nca2.getClustersAtLevel(i).size()+": largest = "+nca2.getLargestCluster(nca2.getClustersAtLevel(i)).getInternalFlow() );
			
		}
		for(int i=nca.getClusterSteps(); i>=0;i--){
			nca.logger.info(nca.getClustersAtLevel(i).size()+": largest = "+nca.getLargestCluster(nca.getClustersAtLevel(i)).getInternalFlow() );
			nca2.logger.info(nca2.getClustersAtLevel(i).size()+": largest = "+nca2.getLargestCluster(nca2.getClustersAtLevel(i)).getInternalFlow() );
			
		}
		

	}


}
