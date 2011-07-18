package playground.mmoyo.utils.calibration;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
//import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.DefaultKeyedValues;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.matsim.api.core.v01.Id;
import org.matsim.counts.Counts;
import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy.PtBseLinkCostOffsetsXMLFileIO;
import playground.mmoyo.utils.DataLoader;
import cadyts.utilities.misc.DynamicData;

/**Produces offset bar plots */
public class LinkCostOffsetbars {
	private static final String TITLE = "Offset for station";
	private static final String X_AXIS = "hour";
	private static final String Y_AXIS = "offset value";
	private static final String PNG = ".png";
	
	private GradientPaint gpBlue = new GradientPaint(0.0f, 0.0f, Color.blue, 0.0f, 0.0f, Color.lightGray);
	private GradientPaint gpRed = new GradientPaint(0.0f, 0.0f, Color.lightGray , 0.0f, 0.0f,  Color.red);
	//private Paint gpBlue = Color.blue;
	//private Paint gpRed = Color.red;
	boolean weighted = false;
	double minStddev = 8.0;
	
	public LinkCostOffsetbars (boolean weighted ){
		this.weighted = weighted;  
	}
	
	private void createGraphs (final TransitRoute trRoute, final DynamicData<TransitStopFacility> stopOffsets, final Counts counts, final String outDir){
		Map <Id, DefaultKeyedValues> facilIdKeyedValue_Map = new LinkedHashMap <Id, DefaultKeyedValues>();

		/////first find out upper and lower values///////////
		double lower=0;
		double upper=0;
		for (TransitRouteStop stop : trRoute.getStops()){
			DefaultKeyedValues data = new DefaultKeyedValues();
			for (int i=0;i<24;i++){
				double linkOffsetValue = stopOffsets.getBinValue(stop.getStopFacility(), i);
				
				if (this.weighted){
					Volume vol = counts.getCount(stop.getStopFacility().getId()).getVolume(i+1);
					double y = vol!=null? vol.getValue() : 0 ; 
					/*first version*/ linkOffsetValue = linkOffsetValue / Math.max(y, Math.pow(minStddev,2)); 
					//second version linkOffsetValue = linkOffsetValue / Math.pow(Math.max(y, minStddev),2);
					//third version linkOffsetValue = linkOffsetValue / Math.max(Math.sqrt(y), minStddev);
					//System.out.println(stop.getStopFacility().getId() + " " + i + " " + y + " " + linkOffsetValue);
				}
				
				upper = Math.max(linkOffsetValue, upper);
				lower = Math.min(linkOffsetValue, lower);
				data.addValue(Integer.toString(i+1), linkOffsetValue);
			}
			facilIdKeyedValue_Map.put(stop.getStopFacility().getId(), data);
		}///////////////////////////////////////////////////////////////////////
		
		
		facilIdKeyedValue_Map.remove(facilIdKeyedValue_Map.keySet().toArray()[facilIdKeyedValue_Map.keySet().size()-1]);//delete last station that does not return any data
		
		int stopIndex= 0;
		BufferedImage bufferedImage = new BufferedImage(400*facilIdKeyedValue_Map.size(), 350 , BufferedImage.TYPE_INT_RGB);
		Graphics graphics = bufferedImage.getGraphics();
		for(Map.Entry <Id,DefaultKeyedValues> entry: facilIdKeyedValue_Map.entrySet() ){
			Id  stopFacilityId = entry.getKey();

			CategoryDataset categoryDataset = DatasetUtilities.createCategoryDataset(X_AXIS, entry.getValue());
			JFreeChart chart = ChartFactory.createBarChart(TITLE , X_AXIS, Y_AXIS, categoryDataset, PlotOrientation.VERTICAL, false, true, false);
			chart.setBackgroundPaint(Color.getHSBColor((float) 0.0, (float) 0.0, (float) 0.93));
			chart.getTitle().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
			String strStationInfo = stopFacilityId + " " + counts.getCount(stopFacilityId).getCsId();
			chart.addSubtitle(new TextTitle(strStationInfo, new Font(Font.SANS_SERIF, Font.PLAIN, 10)));
			BarRenderer renderer = new MyBarRenderer();
			renderer.setShadowVisible(false);
			renderer.setBarPainter( new StandardBarPainter() );
			CategoryPlot plot =chart.getCategoryPlot();
			plot.setBackgroundPaint(Color.white);
			plot.setRangeGridlinePaint(Color.gray);
			plot.setRangeGridlinesVisible(true);
			plot.setRenderer(0, renderer);
			plot.getRangeAxis().setRange(lower, upper);   //set range so that all graphs have the same scale
			plot.setDomainGridlinesVisible(true);
			plot.setDomainGridlinePaint(Color.gray);
			plot.getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
			//plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
			
			BufferedImage bufImage = chart.createBufferedImage(400, 350);
			graphics.drawImage(bufImage, stopIndex*400, 0, null);
		
			/*this is to write the individual stations
			try {
				ChartUtilities.saveChartAsPNG(new File(outDir + stopFacilityId + PNG), chart, 800, 600, null, true, 9);
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			
			stopIndex++;
		}

		graphics.dispose();	
	 	Graphics infoGraphImg = bufferedImage.getGraphics();
		infoGraphImg.setColor(Color.white);
		String weighted = this.weighted? "weighted": "noWeighted";
		File outputFile = new File(outDir + weighted + "OffsetBars_" + trRoute.getId() +  PNG);
		try {
			ImageIO.write(bufferedImage,"png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.print("done.");
	}
	
	class MyBarRenderer extends BarRenderer {
		private static final long serialVersionUID = 1L;

		MyBarRenderer(){
			super();
		}
		
		//override to get diff. color depending on the sign
		public Paint getItemPaint(final int series, final int item) {
			return super.getPlot().getDataset().getValue(series, item).doubleValue()>= 0.00? gpBlue : gpRed; 
		}
    }
	
	public static void main(String[] args) {
		String netFilePath;
		String linkCostOffsetFilePath;
		String countsFilePath;
		String transitScheduleFilePath;
		String strRouteId;
		
		if (args.length ==0){
			netFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
			countsFilePath = "../../berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44.xml";
			transitScheduleFilePath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
			strRouteId = "B-M44.101.901.H";
			linkCostOffsetFilePath = "../../input/juli/500/500.linkCostOffsets.xml";
		}else{
			netFilePath = args[0];
			linkCostOffsetFilePath = args[1];
			countsFilePath = args[2];
			transitScheduleFilePath = args[3];
			strRouteId = args[4];
		}
		
		
		//load data
		DataLoader dLoader = new DataLoader();
		Counts counts = dLoader.readCounts(countsFilePath);
		TransitSchedule schedule = dLoader.readTransitSchedule(netFilePath, transitScheduleFilePath);
		PtBseLinkCostOffsetsXMLFileIO reader = new PtBseLinkCostOffsetsXMLFileIO (schedule);
		DynamicData<TransitStopFacility> stopOffsets = reader.read(linkCostOffsetFilePath);
		TransitRoute trRoute = dLoader.getTransitRoute(strRouteId, schedule);
	
		File file = new File(linkCostOffsetFilePath);
		new LinkCostOffsetbars(true).createGraphs(trRoute, stopOffsets, counts, file.getParent() + "/");
		new LinkCostOffsetbars(false).createGraphs(trRoute, stopOffsets, counts, file.getParent() + "/");
		
	}
}