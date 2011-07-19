package playground.mmoyo.cadyts_integration.ptBseAsPlanStrategy;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.DefaultKeyedValues;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtilities;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkImpl;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
//import org.matsim.counts.Volume;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.counts.reader.CountsReader;
import playground.mmoyo.io.TextFileWriter;
import playground.mmoyo.utils.DataLoader;

class CadytsErrorPlot {
	private static final String NL = "\n";
	private static final String TAB = "\t";
	private static final String SEP = " ";
	private static final String strRoute = "B-M44.101.901.H";
	private static final String X_AXIS = "hour";
	private static final String TITLE = "Weighted square error pro station";
	private static final String Y_AXIS = "Weighted Square Error";
	private static final String PNG = "png";
	private static final String DOTPNG = ".png";
	private static final String WSPLOT = "wsePlot.txt";
	
	void createPlot(final TransitSchedule trSched, final PtBseCountsComparisonAlgorithm ccaOccupancy, final double minStddev, final String iterPath){
		DataLoader dataLoader = new DataLoader();
		List <TransitRouteStop> stoplist = dataLoader.getTransitRoute(strRoute, trSched).getStops();
		StringBuffer sBuff = new StringBuffer();
		double maxError = Double.MIN_VALUE;
		Map <Id, DefaultKeyedValues> facilIdKeyedValue_Map = new LinkedHashMap <Id, DefaultKeyedValues>();
		//Map <Id, XYSeries> facilIdKeyedValue_Map = new LinkedHashMap <Id, XYSeries>();
		
		for (TransitRouteStop stop : stoplist){
			Id trStopFacId = stop.getStopFacility().getId();
			DefaultKeyedValues data = new DefaultKeyedValues();
			//XYSeries series = new XYSeries(Y_AXIS);
			sBuff.append(NL + trStopFacId + NL);
			int[] simVolumes = ccaOccupancy.getVolumesForStop(trStopFacId);
			Count count = ccaOccupancy.counts.getCount(trStopFacId);
			for (int bin=0; bin<24; bin++){
				double y=  count.getVolume(bin+1).getValue();
				double q = simVolumes[bin] * ccaOccupancy.countsScaleFactor;  //Verify if they already were multiplied //double q=  this.simResults.getSimValue(stop.getStopFacility(), startTime_s, endTime_s, TYPE.FLOW_VEH_H); 
				double dev = 2* (0.5 * Math.max(y, Math.pow(minStddev,2))); 
				double mwse = (Math.pow((y-q), 2))/dev;
				maxError = Math.max( maxError, mwse);
				data.addValue(Integer.toString(bin), mwse);
				//series.add(bin + 1, mwse);
				sBuff.append( (bin+1) + TAB + mwse + NL);
			}
			facilIdKeyedValue_Map.put(trStopFacId , data);
		}
		//write to output text file
		String txtfilenPath = iterPath + WSPLOT; 
		new TextFileWriter().write(sBuff.toString(), txtfilenPath , true);
		//create plot
		facilIdKeyedValue_Map.remove(facilIdKeyedValue_Map.keySet().toArray()[facilIdKeyedValue_Map.keySet().size()-1]);//delete last station that does not return any data
		int stopIndex= 0;
		BufferedImage bufferedImage = new BufferedImage(400*facilIdKeyedValue_Map.size(), 350 , BufferedImage.TYPE_INT_RGB);
		Graphics graphics = bufferedImage.getGraphics();
		for(Map.Entry <Id,DefaultKeyedValues> entry: facilIdKeyedValue_Map.entrySet() ){
		//for(Map.Entry <Id,XYSeries> entry: facilIdKeyedValue_Map.entrySet() ){
			Id  stopFacilityId = entry.getKey();

			CategoryDataset categoryDataset = DatasetUtilities.createCategoryDataset(X_AXIS, entry.getValue());
			//XYSeries series = facilIdKeyedValue_Map.get(stopFacilityId);
			//XYDataset xyDataset = new XYSeriesCollection(series);
			
			JFreeChart chart = ChartFactory.createLineChart(TITLE , X_AXIS, Y_AXIS, categoryDataset, PlotOrientation.VERTICAL, false, true, false);
			//JFreeChart chart = ChartFactory.createXYLineChart(TITLE , X_AXIS, Y_AXIS, xyDataset, PlotOrientation.VERTICAL, false, true, false);
			chart.setBackgroundPaint(Color.getHSBColor((float) 0.0, (float) 0.0, (float) 0.93));
			chart.getTitle().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
			String strStationInfo = stopFacilityId + SEP + ccaOccupancy.counts.getCount(stopFacilityId).getCsId();
			chart.addSubtitle(new TextTitle(strStationInfo, new Font(Font.SANS_SERIF, Font.PLAIN, 10)));
			
			BarRenderer renderer = new BarRenderer();
			renderer.setShadowVisible(false);
			renderer.setBarPainter( new StandardBarPainter() );
			
			CategoryPlot plot =chart.getCategoryPlot();
			plot.setBackgroundPaint(Color.white);
			plot.setRangeGridlinePaint(Color.gray);
			plot.setRangeGridlinesVisible(true);
			plot.setRenderer(0, renderer);
			plot.getRangeAxis().setRange(0, maxError);   //set range so that all graphs have the same scale
			plot.setDomainGridlinesVisible(true);
			plot.getDomainAxis().setTickLabelFont(new Font(Font.SANS_SERIF, Font.PLAIN, 7));
			
			BufferedImage bufImage = chart.createBufferedImage(400, 350);
			graphics.drawImage(bufImage, stopIndex*400, 0, null);
			/*  individual charts
			try {
				ChartUtilities.saveChartAsPNG(new File(outDir + stopFacilityId + PNG), chart, 800, 600, null, true, 9);
			} catch (IOException e) {
				e.printStackTrace();
			}
			*/
			stopIndex++;
		}

		graphics.dispose();	
	 	Graphics infoGraphImg = bufferedImage.getGraphics();
		infoGraphImg.setColor(Color.white);
		String outputFile = iterPath + "Cadyts_error_"+ strRoute +  DOTPNG;				
		try {
			ImageIO.write(bufferedImage, PNG, new File(outputFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		String realOccupFilePath = "../../berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44.xml";
		String netfilepath = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/network_multimodal.xml.gz";
		String trScheduleFilePath= "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
		double minstrdv = 8.0;
		
		String ocupFilePath = "../../input/juli/cadytsError/500.simCountCompareOccupancy.txt";
		final String outputDir = "../../input/cadytsError/"; 
		
		//load data
		DataLoader dataloader = new DataLoader();
		NetworkImpl net = dataloader.readNetwork(netfilepath);
		TransitSchedule schedule = dataloader.readTransitSchedule(net, trScheduleFilePath);
		Counts realCounts = dataloader.readCounts(realOccupFilePath);
		
		/////create a PtBseOccupancyAnalyzer with simulated occupancy values
		CountsReader countsReader = new CountsReader (ocupFilePath);
		Map<Id, int[]> SimOccupancies = new HashMap<Id, int[]>();
		for (Id stopId : countsReader.getStopsIds()){
			double[] dblSimValues = countsReader.getStopSimCounts(stopId);
			int[] intSimValues = new int[24];
			System.out.println("\n"+ stopId);
			for (int i=0;i<24;i++){  
				intSimValues[i]=(int)dblSimValues[i];  //convert from double to integer
				
				//Volume vol = realCounts.getCounts().get(stopId).getVolume(i+1);
				//double realCountValue = vol!=null? vol.getValue():0.0;
				//System.out.println(i + " " + intSimValues[i]  + " " + realCountValue);
			}
			SimOccupancies.put(stopId, intSimValues);
		}
		PtBseOccupancyAnalyzer ptBseOccupancyAnalyzer = new PtBseOccupancyAnalyzer();;
		ptBseOccupancyAnalyzer.setOccupancies(SimOccupancies);
		////////////////////////////////////////////////////////
		
		PtBseCountsComparisonAlgorithm ptBseCountsComparisonAlgorithm = new PtBseCountsComparisonAlgorithm(ptBseOccupancyAnalyzer, realCounts, net, 0.0);  //scaleFactor 0 because scaled value is read

		CadytsErrorPlot cadytsErrorPlot = new CadytsErrorPlot();
		cadytsErrorPlot.createPlot(schedule, ptBseCountsComparisonAlgorithm, minstrdv, outputDir);
		
	}
	
}
