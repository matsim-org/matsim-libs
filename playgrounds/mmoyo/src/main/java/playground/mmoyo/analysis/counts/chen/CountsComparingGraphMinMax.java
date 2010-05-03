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

/*
 * TODO: verify that the first graph is the minimal value and the same with maximal
 * error graphs
 * optimize the 
 */

public class CountsComparingGraphMinMax {
	final String SEPARATOR = "/";
	final String POINT = ".";
	final String TYPE_ALIGHT = " Alight";
	final String TYPE_BOARD = " Board";
	final String TYPE_OCUPP = " Occup";
	final String READING = "reading: ";
	final String CREATING = "creating graph: ";
	final String A_SUF= "a";
	final String B_SUF= "b";
	final String O_SUF= "o";
	final String SVN = ".svn";
	final String INFO = "info.txt";
	final String OCUP_ERROR_TYPE = "errorGraphErrorBiasOccupancy";
	final String BOARD_ERROR_TYPE = "errorGraphErrorBiasBoarding";
	final String ALIGHT_ERROR_TYPE = "errorGraphErrorBiasAlighting";
	
	//assumes:
	//1-all graphs were extracted into dir "/ITERS/it.0/graphs/"
	//2.-folders of graph combination start with a 4 char prefix like "time" "sec_"  plus the value vgr "time0.0", "sec_60.0"
	final String VARIABLE = "sec_";
	double firstValue;
	double lastValue;
	
	private void createComparingGraphs(final String dirPath) throws IOException{
		File dir = new File(dirPath);    	
		System.out.println("dir" + dir.getPath());
		String ITERPATH = "/ITERS/it.0/graphs/";   

		/*
		int alightNum=0;
		int boardsNum=0;
		int ocupNum=0;
		*/
		
		//find all graphs and store their location in a mapped list
		//Map <String, List<GraphData>> bufImgMap = new TreeMap <String, List<GraphData>>();
		
		Map <String, List<GraphData>> boardBufImgMap = new TreeMap <String, List<GraphData>>();
		Map <String, List<GraphData>> ocupBufImgMap = new TreeMap <String, List<GraphData>>();
		Map <String, List<GraphData>> alightBufImgMap = new TreeMap <String, List<GraphData>>();
		
		Map <String, List<GraphData>> boardErrorBufImgMap = new TreeMap <String, List<GraphData>>();
		Map <String, List<GraphData>> ocupErrorBufImgMap = new TreeMap <String, List<GraphData>>();
		Map <String, List<GraphData>> alightErrorBufImgMap = new TreeMap <String, List<GraphData>>();
		
		List<Double> variableList = new ArrayList<Double>();
		
		for (int i= 0; i< dir.list().length ; i++){
			System.out.println (READING + dir.list()[i]);
			String coeffCombination = dir.list()[i];
			if (!(coeffCombination.equals(SVN) || coeffCombination.equals(INFO))){
				File graphDir1 = new File(dirPath + SEPARATOR +  coeffCombination + ITERPATH);   ///graphs folder
				for (int ii= 0; ii< graphDir1.list().length; ii++){
					String graphName= graphDir1.list()[ii];
					if (!(graphName.equals(SVN) || graphName.equals(INFO))){
						String filePath = graphDir1 + SEPARATOR + graphName;
						System.out.println(READING + filePath);

						//find out type (
						int sufPos = graphName.lastIndexOf(POINT)-1;
						String suffix = graphName.substring(sufPos, sufPos+1);
						if (suffix.equals(A_SUF)){
							if (!alightBufImgMap.containsKey(graphName)){
								alightBufImgMap.put(graphName,  new ArrayList<GraphData>());
							}
							alightBufImgMap.get(graphName).add(new GraphData(filePath, coeffCombination));
							//alightNum++;
						
						}else if (suffix.equals(B_SUF)){
							if (!boardBufImgMap.containsKey(graphName)){
								boardBufImgMap.put(graphName,  new ArrayList<GraphData>());
							}
							boardBufImgMap.get(graphName).add(new GraphData(filePath, coeffCombination));
							//boardsNum++;

						}else if (suffix.equals(O_SUF)){
							if (!ocupBufImgMap.containsKey(graphName)){
								ocupBufImgMap.put(graphName,  new ArrayList<GraphData>());
							}
							ocupBufImgMap.get(graphName).add(new GraphData(filePath, coeffCombination));
							//ocupNum++;
						
						}else if (graphName.equals(ALIGHT_ERROR_TYPE + ".png")){
							if (!alightErrorBufImgMap.containsKey(ALIGHT_ERROR_TYPE)){
								alightErrorBufImgMap.put(ALIGHT_ERROR_TYPE,  new ArrayList<GraphData>());
							}
							alightErrorBufImgMap.get(ALIGHT_ERROR_TYPE).add(new GraphData(filePath, coeffCombination));
							
						}else if (graphName.equals(BOARD_ERROR_TYPE + ".png")){
							if (!boardErrorBufImgMap.containsKey(BOARD_ERROR_TYPE)){
								boardErrorBufImgMap.put(BOARD_ERROR_TYPE,  new ArrayList<GraphData>());
							}
							boardErrorBufImgMap.get(BOARD_ERROR_TYPE).add(new GraphData(filePath, coeffCombination));
							
						}else if (graphName.equals(OCUP_ERROR_TYPE + ".png")){
							if (!ocupErrorBufImgMap.containsKey(OCUP_ERROR_TYPE)){
								ocupErrorBufImgMap.put(OCUP_ERROR_TYPE,  new ArrayList<GraphData>());
							}
							ocupErrorBufImgMap.get(OCUP_ERROR_TYPE).add(new GraphData(filePath, coeffCombination));
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
		
		createGraph(TYPE_BOARD, boardBufImgMap);
		createGraph(TYPE_ALIGHT, alightBufImgMap);
		createGraph(TYPE_OCUPP, ocupBufImgMap);
		
		createGraph(BOARD_ERROR_TYPE, boardErrorBufImgMap);		
		createGraph(OCUP_ERROR_TYPE, ocupErrorBufImgMap);
		createGraph(ALIGHT_ERROR_TYPE, alightErrorBufImgMap);
	}
	
	private void createGraph(String type, Map <String, List<GraphData>> bufImgMap) throws IOException{
		int posY=0;
		boolean first=true;
		
		BufferedImage buffImage = new BufferedImage( 400+400, (bufImgMap.size()*300) + 15, BufferedImage.TYPE_INT_RGB); 
		
		Graphics graphics = buffImage.getGraphics();
		for(Map.Entry <String,List<GraphData>> entry: bufImgMap.entrySet()){
			String key = entry.getKey(); 
			List<GraphData> graphPathList = entry.getValue();
			Collections.sort(graphPathList);
			//GraphData firstGraph = graphPathList.get(0);
			//GraphData lastGraph = graphPathList.get(graphPathList.size()-1);
			
			if (first){
				BufferedImage head1= createHeader(VARIABLE + firstValue + " "  + type);
				BufferedImage head2= createHeader(VARIABLE + lastValue + " "  + type);
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
		File outputFile1 = new File("../playgrounds/mmoyo/output/fifth/prueba/" + type + ".png");
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
	
	class GraphData implements Comparable<GraphData>{
		String coeffCombination;
		String filePath;
		double variableValue;
		
		GraphData(String filePath, String coeffCombination){
			this.filePath =  filePath;
			this.coeffCombination = coeffCombination;
	
			if (coeffCombination.startsWith(VARIABLE)){ //make sure that the time combination starts with a prefix
				variableValue = Double.parseDouble(coeffCombination.substring(4));
			}
		}

		private String getCoeffCombination() {
			return coeffCombination;
		}

		private String getFilePath() {
			return filePath;
		}

		private double getTimePriority(){
			return this.variableValue;	
		}
		
		@Override
		public int compareTo(GraphData otherGraphData) {
		    return Double.compare(variableValue, otherGraphData.getTimePriority());
		}
	}
	
	public static void main(String[] args) {
		//String dir = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/KMZ_counts_scalefactor50";
		String dir = "../playgrounds/mmoyo/output/fifth/output";
		try {
			new CountsComparingGraphMinMax().createComparingGraphs(dir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}



	

