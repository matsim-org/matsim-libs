package playground.dhosse.bachelorarbeit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.charts.BarChart;

public class NetworkInspector {
	
	private final Network net;
	
	private Map<Id,Double> distances = new HashMap<Id,Double>();
	
	private Logger logger = Logger.getLogger(NetworkInspector.class);
	
	private DecimalFormat df = new DecimalFormat(",##0.00");
	
	public NetworkInspector(final Network net){
		
		this.net = net;
		
	}
	
	public void isRoutable(){
		
		logger.info("network contains " + this.net.getNodes().size() + " nodes and " +
		this.net.getLinks().size() + " links");
		
		NetworkCleaner nc = new NetworkCleaner();
		nc.run(this.net);
		
	}
	
	public void checkNetworkAttributes(boolean checkLinks, boolean checkNodes){
		if(checkLinks){
			try {
				checkLinkAttributes();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(checkNodes){
				checkNodeAttributes();
		}
	}
	
	public void checkLinkAttributes() throws IOException{
		logger.info("checking link attributes...");
		
		File file = new File("./test/lengthBelowStats.txt");
		FileWriter writer = new FileWriter(file);
		int writerIndex = 0;
		
		double[] nLanes = new double[6];
		
		for(Link link : net.getLinks().values()){
			double distance = 
					Math.sqrt(Math.pow(link.getToNode().getCoord().getX() -
							link.getFromNode().getCoord().getX(),2) +
							Math.pow(link.getToNode().getCoord().getY() -
							link.getFromNode().getCoord().getY(), 2));
			
			int numberOfLanes = (int) link.getNumberOfLanes();
			nLanes[numberOfLanes-1]++;
			
			this.distances.put(link.getId(), distance);
			
			if(link.getLength()<7){
				writer.write("length of link " + link.getId() + " below" +
						" min storage capacity for one vehicle (" + df.format(link.getLength()) +
						" m instead of 7 m)\n");
				writerIndex++;
			}
		}
		writer.close();
		
		logger.info("done.");
		
		if(writerIndex>0){
			logger.warn(writerIndex + " warnings about storage capacity. written to file " + file.getName());
//		System.out.println("nlinks: "+net.getLinks().size());
	}
	
		logger.info("writing link statistics files...");
		
		createLaneStatisticsFiles(nLanes);
		
		createLinkLengthComparisonFile();
		
		logger.info("done.");

//		System.out.println("nnodes: "+net.getNodes().size());
	}
	
	public void checkNodeAttributes() {
		
		logger.info("checking node attributes...");
		
		double[] inDegrees = new double[10];
		double[] outDegrees = new double[10];
		
		for(Node node : net.getNodes().values()){
			inDegrees[node.getInLinks().size()-1]++;
			outDegrees[node.getOutLinks().size()-1]++;
		}
		
		logger.info("done.");
		
		logger.info("writing node statistics files...");
		
		try {
			createNodeDegreesFiles(inDegrees, outDegrees);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.info("done.");
		
	}
	
	private void createLaneStatisticsFiles(double[] values) throws IOException {
		
		logger.info("writing lane statistics file...");
		
		File file = new File("./test/laneStatistics.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("Degree");
		for(int i=0;i<values.length;i++){
			writer.write("\t"+ (i+1));
		}
		writer.write("\nnObjects");
		for(int j=0;j<values.length;j++){
			writer.write("\t"+values[j]);
		}
		writer.close();
		
		BarChart chart = new BarChart("Number of lanes", "number of lanes", "number of objects");
		chart.addSeries("nlanes", values);
		chart.saveAsPng("./test/laneStatistics.png", 800, 600);
	}

	private void createLinkLengthComparisonFile() throws IOException {
		
		logger.info("writing length statistics file...");
		
		double length = 0;
		double gLength = 0;

		File file = new File("./test/linkLengthComparison.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("Id\tlength\tactualLength");
		
		for(Id id : this.distances.keySet()){
			writer.write("\n"+id.toString()+"\t"+this.net.getLinks().get(id).getLength()+
					"\t"+this.distances.get(id));
			length += this.net.getLinks().get(id).getLength();
			gLength += this.distances.get(id);
		}
		
		writer.close();
		System.out.println("total length of network: " + length + "m\n"
				+ "geom. length of network: " + gLength + "m");
	}

	private void createNodeDegreesFiles(double[] inDegrees, double[] outDegrees) throws IOException {
		
		logger.info("writing node degrees files");
		
		File file = new File("./test/nodeDegrees.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("in/out\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\nnobjects");
		
		for(int i=0;i<inDegrees.length;i++){
			writer.write("\t"+inDegrees[i]+"/"+outDegrees[i]);
		}
		writer.close();

		BarChart chart = new BarChart("Node degrees", "degree", "number of objects");
		chart.addSeries("in-degrees", inDegrees);
		chart.addSeries("out-degrees", outDegrees);
		chart.saveAsPng("./test/nodeDegrees.png", 800, 600);
	}
	
//	private void createLinkCapacityFiles() throws IOException{
//		File file = new File("./test/linkCapacities.txt");
//		FileWriter writer = new FileWriter(file);
//		writer.write("nObjects");
//	}

}
