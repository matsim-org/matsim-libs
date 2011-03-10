package playground.mmoyo.utils;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
		/*
		
		//get file 
		String kmzFile = tempOutDir + STR_COUNTPATH ;
		KMZ_Extractor kmzExtractor = new KMZ_Extractor(kmzFile, strGraphDir);
		kmzExtractor.extractFile(STR_ERRGRAPHFILE);
	
		//rename it with the combination name
		File file = new File(strGraphDir + STR_ERRGRAPHFILE);
		File file2 = new File(strGraphDir + strCombination + STR_PNG);
		if (!file.renameTo(file2)) {
		*/
	}
	
	
	public static void main(String[] args) throws IOException {
		String itDir = "I:/z_Runs/";
		int maxGraphsNum = 9;
		String graphName = "812550.1o.png"; //"812030.1o.png"; //"errorGraphErrorBiasOccupancy.png";  //"812550.1o.png";  //"errorGraphErrorBiasOccupancy.png"
		String outDir = "C:/Users/omicron/Desktop/graphsBerlin5xb/"; 
		
		//new StopAnalyzer(itDir).createSequence(graphName , maxGraphsNum );
		new StopAnalyzer(itDir).ExtractGraphs(graphName, outDir);
	}
}
