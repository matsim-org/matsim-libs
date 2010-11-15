package playground.mmoyo.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
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

import javax.imageio.ImageIO;

/**Creates a sequence of graphs of a stop showing its evolution through iterations*/
public class StopAnalyzer {
	private String iterDir;
	private final String zero = "0";
	
	public StopAnalyzer(final String iterDir){
		this.iterDir = iterDir;
	}
	
	public void createSequence (final String errorGraphName , final int maxGraphsNum) throws IOException{
		
		File dir = new File(this.iterDir); 
		String[] children = dir.list(new IterarionFileFilter()); //list only iteration ending with "0" of occupancy

		//sort the children numeric ascending 
		List<Integer> iterNumList = new ArrayList<Integer>();
		final String point = ".";
		for (int i=0; i<children.length ; i++){
			String s = children[i];
			int numIt=  Integer.valueOf (s.substring(s.indexOf(point)+1, s.length()));
			if (numIt!=0){	 //iteration zero does not have kmz file
				iterNumList.add(numIt);
			}
		}
		Collections.sort(iterNumList);
		
		//reduce the size of the list to have only maxGraphsNum
		while (iterNumList.size() > maxGraphsNum){
			iterNumList.remove(iterNumList.size()-1);
		}
		
		//look for kmz files in all iteration folders
		final String countscompare = ".ptcountscompare.kmz";
		final String slash = "/";
		final String stopFileName = errorGraphName;
		final String it = "it.";
		BufferedImage bufferedImage = new BufferedImage(400* iterNumList.size(), 300 , BufferedImage.TYPE_INT_RGB);
		Graphics graphics = bufferedImage.getGraphics();		

		int i=0;
		for (int itNum : iterNumList){
			//get station graph file from each kmzFile
			String kmzFilePath = this.iterDir + it + itNum + slash + itNum + countscompare;
			ZipFile zipFile = new ZipFile(kmzFilePath);
			ZipEntry zipEntry = zipFile.getEntry(stopFileName);
			
			if (zipEntry!=null){   
				//read zip file
	    		File file = new File(stopFileName);
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

		File outputGraphFile = new File(this.iterDir + stopFileName);
		ImageIO.write(bufferedImage,"png", outputGraphFile);
	}
	
	class IterarionFileFilter implements FilenameFilter {
	    public boolean accept(File dir, String name) {
	        return (name.endsWith(zero));
	    }
	}
	
	public static void main(String[] args) throws IOException {
		String itDir = "../ptManuel/cad_minTransRoutes/output/ITERS/";
		int maxGraphsNum = 9;
		String graphName = "812030.1o.png"; //"errorGraphErrorBiasOccupancy.png";  //"812550.1o.png";  //"errorGraphErrorBiasOccupancy.png"
		
		new StopAnalyzer(itDir).createSequence(graphName , maxGraphsNum );
	}
}
