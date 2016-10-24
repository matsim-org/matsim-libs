package playground.pieter.network.clustering;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.NetworkReaderMatsimV2;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.HashSet;
import java.util.TreeMap;


public class MinmizeNumberOfOutlinksNCA extends NodeClusteringAlgorithm {
	private static final String ALGORITHMNAME = "MinimizeNumberOfOutlinks";
	public static int NUMTHREADS=4;
	public MinmizeNumberOfOutlinksNCA(Network network, String linkMethodName,
									 String[] argTypes, Object[] args){
		super(ALGORITHMNAME,network,linkMethodName,argTypes,args);
	}
	private MinmizeNumberOfOutlinksNCA(Network network){
		super(ALGORITHMNAME, network);
	}
	@Override
	protected NodeCluster findSingleInLinkClusters(
			TreeMap<Integer, NodeCluster> clusters) {
		int minOutLinks = Integer.MAX_VALUE;
		NodeCluster outCluster = null;
		for (int i : clusters.keySet()) {
			NodeCluster nc = clusters.get(i);
			if (nc.getInLinks().size() == 1) {
				for (ClusterLink cl : nc.getInLinks().values()) {
					NodeCluster newCluster;
					newCluster = new NodeCluster(cl.getFromCluster(), nc,
							internalFlowMethod, internalFlowMethodParameters,
							clusterSteps, cl.getFromCluster().getId());
					int outLinkSize = newCluster.getOutLinkSize();
					if (outLinkSize < minOutLinks) {
						minOutLinks = outLinkSize;
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
		int bestValue = Integer.MAX_VALUE;
		HashSet<ClusterCombo> viableClusterCombos = getViableClusterCombos(currentNodeClusters);
		for (ClusterCombo cc : viableClusterCombos) {
			String[] ccs = cc.getComboId().split("z");

			int i = Integer.parseInt(ccs[0]);
			int j = Integer.parseInt(ccs[1]);
			NodeCluster nc = new NodeCluster(currentNodeClusters.get(i),
					currentNodeClusters.get(j), this.internalFlowMethod,
					internalFlowMethodParameters, clusterStep, 0);
			int outLinkSize = nc.getOutLinkSize();

			if (outLinkSize < bestValue) {
				bestValue = outLinkSize;
				bestCluster = nc;
			}


		}


		return bestCluster;
	}


	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String fileName = args[1];
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils
				.createConfig());
		NetworkReaderMatsimV2 nwr = new NetworkReaderMatsimV2(scenario.getNetwork());
		// nwr.readFile(args[0]);
//		 nwr.readFile("F:/TEMP/smallnet.xml");
		nwr.readFile(args[0]);
//		 nwr.readFile("data/singaporev1/network/planningNetwork_CLEAN.xml");
		MinmizeNumberOfOutlinksNCA nca = new MinmizeNumberOfOutlinksNCA(scenario.getNetwork(),
				"getCapacityTimesSpeed", null, null);
		// ncr.run("getCapacity", new String[] { "java.lang.Double" },
		// new Object[] { new Double(3600) });
		nca.run();
		new ClusterWriter().writeClusters(fileName,nca);
		MinmizeNumberOfOutlinksNCA nca2 = new MinmizeNumberOfOutlinksNCA(scenario.getNetwork());
		
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
