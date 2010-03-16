package playground.mmoyo.analysis.counts.chen;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

/** Joins graphs of kml files of different cost calculations in order to compare them **/
public class CountsComparingGraph2 {
	String dirPath;
	String SEPARATOR = "/";
	String POINT = ".";
	String TYPE_ALIGHT = " Alight";
	String TYPE_BOARD = " Board";
	String TYPE_OCUPP = " Occup";
	String READING = "reading: ";
	String CREATING = "creating graph: ";
	String A_SUF= "a";
	String B_SUF= "b";
	String C_SUF= "o";
	String SVN = ".svn";
	String INFO = "info.txt";
	
	public CountsComparingGraph2(String dirPath) {
		this.dirPath = dirPath;
	}

	private void createComparingGraphs() throws IOException{
		File dir = new File(this.dirPath);    	

		//assuming the iteration 0  and that the graphs inside the kml file were extracted at this directory: 
		String ITERPATH = "/ITERS/it.0/graphs/";   
		
		//find all graphs and store their location in a mapped list
		Map <String, List<GraphData>> bufImgMap = new TreeMap <String, List<GraphData>>();
		for (int i= 0; i< dir.list().length ; i++){
			System.out.println (READING + dir.list()[i]);
			String coeffCombination = dir.list()[i];
			if (!(coeffCombination.equals(SVN) || coeffCombination.equals(INFO))){
				File graphDir1 = new File(this.dirPath + SEPARATOR +  coeffCombination + ITERPATH);   ///graphs folder
				for (int ii= 0; ii< graphDir1.list().length; ii++){
					String graphName= graphDir1.list()[ii];
					if (!(graphName.equals(SVN) || graphName.equals(INFO))){
						String filePath = graphDir1 + SEPARATOR + graphName;
						System.out.println(READING + filePath);
						if (!bufImgMap.containsKey(graphName)){
							bufImgMap.put(graphName,  new ArrayList<GraphData>());
						}
						bufImgMap.get(graphName).add(new GraphData(filePath, coeffCombination));
					}
				}
			}
		}
		
		
		//create the comparing graph
		for(Map.Entry <String,List<GraphData>> entry: bufImgMap.entrySet() ){
			String key = entry.getKey(); 
			List<GraphData> graphPathList = entry.getValue();

			System.out.println(CREATING + key);

			//find out type (
			String type = null;
			int sufPos = key.lastIndexOf(POINT)-1;
			String suffix = key.substring(sufPos, sufPos+1);
			if (suffix.equals(A_SUF)){
				type = TYPE_ALIGHT;
			}else if (suffix.equals(B_SUF)){
				type = TYPE_BOARD;
			}else if (suffix.equals(C_SUF)){
				type=  TYPE_OCUPP;
			}
			
			BufferedImage compImg = new BufferedImage(400 * graphPathList.size(), 315, BufferedImage.TYPE_INT_RGB); 
			Graphics compGraphic = compImg.getGraphics();
			
			//sort by time priority
			Collections.sort(graphPathList);

			int posx=0;
			for (GraphData graphData : graphPathList){
				
				//read graph file
				File graphFile = new File(graphData.getFilePath());
				String coeffCombination = graphData.getCoeffCombination();
				
				//create head   ->create only 21x3 first? 
				
				BufferedImage headImage = new BufferedImage(400, 25, BufferedImage.TYPE_INT_RGB);
				Graphics headerGraphic = headImage.getGraphics();
				headerGraphic.setColor(Color.white);
				headerGraphic.fillRect(0,0,400,15);
				headerGraphic.setColor(Color.black);
				headerGraphic.drawString(coeffCombination + type, 180, 10);
				headerGraphic.dispose();
				
				//create image with head 	400=width    300= height   15 head height 
				BufferedImage graphImg = ImageIO.read(graphFile);
				BufferedImage graphWithHead = new BufferedImage(400, 300 + 15 , BufferedImage.TYPE_INT_RGB);
				Graphics g = graphWithHead.getGraphics();
				g.drawImage(headImage,0,0, null);
				g.drawImage(graphImg,0,15, null);
				g.dispose();  
		
				compGraphic.drawImage(graphWithHead, posx, 0, null);
				posx += 400;
			}
			compGraphic.dispose();
			File outputFile = new File("../playgrounds/mmoyo/output/" + key);
			ImageIO.write(compImg,"png", outputFile);	
		}
	
		System.out.println ("done.");
	}
	
	class GraphData implements Comparable<GraphData>{
		String coeffCombination;
		String filePath;
		double timePriority;
		
		GraphData(String filePath, String coeffCombination){
			this.filePath =  filePath;
			this.coeffCombination = coeffCombination;
	
			if (coeffCombination.startsWith("time")){ //make sure that the time combination starts with "time"
				timePriority = Double.parseDouble(coeffCombination.substring(4));
			}
		}

		private String getCoeffCombination() {
			return coeffCombination;
		}

		private String getFilePath() {
			return filePath;
		}

		private double getTimePriotity(){
			return this.timePriority;	
		}
		
		@Override
		public int compareTo(GraphData otherGraphData) {
		    return Double.compare(timePriority, otherGraphData.getTimePriotity());
		}
	}
	
	public static void main(String[] args) {
		//String dir = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/KMZ_counts_scalefactor50";
		String dir = "../playgrounds/mmoyo/output/outputfirst";
		try {
			new CountsComparingGraph2(dir).createComparingGraphs();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}



	

