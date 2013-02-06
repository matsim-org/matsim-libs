package playground.dhosse.bachelorarbeit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.charts.BarChart;

public class NetworkInspector {
	
	private final Scenario scenario;
	
	private Map<Id,Double> distances = new HashMap<Id,Double>();
	
	private Logger logger = Logger.getLogger(NetworkInspector.class);
	
	private DecimalFormat df = new DecimalFormat(",##0.00");
	
	private double[] linkCapacities = new double[7];
	
	private double[] nLanes = new double[6];
	
	private String outputFolder = "C:/Users/Daniel/Dropbox/bsc";
	
	public NetworkInspector(final Scenario sc){
		
		this.scenario = sc;
		
	}
	
	public void isRoutable(){
		
		logger.info("network contains " + this.scenario.getNetwork().getNodes().size() +
				" nodes and " + this.scenario.getNetwork().getLinks().size() + " links");
		
		NetworkCleaner nc = new NetworkCleaner();
		nc.run(this.scenario.getNetwork());
	}
	
	public void checkLinkAttributes() throws IOException{
		logger.info("checking link attributes...");
		
		File file = new File(this.outputFolder+"/test/lengthBelowStats.txt");
		FileWriter writer = new FileWriter(file);
		int writerIndex = 0;
		
		for(Link link : this.scenario.getNetwork().getLinks().values()){
			double distance = 
					Math.sqrt(Math.pow(link.getToNode().getCoord().getX() -
							link.getFromNode().getCoord().getX(),2) +
							Math.pow(link.getToNode().getCoord().getY() -
							link.getFromNode().getCoord().getY(), 2));
			
			int numberOfLanes = (int) link.getNumberOfLanes();
			this.nLanes[numberOfLanes-1]++;
			
			if(link.getCapacity()<=125)//kapazitäten nach ras-q
				this.linkCapacities[0]++;
			else if(link.getCapacity()<=625&&link.getCapacity()>125)
				this.linkCapacities[1]++;
			else if(link.getCapacity()<=830&&link.getCapacity()>625)
				this.linkCapacities[2]++;
			else if(link.getCapacity()<=1250&&link.getCapacity()>830)
				this.linkCapacities[3]++;
			else if(link.getCapacity()<=2500&&link.getCapacity()>1250)
				this.linkCapacities[4]++;
			else if(link.getCapacity()<=3000&&link.getCapacity()>2500)
				this.linkCapacities[5]++;
			else if(link.getCapacity()>4000)
				this.linkCapacities[6]++;
			
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
		
		createLaneStatisticsFiles();
		
		createLinkLengthComparisonFile();
		
		createLinkCapacityFiles();
		
		logger.info("done.");

//		System.out.println("nnodes: "+net.getNodes().size());
	}
	
	public void checkNodeAttributes() {
		
		logger.info("checking node attributes...");
		
		double[] inDegrees = new double[10];
		double[] outDegrees = new double[10];
		
		for(Node node : this.scenario.getNetwork().getNodes().values()){
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
	
	private void createLaneStatisticsFiles() throws IOException {
		
		logger.info("writing lane statistics file...");
		
		File file = new File(this.outputFolder+"/test/laneStatistics.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("Degree");
		for(int i=0;i<this.nLanes.length;i++){
			writer.write("\t"+ (i+1));
		}
		writer.write("\nnObjects");
		for(int j=0;j<this.nLanes.length;j++){
			writer.write("\t"+this.nLanes[j]);
		}
		writer.close();
		
		BarChart chart = new BarChart("Number of lanes", "number of lanes", "number of objects");
		chart.addSeries("nlanes", this.nLanes);
		chart.saveAsPng(this.outputFolder+"/test/laneStatistics.png", 800, 600);
	}

	private void createLinkLengthComparisonFile() throws IOException {
		
		logger.info("writing length statistics file...");
		
		double length = 0;
		double gLength = 0;

		File file = new File(this.outputFolder+"/test/linkLengthComparison.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("Id\tlength\tactualLength");
		
		for(Id id : this.distances.keySet()){
			writer.write("\n"+id.toString()+"\t"+
					this.scenario.getNetwork().getLinks().get(id).getLength()+
					"\t"+this.distances.get(id));
			length += this.scenario.getNetwork().getLinks().get(id).getLength();
			gLength += this.distances.get(id);
		}
		
		writer.close();
		System.out.println("total length of network links: " + length + "m\n"
				+ "geom. length of network links: " + gLength + "m");
	}
	
private void createLinkCapacityFiles() throws IOException{
		
		logger.info("writing capacities statistics file...");
		
		//kategorien: 500|1000|1500|2000|>...
		String[] categories = new String[7];
		categories[0]="<=125";
		categories[1]="<=625";
		categories[2]="<=830";
		categories[3]="<=1250";
		categories[4]="<=2500";
		categories[5]="<=3000";
		categories[6]=">4000";
		
		BarChart chart = new BarChart("Link capacities","capacity","number of objects",categories);
		chart.addSeries("capacities", this.linkCapacities);
		chart.saveAsPng(this.outputFolder+"/test/linkCapacities.png", 800, 600);
		
	}

	private void createNodeDegreesFiles(double[] inDegrees, double[] outDegrees) throws IOException {
		
		logger.info("writing node degrees files");
		
		File file = new File(this.outputFolder+"/test/nodeDegrees.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("in/out\t1\t2\t3\t4\t5\t6\t7\t8\t9\t10\nnobjects");
		
		for(int i=0;i<inDegrees.length;i++){
			writer.write("\t"+inDegrees[i]+"/"+outDegrees[i]);
		}
		writer.close();

		BarChart chart = new BarChart("Node degrees", "degree", "number of objects");
		chart.addSeries("in-degrees", inDegrees);
		chart.addSeries("out-degrees", outDegrees);
		chart.saveAsPng(this.outputFolder+"/test/nodeDegrees.png", 800, 600);
	}
	
}
