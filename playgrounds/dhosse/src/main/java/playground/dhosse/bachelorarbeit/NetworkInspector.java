package playground.dhosse.bachelorarbeit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.charts.BarChart;

public class NetworkInspector {
	
	private final Network net;
	
	private Map<Id,Integer> inDegrees = new HashMap<Id,Integer>();
	private Map<Id,Integer> outDegrees = new HashMap<Id,Integer>();
	private Map<Id,Double> distances = new HashMap<Id,Double>();
	
	public NetworkInspector(final Network net){
		this.net = net;
	}
	
	public void checkNetworkAttributes(boolean checkLinks, boolean checkNodes){
		if(checkLinks){
			checkLinkAttributes();
		}
		if(checkNodes){
			checkNodeAttributes();
		}
		try {
			createHistograms();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void checkLinkAttributes(){
		for(Link link : net.getLinks().values()){
			double distance = 
					Math.sqrt(Math.pow(link.getToNode().getCoord().getX()-link.getFromNode().getCoord().getX(),2) +
					Math.pow(link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY(), 2));
			this.distances.put(link.getId(), distance);
//			if((distance<7)||length<7){
//				System.err.println("Length of link " + link.getId() + " smaller than min: dist "+distance+",length "+length);
//			}
//			System.out.println("nlanes: " + link.getNumberOfLanes());
		}
		System.out.println("nlinks: "+net.getLinks().size());
	}
	
	public void checkNodeAttributes(){
		for(Node node : net.getNodes().values()){
			inDegrees.put(node.getId(), node.getInLinks().size());
			outDegrees.put(node.getId(), node.getOutLinks().size());
		}
		System.out.println("nnodes: "+net.getNodes().size());
	}
	
	private void createHistograms() throws IOException{

		double[] inDegrees = new double[10];
		double[] outDegrees = new double[10];
		double[] nLanes = new double[6];
		
		for(Integer degree : this.inDegrees.values()){
			inDegrees[degree-1]++;
		}
		for(Integer degree : this.outDegrees.values()){
			outDegrees[degree-1]++;
		}
		
		for(Link link:this.net.getLinks().values()){
			int numberOfLanes = (int) link.getNumberOfLanes();
			nLanes[numberOfLanes-1]++;
		}
		
		createNodeDegreesFiles(inDegrees, outDegrees);
		createLinkLengthComparisonFile();
		createLaneStatisticsFiles(nLanes);
	}

	private void createLaneStatisticsFiles(double[] values) throws IOException {
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
		File file = new File("./test/linkLengthComparison.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("Id\tlength\tactualLength\n");
		for(Link link : net.getLinks().values()){
			writer.write(link.getId().toString()+"\t"+link.getLength()+"\t"+this.distances.get(link.getId())+"\n");
		}
		writer.close();
	}

	private void createNodeDegreesFiles(double[] inDegrees, double[] outDegrees) throws IOException {
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

}
