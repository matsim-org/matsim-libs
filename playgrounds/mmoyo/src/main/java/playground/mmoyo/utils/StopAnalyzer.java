package playground.mmoyo.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.imageio.ImageIO;
import org.apache.log4j.Logger;

/** 
 * Creates a sequence of graphs of a stop showing its evolution through iterations
 */
public class StopAnalyzer {
	private String iterDir;
	private final String STR_ZERO = "0";
	private static final Logger log = Logger.getLogger(StopAnalyzer.class);
	
	public StopAnalyzer(final String iterDir){
		this.iterDir = iterDir;
	}
	
	public void createSequence (final String kmzFileName, final String graphName , final int maxGraphsNum) throws IOException{
		
		File dir = new File(iterDir); 
		if (!dir.exists()){
			throw new FileNotFoundException("The iterations directory does not exists: " + iterDir);
		}
		String[] children = dir.list(new IterarionFileFilter()); //list only iteration ending with "0" of occupancy

		//sort the children numeric ascending 
		List<Integer> iterNumList = new ArrayList<Integer>();
		final String point = ".";
		for (int i=0; i<children.length ; i++){
			String s = children[i];
			int numIt=  Integer.valueOf (s.substring(s.indexOf(point)+1, s.length()));
			iterNumList.add(numIt);
		}
		Collections.sort(iterNumList);
		
		//reduce the size of the list to have only maxGraphsNum
		if (maxGraphsNum>0){
			while (iterNumList.size() > maxGraphsNum){
				iterNumList.remove(iterNumList.size()-1);
			}
		}
		
		//look for kmz files in all iteration folders
		final String slash = "/";
		final String it = "it.";
		BufferedImage bufferedImage = new BufferedImage(400* iterNumList.size(), 300 , BufferedImage.TYPE_INT_RGB);
		Graphics graphics = bufferedImage.getGraphics();		

		int i=0;
		for (int itNum : iterNumList){
			//get station graph file from each kmzFile
			String kmzFilePath = this.iterDir + it + itNum + slash + itNum + point + kmzFileName;
			log.info(kmzFilePath);
			ZipFile zipFile = new ZipFile(kmzFilePath);
			ZipEntry zipEntry = zipFile.getEntry(graphName);
			
			if (zipEntry!=null){   
				//read zip file
	    		File file = new File(graphName);
	    		InputStream inStream = zipFile.getInputStream(zipEntry);
	        	OutputStream outStream = new BufferedOutputStream(new FileOutputStream(file));
	        	byte[] buffer = new byte[1024];
	        	int len;
	        	while((len = inStream.read(buffer)) >= 0){
	        		outStream.write(buffer, 0, len);
	        	}
	        	inStream.close();
	        	outStream.close();
			
	        	//create graph
	        	BufferedImage errorGraphImg = ImageIO.read(file);
	        	graphics.drawImage(errorGraphImg, 400*(i++), 0, null); //400 is normal graph height, 100 for csid, 100 for coorX, 100 for coorY
			}
		}
		graphics.dispose();

		File outputGraphFile = new File(this.iterDir + graphName);
		ImageIO.write(bufferedImage,"png", outputGraphFile);
		System.out.println("graph series was written at: " + outputGraphFile.getCanonicalPath());
	}
	
	class IterarionFileFilter implements FilenameFilter {
	    public boolean accept(File dir, String name) {
	        return (name.endsWith(STR_ZERO));
	    }
	}
	
	/** This does not create a sequence graph, it just extract the same image of all iterations into a to a given output directory */
	private void ExtractGraphs(final String imageName, final String outputDir){
		File dir = new File(this.iterDir); 
		String kmzPath = "/ITERS/it.10/10.ptcountscompare.kmz";
		final String STR_PNG = ".png";
		for (String combName : dir.list()){
			String kmzFilePath = this.iterDir + combName + kmzPath;
			try{
				byte[] buffer = new byte[1024];
		        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(kmzFilePath));
		        ZipEntry zipEntry = zipInputStream.getNextEntry();
		        while (zipEntry != null) {
		        	if (zipEntry.getName().equals(imageName)){
		        		System.out.println("extracting: " + combName );		            		
		            
		        		FileOutputStream fileoutputstream;
			            fileoutputstream = new FileOutputStream(outputDir + combName + STR_PNG);
			            int i;
			            while ((i = zipInputStream.read(buffer, 0, 1024)) > -1){
			            	fileoutputstream.write(buffer, 0, i);
			            }
			            fileoutputstream.close(); 
			            zipInputStream.closeEntry();
		        	}
		        	zipEntry = zipInputStream.getNextEntry();
		        }
				zipInputStream.close();
			}catch (Exception e){
				e.printStackTrace();
		    }
		}

	}
	
	public static void main(String[] args) throws IOException {
		String itDir= null;
		int maxGraphsNum = 0;     //zero if all plots are desired for analysis. Otherwise, the maximum number must be set.
		String graphName = null;
		String kmzFileName = null;
		
		if (args.length>0){
			itDir = args[0];
			maxGraphsNum = Integer.valueOf (args[1]);
			graphName = args[2];
			kmzFileName = args[3];
		}else{
			itDir = "../../input/newDemand/apr2012/cal24/output/ITERS/";
			maxGraphsNum = 0;
			graphName = "Occupancy-countsSimRealPerHour_1.png";  //"errorGraphErrorBiasOccupancy.png";
			kmzFileName = "cadytsPtCountscompare.kmz";
		}
		new StopAnalyzer(itDir).createSequence(kmzFileName, graphName , maxGraphsNum );
		//new StopAnalyzer(itDir).ExtractGraphs(graphName, outDir);
	}
}
