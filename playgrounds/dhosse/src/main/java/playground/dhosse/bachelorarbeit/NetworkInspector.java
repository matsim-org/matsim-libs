package playground.dhosse.bachelorarbeit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.charts.BarChart;
import org.opengis.geometry.BoundingBox;

public class NetworkInspector {
	
	private final Scenario scenario;
	
	private Map<Id,Double> distances = new HashMap<Id,Double>();
	
	private List<Link> lengthBelowStorageCapacity = new ArrayList<Link>();
	
	private List<Node> deadEnds = new ArrayList<Node>();
	private List<Node> exitRoads = new ArrayList<Node>();
	
	private Logger logger = Logger.getLogger(NetworkInspector.class);
	
	private double[] linkCapacities = new double[7];
	
	private double[] nLanes = new double[6];
	
	private double[] inDegrees = new double[10];
	private double[] outDegrees = new double[10];
	
	private double[] lengths = new double[9];
	
	private double totalLength = 0;
	private double totalGLength = 0;
	
	private String outputFolder = "C:/Users/Daniel/Dropbox/bsc";
	
	public NetworkInspector(final Scenario sc){
		
		this.scenario = sc;
		
	}
	
	public boolean isRoutable(){
		
		logger.info("network contains " + this.scenario.getNetwork().getNodes().size() +
				" nodes and " + this.scenario.getNetwork().getLinks().size() + " links");
		
//		NetworkCleaner nc = new NetworkCleaner();
//		nc.run(this.scenario.getNetwork());
		
		return false;
		
	}
	
	public void checkLinkAttributes() {
		
		logger.info("checking link attributes...");
		
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
			
			if(link.getLength()<7)
				this.lengths[0]++;
			else if(link.getLength()<15&&link.getLength()>=7)
				this.lengths[1]++;
			else if(link.getLength()<50&&link.getLength()>=15)
				this.lengths[2]++;
			else if(link.getLength()<100&&link.getLength()>=50)
				this.lengths[3]++;
			else if(link.getLength()>=100&&link.getLength()<150)
				this.lengths[4]++;
			else if(link.getLength()>=150&&link.getLength()<200)
				this.lengths[5]++;
			else if(link.getLength()>=200&&link.getLength()<300)
				this.lengths[6]++;
			else if(link.getLength()>=300&&link.getLength()<500)
				this.lengths[7]++;
			else if(link.getLength()>=500)
				this.lengths[8]++;
			
			this.distances.put(link.getId(), distance);
			
			this.totalLength += link.getLength();
			this.totalGLength += distance;
			
			if(link.getLength()<7){
				this.lengthBelowStorageCapacity.add(link);
				writerIndex++;
			}
		}
		
		
		logger.info("done.");
		
		logger.info("writing link statistics files...");
		
		File file = new File(this.outputFolder+"/test/lengthBelowStorageCapacity.txt");
		FileWriter writer;
		try {
			
			writer = new FileWriter(file);
			for(Link link : this.lengthBelowStorageCapacity){
				writer.write("length of link " + link.getId() + " below min length for storage capacity of one vehicle (" +
						link.getLength() + "m instead of 7 m)\n");
			}
			writer.close();
			
			createLaneStatisticsFiles();
			
			createLinkLengthComparisonFile();
			
			createLinkCapacityFiles();
			
			createLinkLengthFiles();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(writerIndex>0)
			logger.warn(writerIndex + " warnings about storage capacity. written to file " + file.getName());
		
		logger.info("total length of network: " + this.totalLength + "m, total geom. length: " + this.totalGLength + "m");
	
		logger.info("done.");

	}
	
	public void checkNodeAttributes() {
		
		logger.info("checking node attributes...");
		
		NetworkBoundaryBox bbox = new NetworkBoundaryBox();
		bbox.setDefaultBoundaryBox(this.scenario.getNetwork());
		
		NetworkBoundaryBox bbox2 = new NetworkBoundaryBox();
		double factor = 0.95;
		bbox2.setCustomBoundaryBox(bbox.getXMin()*factor, bbox.getYMin()*factor, bbox.getXMax()*factor, bbox.getYMax()*factor);
		
		for(Node node : this.scenario.getNetwork().getNodes().values()){
			
			inDegrees[node.getInLinks().size()-1]++;
			outDegrees[node.getOutLinks().size()-1]++;
			
			if(node.getInLinks().size()==1&&node.getOutLinks().size()==1){
				
				Link inLink = node.getInLinks().values().iterator().next();
				Link outLink = node.getOutLinks().values().iterator().next();
				
				if(inLink.getFromNode().equals(outLink.getToNode())){
					
					if(node.getCoord().getX()<=bbox2.getXMax()&&node.getCoord().getX()>=bbox2.getXMin()&&
							node.getCoord().getY()<=bbox2.getYMax()&&node.getCoord().getY()>=bbox.getYMin())
						this.deadEnds.add(node);
					else
						this.exitRoads.add(node);
					
				}
				
			}
			
		}
		
		logger.info("done.");
		
		logger.warn(this.deadEnds.size() + " dead ends and " + this.exitRoads.size() + " exit roads found.");
		
		logger.info("writing node statistics files...");
		
		try {
			createNodeDegreesFiles(inDegrees, outDegrees);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		logger.info("done.");
		
	}
	
	private void createLinkLengthFiles() throws IOException {
		
		logger.info("writing length statistics file...");
		
		File file = new File(this.outputFolder+"/test/lengthStatistics.txt");
		FileWriter writer = new FileWriter(file);
		
		writer.write("\t\t<7\t<15\t<50\t<100\t<150\t<200\t<300\t<500\t>500\nnObjects");
		
		for(int j=0;j<this.lengths.length;j++){
			writer.write("\t"+this.lengths[j]);
		}
		writer.close();
		
		String[] categories = new String[9];
		categories[0]="< 7";
		categories[1]="< 15";
		categories[2]="< 50";
		categories[3]="< 100";
		categories[4]="< 150";
		categories[5]="< 200";
		categories[6]="< 300";
		categories[7]="< 500";
		categories[8]="> 500";
		
		BarChart chart = new BarChart("Link lengths", "length [m]", "number of objects",categories);
		chart.addSeries("link lengths", this.lengths);
		chart.saveAsPng(this.outputFolder+"/test/lengthStatistics.png", 800, 600);
		
	}
	
	private void createLaneStatisticsFiles() throws IOException {
		
		logger.info("writing lane statistics file...");
		
		File file = new File(this.outputFolder+"/test/laneStatistics.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("NLanes");
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
		
		logger.info("writing length comparison file...");

		File file = new File(this.outputFolder+"/test/linkLengthComparison.txt");
		FileWriter writer = new FileWriter(file);
		writer.write("Id\tlength\tgeometric Length");
		
		for(Id id : this.distances.keySet()){
			writer.write("\n"+id.toString()+"\t"+
					this.scenario.getNetwork().getLinks().get(id).getLength()+
					"\t"+this.distances.get(id));
		}
		
		writer.close();

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
		
		//dead ends, exit roads
		
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
