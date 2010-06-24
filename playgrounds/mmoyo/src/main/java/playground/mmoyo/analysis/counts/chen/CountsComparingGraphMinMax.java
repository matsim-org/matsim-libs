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

import playground.mmoyo.utils.KMZ_Extractor;

/** Joins graphs of kml files of different cost calculations in order to compare them **/

/*
 * TODO: verify that the first graph is the minimal value and the same with maximal
 * error graphs
 * optimize the 
 */
public class CountsComparingGraphMinMax {
	//assumes:
	//1-all graphs were extracted into dir "/ITERS/it.0/graphs/"
	//2.-folders of graph combination start with a 4 char prefix like "time" "sec_"  plus the value vgr "time0.0", "sec_60.0"
	double firstValue;
	double lastValue;
	private String minMaxDirPath;
	final String PNG = ".png";
	
	public void createComparingGraphs(final String runsDir, final String indVarPrefix) throws IOException{
		final String READING = "reading: ";
		final String A_SUF= "a";
		final String B_SUF= "b";
		final String O_SUF= "o";
		final String INFO = "info.txt";
		final String SVN = ".svn";
		final String OCUP_ERROR_TYPE = "errorGraphErrorBiasOccupancy";
		final String BOARD_ERROR_TYPE = "errorGraphErrorBiasBoarding";
		final String ALIGHT_ERROR_TYPE = "errorGraphErrorBiasAlighting";
		final String SEPARATOR = "/";
		final String POINT = ".";
		final String it0Dir = "/ITERS/it.0";
		final String graphsDir = "/graphs/";
		final String kmzFile = "/0.countscompare.kmz";

		File runsDirFileObj = new File(runsDir);    	
		this.minMaxDirPath = runsDirFileObj.getParent()+ SEPARATOR + "comparingMinMax/" ;
		System.out.println("dir" + runsDirFileObj.getPath());
		
		//create comparingMinMax directory
		File minMaxDirFileObj = new File (this.minMaxDirPath);
		if (!minMaxDirFileObj.exists()){minMaxDirFileObj.mkdir(); }
		minMaxDirFileObj = null;	
		
		/*
		int alightNum=0;
		int boardsNum=0;
		int ocupNum=0;
		*/

		//find all graphs and store their location in a mapped list
		Map <String, List<GraphData>> boardBufImgMap = new TreeMap <String, List<GraphData>>();
		Map <String, List<GraphData>> ocupBufImgMap = new TreeMap <String, List<GraphData>>();
		Map <String, List<GraphData>> alightBufImgMap = new TreeMap <String, List<GraphData>>();
		
		Map <String, List<GraphData>> boardErrorBufImgMap = new TreeMap <String, List<GraphData>>();
		Map <String, List<GraphData>> ocupErrorBufImgMap = new TreeMap <String, List<GraphData>>();
		Map <String, List<GraphData>> alightErrorBufImgMap = new TreeMap <String, List<GraphData>>();
		
		List<Double> variableList = new ArrayList<Double>();
		
		for (int i= 0; i< runsDirFileObj.list().length ; i++){
			System.out.println (READING + runsDirFileObj.list()[i]);
			String coeffCombination = runsDirFileObj.list()[i];
			if (!(coeffCombination.equals(SVN) || coeffCombination.equals(INFO))){

				//uncompress the counts graph from countscompare.kmz file  into the  folder of iteration 0*/
				String iter0_dir = runsDir + SEPARATOR + coeffCombination + it0Dir;
				String graphDirPath = iter0_dir +  graphsDir;
				System.out.println(graphDirPath);
				File graphDirFileObj = new File(graphDirPath); 
				graphDirFileObj.mkdir();
				KMZ_Extractor kmz_Extractor = new KMZ_Extractor(iter0_dir + kmzFile, graphDirPath);    
				kmz_Extractor.extractGraphs();		
				
				for (int ii= 0; ii< graphDirFileObj.list().length; ii++){
					String graphName= graphDirFileObj.list()[ii];
					if (!(graphName.equals(SVN) || graphName.equals(INFO))){
						String filePath = graphDirFileObj.getPath() + SEPARATOR + graphName;
						System.out.println(READING + filePath);

						//find out type (
						int sufPos = graphName.lastIndexOf(POINT)-1;
						String suffix = graphName.substring(sufPos, sufPos+1);
						if (suffix.equals(A_SUF)){
							if (!alightBufImgMap.containsKey(graphName)){
								alightBufImgMap.put(graphName,  new ArrayList<GraphData>());
							}
							alightBufImgMap.get(graphName).add(new GraphData(filePath, coeffCombination, indVarPrefix));
							//alightNum++;
						
						}else if (suffix.equals(B_SUF)){
							if (!boardBufImgMap.containsKey(graphName)){
								boardBufImgMap.put(graphName,  new ArrayList<GraphData>());
							}
							boardBufImgMap.get(graphName).add(new GraphData(filePath, coeffCombination, indVarPrefix));
							//boardsNum++;

						}else if (suffix.equals(O_SUF)){
							if (!ocupBufImgMap.containsKey(graphName)){
								ocupBufImgMap.put(graphName,  new ArrayList<GraphData>());
							}
							ocupBufImgMap.get(graphName).add(new GraphData(filePath, coeffCombination, indVarPrefix));
							//ocupNum++;
						
						}else if (graphName.equals(ALIGHT_ERROR_TYPE + PNG)){
							if (!alightErrorBufImgMap.containsKey(ALIGHT_ERROR_TYPE)){
								alightErrorBufImgMap.put(ALIGHT_ERROR_TYPE,  new ArrayList<GraphData>());
							}
							alightErrorBufImgMap.get(ALIGHT_ERROR_TYPE).add(new GraphData(filePath, coeffCombination, indVarPrefix));
							
						}else if (graphName.equals(BOARD_ERROR_TYPE + PNG)){
							if (!boardErrorBufImgMap.containsKey(BOARD_ERROR_TYPE)){
								boardErrorBufImgMap.put(BOARD_ERROR_TYPE,  new ArrayList<GraphData>());
							}
							boardErrorBufImgMap.get(BOARD_ERROR_TYPE).add(new GraphData(filePath, coeffCombination, indVarPrefix));
							
						}else if (graphName.equals(OCUP_ERROR_TYPE + PNG)){
							if (!ocupErrorBufImgMap.containsKey(OCUP_ERROR_TYPE)){
								ocupErrorBufImgMap.put(OCUP_ERROR_TYPE,  new ArrayList<GraphData>());
							}
							ocupErrorBufImgMap.get(OCUP_ERROR_TYPE).add(new GraphData(filePath, coeffCombination, indVarPrefix));
						}
							
						variableList.add(Double.parseDouble(coeffCombination.substring(4)));
					}
				}
			}
		}
		
		//find out min and max coeff value
		Collections.sort(variableList);
		firstValue= variableList.get(0).doubleValue();
		lastValue= variableList.get(variableList.size()-1).doubleValue();
		
		createGraph(" Board", boardBufImgMap, indVarPrefix);
		createGraph(" Alight", alightBufImgMap, indVarPrefix);
		createGraph(" Occup", ocupBufImgMap, indVarPrefix);
		
		createGraph(BOARD_ERROR_TYPE, boardErrorBufImgMap, indVarPrefix);		
		createGraph(OCUP_ERROR_TYPE, ocupErrorBufImgMap, indVarPrefix);
		createGraph(ALIGHT_ERROR_TYPE, alightErrorBufImgMap, indVarPrefix);
	}
	
	private void createGraph(String type, Map <String, List<GraphData>> bufImgMap, String indVarPrefix) throws IOException{
		int posY=0;
		boolean first=true;
		
		BufferedImage buffImage = new BufferedImage( 400+400, (bufImgMap.size()*300) + 15, BufferedImage.TYPE_INT_RGB); 
		
		Graphics graphics = buffImage.getGraphics();

		//for(Map.Entry <String,List<GraphData>> entry: bufImgMap.entrySet()){
		for (List<GraphData> graphPathList : bufImgMap.values()) {
			///String key = entry.getKey(); 
			///List<GraphData> graphPathList = entry.getValue();
			///Collections.sort(graphPathList);
			//GraphData firstGraph = graphPathList.get(0);
			//GraphData lastGraph = graphPathList.get(graphPathList.size()-1);
			
			if (first){
				BufferedImage head1= createHeader(indVarPrefix + firstValue + " "  + type);
				BufferedImage head2= createHeader(indVarPrefix + lastValue + " "  + type);
				graphics.drawImage(head1, 0, 0, null);
				graphics.drawImage(head2, 400, 0, null);
				posY= 15;
				first=false;
			}
			
			BufferedImage firstGraphImg = ImageIO.read(new File(graphPathList.get(0).getFilePath()));
			BufferedImage lastGraphImg = ImageIO.read(new File(graphPathList.get(graphPathList.size()-1).getFilePath()));
			
			graphics.drawImage(firstGraphImg, 0, posY, null);
			graphics.drawImage(lastGraphImg, 400, posY, null);
			posY +=300;
		}	
		graphics.dispose();
		File outputFile1 = new File(this.minMaxDirPath + type + PNG);
		ImageIO.write(buffImage,"png", outputFile1);		
	}
	
	private BufferedImage createHeader(String title){  //title = coeffCombination + type
		BufferedImage headImage = new BufferedImage(400, 25, BufferedImage.TYPE_INT_RGB);
		Graphics headerGraphic = headImage.getGraphics();
		headerGraphic.setColor(Color.white);
		headerGraphic.fillRect(0,0,400,15);
		headerGraphic.setColor(Color.black);
		headerGraphic.drawString(title, 180, 10);
		headerGraphic.dispose();
		return headImage;
	}
	
	public static void main(String[] args) {
		String runsDir;
		String indVarPrefix;
		
		if (args.length==2){
			runsDir = args[0];
			indVarPrefix = args[1];
		}else{
			//runsDir = "../playgrounds/mmoyo/output/@prueba/output";
			runsDir = "../playgrounds/mmoyo/output/dist50/runs";
			indVarPrefix =  "dist"; 
		}
		
		try {
			new CountsComparingGraphMinMax().createComparingGraphs(runsDir, indVarPrefix);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}