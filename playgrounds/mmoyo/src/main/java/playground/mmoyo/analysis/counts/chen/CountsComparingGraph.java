package playground.mmoyo.analysis.counts.chen;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;

import playground.mmoyo.utils.KMZ_Extractor;

/** Joins graphs of kml files of different cost calculations in order to compare them **/
public class CountsComparingGraph {
	
	public void createComparingGraphs(final String dirPath, final String indVarPrefix) throws IOException{
		File graphsDir = new File(dirPath);    	
		if (!graphsDir.exists()){ 
			throw new FileNotFoundException("Can not find: " + dirPath);
		}
		
		System.out.println("dir" + graphsDir.getPath());

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
		String ITERPATH = "/ITERS/it.0/graphs/"; 
		
		//uncompress the counts graph from countscompare.kmz file  into the  folder of iteration 0*/
		for (int i= 0; i< graphsDir.list().length ; i++){
			String iter0_dir = dirPath + SEPARATOR + graphsDir.list()[i] +  "/ITERS/it.0/";
			(new File(dirPath + SEPARATOR + graphsDir.list()[i] +  ITERPATH)).mkdir();
		    new KMZ_Extractor(iter0_dir + "0.countscompare.kmz", iter0_dir + "graphs/").extractGraphs();    
		}
		
		//create comparingGraphs dir
		String comparingGraphDir = graphsDir.getParent() + "/comparingGraphs/";
		(new File(comparingGraphDir)).mkdir();
		
		Map <String, List<GraphData>> bufImgMap = new TreeMap <String, List<GraphData>>();
		for (int i= 0; i< graphsDir.list().length ; i++){
			String coeffCombination = graphsDir.list()[i];
			System.out.println (READING + coeffCombination);
			if (!(coeffCombination.equals(SVN) || coeffCombination.equals(INFO))){
				File graphDir1 = new File(dirPath + SEPARATOR +  coeffCombination + ITERPATH);   ///graphs folder
				
				//find all graphs and store their location in a mapped list
				for (int ii= 0; ii< graphDir1.list().length; ii++){
					String graphName= graphDir1.list()[ii];
					if (!(graphName.equals(SVN) || graphName.equals(INFO))){
						String filePath = graphDir1 + SEPARATOR + graphName;
						System.out.println(READING + filePath);
						if (!bufImgMap.containsKey(graphName)){
							bufImgMap.put(graphName,  new ArrayList<GraphData>());
						}
						bufImgMap.get(graphName).add(new GraphData(filePath, coeffCombination, indVarPrefix));
					}
				}
				//////////////////////////////////////////////////////////
			}
		}
		
		//create the comparing graph
		for(Map.Entry <String,List<GraphData>> entry: bufImgMap.entrySet() ){
			String key = entry.getKey(); 
			List<GraphData> graphPathList = entry.getValue();

			System.out.println(CREATING + key);

			//find out type (
			String type = "";
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
			File outputFile = new File(comparingGraphDir + key);
			ImageIO.write(compImg,"png", outputFile);	
			
		}
		
		//delete temporal graphs files
		String[] strGraphsDirs = graphsDir.list();
		for (int i= 0; i< strGraphsDirs.length ; i++){
			String graphPath = dirPath + SEPARATOR + strGraphsDirs[i] +  ITERPATH;
			File fileGraphDir = new File(graphPath);
			
			//delete graphs inside
			String[] graphs = fileGraphDir.list();
			for (int j= 0; j< graphs.length ; j++){
				File fileGraph = new File(graphPath + graphs[j]);
				//System.out.println(fileGraph.getPath()+ " " + fileGraphDir.exists());
				System.out.println("deleted " +  fileGraph.getPath() + " " + fileGraph.delete());
			}
			fileGraphDir.delete();
		}/////////////////////////////////////
		
		System.out.println ("done.");
	}
	
	public static void main(String[] args) {
		String runsDir;
		String indVarName;

		if (args.length==2){
			runsDir = args[0];
			indVarName = args[1];
		}else{
			//runsDir = "../playgrounds/mmoyo/output/@prueba/config_20plans.xml";
			runsDir = "../playgrounds/mmoyo/output/dist50/runs";
			indVarName =  "dist"; 
		}
		
		try {
			new CountsComparingGraph().createComparingGraphs(runsDir, indVarName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}