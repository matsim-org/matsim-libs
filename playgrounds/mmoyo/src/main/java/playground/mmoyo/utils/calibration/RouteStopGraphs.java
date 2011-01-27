package playground.mmoyo.utils.calibration;

import java.awt.Color;
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

import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;

/**Reads a kmz file and creates the sequence of occupancy error graphs for stops of a given transit Route*/
public class RouteStopGraphs {
	private ZipFile zipFile;
	private TransitSchedule schedule;
	private final Counts counts;
	private final String outputDir;
	
	public RouteStopGraphs(final String kmzFile, final TransitSchedule schedule, final Counts counts){
		this.schedule= schedule;
		this.counts = counts;
		
		//set zipFile and output dir
		try {
			this.zipFile = new ZipFile(kmzFile);
		} 
		catch(IOException e) {
			e.printStackTrace();
		}
		
		File outDirFile = new File(kmzFile);
		this.outputDir = outDirFile.getParent() + outDirFile.separatorChar;
		outDirFile = null;
	}
	
	private void createRouteGraph(final String strRouteId) throws IOException{
		TransitRoute route = new DataLoader().getTransitRoute(strRouteId, this.schedule);
		List<String> stopids = new ArrayList<String>();
		for (TransitRouteStop stop: route.getStops()){
			stopids.add( stop.getStopFacility().getId().toString());
		}
		
		BufferedImage bufferedImage = new BufferedImage(400*stopids.size(), 350 , BufferedImage.TYPE_INT_RGB);
		Graphics graphics = bufferedImage.getGraphics();
		String png = ".png";

		char type = 'o';   //we want to get only occupancy files
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
	        	BufferedImage errorGraphImg = ImageIO.read(file);
	        	graphics.drawImage(errorGraphImg, 400*(i), 0, null); //400 is normal graph height, 100 for csid, 100 for coorX, 100 for coorY

	        	//set csId and coord below
	    		Count count = this.counts.getCounts().get(new IdImpl(strStopId));
	    		String cdId= count.getCsId();
	    		Coord coord = count.getCoord();

	    		Graphics infoGraphImg = bufferedImage.getGraphics();
				infoGraphImg.setColor(Color.white);
				infoGraphImg.fillRect(400*(i),300,400,50);
				infoGraphImg.setColor(Color.black);
				infoGraphImg.drawString(cdId, (400*i)+ 50, 320);
				infoGraphImg.drawString(coord.toString(), 400*(i) + 50, 340);
				infoGraphImg.dispose();
			}
			i++;
		}
		graphics.dispose();

		File outputFile = new File(this.outputDir + strRouteId + type + png);
		ImageIO.write(bufferedImage,"png", outputFile);
		System.out.print("done.");
	}
	
	public static void main(String[] args) {
		final String scheduleFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		final String networkFile = "../shared-svn/studies/countries/de/berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		final String occupCountFile = "../shared-svn/studies/countries/de/berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44_344.xml";
		final String kmzFile = "../playgrounds/mmoyo/output/routeAnalysis/scoreOff/TransMOD/500.ptBseCountscompare.kmz";
		
		DataLoader dataLoader = new DataLoader();
		TransitSchedule schedule = dataLoader.readTransitSchedule(networkFile, scheduleFile);
		Counts counts = dataLoader.readCounts(occupCountFile);
		RouteStopGraphs routeStopGraphs = new RouteStopGraphs(kmzFile, schedule, counts);
		
		//make bus line M44 routes
		try {
			routeStopGraphs.createRouteGraph("B-M44.102.901.H");
			routeStopGraphs.createRouteGraph("B-M44.102.901.R"); 
			routeStopGraphs.createRouteGraph("B-M44.101.901.H"); 
			routeStopGraphs.createRouteGraph("B-M44.101.901.R"); 
		} catch (IOException e) {
			e.printStackTrace();
		}    
	}

}
