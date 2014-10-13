package playground.pieter.network.clustering;

//import groovy.lang.Binding;
//import groovy.lang.GroovyShell;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.io.IOUtils;

class ClusterReader {

    public void readClusters(String fileName, Network network,
			NodeClusteringAlgorithm nca) {
		BufferedReader reader = IOUtils.getBufferedReader(fileName);
//		if (true) {
//			throw new RuntimeException("Commented out non-compiling code. Please check your code. mrieser/14dec2012");
//		}
		Binding binding = new Binding();
		GroovyShell shell = new GroovyShell(binding);
		Object value = shell
				.evaluate("for (x=0; x<5; x++){println \"Hello\"}; return x");
		int i = 0;
		String algoName = null;
		String linkMethodName = null;
		String[] linkMethodParamTypes = null;
		Object[] linkMethodParameters = null;
		try {
			while(reader.ready()){				
				String line = reader.readLine();
				switch (i) {
				case 0:
					algoName = line;
					break;
				case 1:
					linkMethodName = line;
					break;
				case 2:
					linkMethodParamTypes = (String[]) shell.evaluate(line);
					break;
				case 3:
					linkMethodParameters = (Object[]) shell.evaluate(line);
					break;
				default:
					break;
				}
				if (!algoName.equals(nca.getAlgorithmName())) {
					throw new IllegalArgumentException(
							"This cluster history requires algo type " + algoName
							+ " but passing algorithm type "
							+ nca.getAlgorithmName());
				}
				if (i > 4) {
					if (nca.getInternalFlowMethod() == null) {
						Method method = NodeClusteringAlgorithm
								.getLinkGetMethodWithArgTypes(linkMethodName,
										linkMethodParamTypes);
						nca.setInternalFlowMethod(method);
						nca.setInternalFlowMethodParameterTypes(linkMethodParamTypes);
						nca.setInternalFlowMethodParameters(linkMethodParameters);
					}
					String[] split = line.split("\t");
					if (split.length == 3) {
						Id nodeId = Id.createNodeId(split[1]);
						ClusterNode cn = new ClusterNode((NodeImpl) network
								.getNodes().get(nodeId));
						NodeCluster nc = new NodeCluster(cn, nca, 0,
								Integer.parseInt(split[2]),
								nca.getInternalFlowMethod(),
								nca.getInternalFlowMethodParameters());
						nca.getNodes().put(nodeId, cn);
						nca.getLeafNodeClusters().put(Integer.parseInt(split[2]),
								nc);
					} else {
						nca.createArbitraryClusterTree(Integer.parseInt(split[0]),
								Integer.parseInt(split[1]),
								Integer.parseInt(split[2]),
								Integer.parseInt(split[3]));
					}
				}
				
				i++;
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
