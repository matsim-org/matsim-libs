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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.utils.DataLoader;



/**Reads a kmz file and creates the sequence of occupancy error graphs for stops of a given transit Route*/
public class RouteStopGraphs2 {
	private ZipFile zipFile;
	private TransitSchedule schedule;
	private final Counts counts;
	private final String outputDir;
	private final String str_itNum;
	
	public RouteStopGraphs2(final String kmzFilePath, final TransitSchedule schedule, final Counts counts){
		this.schedule= schedule;
		this.counts = counts;
		
		//set zipFile and output dir
		try {
			this.zipFile = new ZipFile(kmzFilePath);
		} 
		catch(IOException e) {
			e.printStackTrace();
		}
		
		File kmzFile = new File(kmzFilePath);
		this.outputDir = kmzFile.getParent() + kmzFile.separatorChar;
		this.str_itNum= "it" + kmzFile.getName().substring(0, kmzFile.getName().indexOf(".")) + "_"; 
		kmzFile = null;
	}
	
	private void createRouteGraph(final String strRouteId) throws IOException{
		TransitRoute route = new DataLoader().getTransitRoute(strRouteId, this.schedule);
		BufferedImage bufferedImage = new BufferedImage(400* (route.getStops().size()), 350 , BufferedImage.TYPE_INT_RGB);
		Graphics graphics = bufferedImage.getGraphics();
    	graphics.setColor(Color.white);
		final String png = ".png";
		final char type = 'o';   //we want to get only occupancy files

		for (int i=0; i<route.getStops().size()-1; i++ ){
			String strStopId = route.getStops().get(i).getStopFacility().getId().toString(); 
			String fileName =  strStopId + type + png;	
			
			addBufferedImage(graphics, fileName, 400*(i));

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
		
		//add general error graph
		String graphName = "errorGraphErrorBiasOccupancy.png";
		addBufferedImage(graphics, graphName,400* (route.getStops().size()-1));
    	Graphics infoGraphImg = bufferedImage.getGraphics();
		infoGraphImg.setColor(Color.white);
		infoGraphImg.fillRect(400*(route.getStops().size()-1),300,400,50);
		
		graphics.dispose();

		File outputFile = new File(this.outputDir + this.str_itNum + strRouteId + type + png);
		ImageIO.write(bufferedImage,"png", outputFile);
		System.out.print("done.");
	}
	
	private void addBufferedImage(Graphics graphics, final String graphName, final int x) throws IOException{
		ZipEntry zipEntry = this.zipFile.getEntry(graphName);
		if (zipEntry!=null){   
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
	    	graphics.drawImage(errorGraphImg, x, 0, null); //i-1 it takes the place of the last (dummy station)
		}
	}
	
	public static void main(String[] args) {
		final String scheduleFile; 
		final String networkFile; 
		final String occupCountFile;
		final String kmzFile;
		
		if (args.length>0){
			scheduleFile = args[0];
			networkFile = args[1];
			occupCountFile =args[2];
			kmzFile = args[3];			
		}else{
			scheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
			networkFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			occupCountFile = "../../berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44_344.xml";
			kmzFile = "../../input/RouteStopGraphs/770.ptBseCountscompare.kmz";			
		}
		
		DataLoader dataLoader = new DataLoader();
		TransitSchedule schedule = dataLoader.readTransitSchedule(networkFile, scheduleFile);
		Counts counts = dataLoader.readCounts(occupCountFile);
		RouteStopGraphs2 routeStopGraphs = new RouteStopGraphs2(kmzFile, schedule, counts);
		
		//make bus line M44 routes
		try {
			routeStopGraphs.createRouteGraph("B-M44.101.901.H"); 
			//routeStopGraphs.createRouteGraph("B-M44.101.901.R");
			//routeStopGraphs.createRouteGraph("B-M44.102.901.H");
			//routeStopGraphs.createRouteGraph("B-M44.102.901.R"); 
		} catch (IOException e) {
			e.printStackTrace();
		}    
	}

}
