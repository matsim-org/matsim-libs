package playground.pieter.network.clustering;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.matsim.core.utils.io.IOUtils;

class ClusterWriter {
	public void writeClusters(String fileName, NodeClusteringAlgorithm nca) {
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		StringBuilder sb = new StringBuilder(nca.clusterSteps);
		ArrayList<String> clusterSequence = new ArrayList<>();
		// read the clusters from the top down, to reverse the entire array at
		// the end
		HashSet<NodeCluster> outClusters = new HashSet<>();
		ArrayList<NodeCluster> tempClusters = new ArrayList<>();
		tempClusters.addAll(nca.getRootClusters().values());
		int currentLevel = 0;
		// first, find the highest nodecluster in the set
		for (NodeCluster nc : nca.getRootClusters().values()) {
			if (nc.getClusterStepFormed() > currentLevel)
				currentLevel = nc.getClusterStepFormed();
		}
		boolean done = false;
		sb.append(nca.getAlgorithmName()).append("\n");
		sb.append(nca.internalFlowMethod.getName()).append("\n");
		sb.append(nca.internalFlowMethodParameterTypes).append("\n");
		sb.append(nca.internalFlowMethodParameters).append("\n");
		sb.append("STEP\tID1\tID2\tID_OUT\n");
		int currentPoint = 0;
		while (!done) {
			if (currentLevel == 0) {
				done = true;
			} else {
				ArrayList<NodeCluster> tempClusters2 = new ArrayList<>();
				outClusters.addAll(tempClusters);
				tempClusters2.addAll(tempClusters);
				tempClusters = new ArrayList<>();
				for (NodeCluster nc : tempClusters2) {
					if (nc.getClusterStepFormed() < currentLevel || nc.isLeaf())
						tempClusters.add(nc);
					else
						tempClusters.addAll(nc.getChildren().values());

				}
				currentLevel--;
			}
		}
		outClusters.addAll(tempClusters);
		//hashset is not comparable
		tempClusters = new ArrayList<>();
		tempClusters.addAll(outClusters);
		Collections.sort(tempClusters, new NodeCluster.StepFormedComparator());
		for (NodeCluster nc : tempClusters) {
			if (!nc.isLeaf()) {
				sb.append(String.format("%06d\t%06d\t%06d\t%06d\n",
						nc.getClusterStepFormed(), nc.getChild1().getId(),
						nc.getChild2().getId(), nc.getId()));
			}else{
				sb.append(String.format("%06d\t%s\t%06d\n",
						nc.getClusterStepFormed(), nc.getNodes().keySet().iterator().next().toString(),
						 nc.getId()));
			}

		}
		try {
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
}
