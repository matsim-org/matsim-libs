package playground.mmoyo.analysis.tools;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;

public class RouteStopGraphs {
	private ZipFile zipFile;
	private TransitSchedule schedule;
	
	public RouteStopGraphs(final ZipFile kmzFile, final TransitSchedule schedule){
		this.schedule= schedule;
		this.zipFile = kmzFile;
	}
	
	public void createRouteGraph(final String strRouteId) throws IOException{
		TransitRoute route = new DataLoader().getTransitRoute(strRouteId, this.schedule);
		List<String> stopids = new ArrayList<String>();
		for (TransitRouteStop stop: route.getStops()){
			stopids.add( stop.getStopFacility().getId().toString());
		}
		//createGraph(routeId.toString(), stopids, 'a');
		//createGraph(strRouteId.toString(), stopids, 'b');
		createGraph(strRouteId.toString(), stopids, 'o');
	}
	
	private void createGraph(final String routeId, final List<String> stopids, final char type) throws IOException{
		BufferedImage bufferedImage = new BufferedImage(400*stopids.size(), 300 , BufferedImage.TYPE_INT_RGB);
		Graphics graphics = bufferedImage.getGraphics();
		String png = ".png";

		//read counts data only to get the 
		
		
		
		int i=0;
		for (String strStopId: stopids){
			String fileName = strStopId + type + png;	
			ZipEntry zipEntry = this.zipFile.getEntry(fileName);
			if (zipEntry!=null){   
				//read zip file
	    		File file = new File(fileName);
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
	        	BufferedImage graphImg = ImageIO.read(file);
	        	graphics.drawImage(graphImg, 400*(i), 0, null);
			}
			i++;
		}
		graphics.dispose();

		String outputDir = "../playgrounds/mmoyo/output/routeAnalysis/";
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists()){
			outputDirFile.mkdir();
		}
		
		File outputFile = new File(outputDir + routeId + type + png);
		ImageIO.write(bufferedImage,"png", outputFile);
	}
	
	public static void main(String[] args) {
		final String scheduleFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		final String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		final String kmzFile = "../playgrounds/mmoyo/output/cadytsOriginal/ITERS/it.10/10.countscompare.kmz";

		TransitSchedule schedule = new DataLoader().readTransitSchedule(networkFile, scheduleFile);
		try {
			ZipFile zipFile = new ZipFile(kmzFile);
			RouteStopGraphs routeStopGraphs = new RouteStopGraphs(zipFile, schedule);
			//bus line M44
			routeStopGraphs.createRouteGraph("B-M44.102.901.H");    
			routeStopGraphs.createRouteGraph("B-M44.102.901.R"); 
			routeStopGraphs.createRouteGraph("B-M44.101.901.H"); 
			routeStopGraphs.createRouteGraph("B-M44.101.901.R"); 
			//bus line 344
			routeStopGraphs.createRouteGraph("B-344.101.901.H"); 
			routeStopGraphs.createRouteGraph("B-344.101.901.R"); 
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
