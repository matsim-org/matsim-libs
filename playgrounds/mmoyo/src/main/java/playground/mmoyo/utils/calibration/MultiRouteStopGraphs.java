package playground.mmoyo.utils.calibration;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.mmoyo.analysis.counts.reader.CountsReader;
import playground.mmoyo.utils.DataLoader;

public class MultiRouteStopGraphs{
	private final String REALCOUNTS = "Real Counts";
	final Counts counts;
	
	public MultiRouteStopGraphs(final Counts counts)  {
		this.counts= counts;
	}

	public void run(final List<TransitStopFacility> stopFList,List<Tuple<String,String>> coutCompareFileList){
		List<Tuple<String,CountsReader>> countsReaderList = new ArrayList<Tuple<String,CountsReader>>();
		
		//reads counts to translate tuple to real counts 
		for (Tuple<String, String> tuple : coutCompareFileList){
			CountsReader countsReader= new CountsReader(tuple.getSecond());
			countsReaderList.add(new Tuple<String, CountsReader>(tuple.getFirst(), countsReader));
		}
		
		for (TransitStopFacility stopF : stopFList){
			Id stopId = stopF.getId();
			DefaultCategoryDataset linesDataset = new DefaultCategoryDataset();			
		    
			for (Tuple<String,CountsReader> tuple: countsReaderList){
				CountsReader countsReader= tuple.getSecond();
				double[] realCountsFromccFile = countsReader.getRealValues(stopId);
				double[] simulatedScaledCounts = countsReader.getSimulatedScaled(stopId);

				for (int h = 0; h < 24; h++) {
					//validate that real counts match
					if (realCountsFromccFile[h] != this.counts.getCount(stopId).getVolume(h+1).getValue()) { 
						throw new RuntimeException("Different values for real counts, validate input files");
					}
					
					//handle simulatedScaled counts
					linesDataset.addValue(simulatedScaledCounts[h], tuple.getFirst(), Integer.toString(h + 1));
				}	
			}

			//save real counts first in barsDataset
			DefaultCategoryDataset barsDataset = new DefaultCategoryDataset();
			for (int h = 1; h <= 24; h++) {
				barsDataset.addValue(this.counts.getCount(stopId).getVolume(h).getValue() , this.REALCOUNTS, Integer.toString(h));  
			}
			
			createChart (this.counts.getCount(stopId) , barsDataset, linesDataset);
		}
	}
	
	private void createChart(final Count count, final DefaultCategoryDataset barsDataset, final DefaultCategoryDataset linesDataset){
        final JFreeChart chart = ChartFactory.createBarChart(
            count.getCsId(),  					// chart title
            "hour",         			// y axis label
            "Mean rel error [%]", 		// x axis label
            barsDataset,           		// primary dataset
            PlotOrientation.VERTICAL, 
            true,               		// include legend
            true,
            false
        );

        Color transparent = new Color(0, 0, 255, 0);
        chart.setBackgroundPaint(transparent);
        chart.getLegend().setVisible(false);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 19));
        
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        plot.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);
       
        BarRenderer barRenderer = new BarRenderer();
        barRenderer.setSeriesOutlinePaint(0, Color.gray);
        barRenderer.setSeriesPaint(0, transparent);  //  Color.getHSBColor((float) 0.62, (float) 0.56, (float) 0.93) 
        barRenderer.setSeriesToolTipGenerator(0, new StandardCategoryToolTipGenerator());
        barRenderer.setItemMargin(0.0);
        barRenderer.setShadowVisible(false);  
        barRenderer.setBarPainter( new StandardBarPainter() );
        barRenderer.setDrawBarOutline(true);
        barRenderer.setShadowVisible(false);  
        
        final CategoryItemRenderer linesRenderer = new LineAndShapeRenderer();
        linesRenderer.setSeriesPaint(0, Color.red);
        linesRenderer.setSeriesPaint(1, Color.green);
        linesRenderer.setSeriesPaint(2, Color.blue);

        plot.setRenderer(0, barRenderer);
        plot.setRenderer(1, linesRenderer);
        plot.setDataset(1, linesDataset);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setBackgroundPaint(transparent);
        plot.setRangeGridlinePaint(transparent); //it is better transparent than omitting it		
		
		try {
			ChartUtilities.saveChartAsPNG(new File("../../input/tmp/"+ count.getLocId() + ".png"), chart , 650, 455);
		} catch (IOException e) {
			e.printStackTrace();
		}

    }

    public static void main(final String[] args) {
    	String scheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
    	String countsFile = "../..//berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44.xml";
    	String strRouteId = "B-M44.101.901.H";
    	
    	List<Tuple<String,String>> countCompareFileList = new ArrayList<Tuple<String,String>>();
    	countCompareFileList.add(new Tuple<String, String>("Sim. Before Calibration", "../../runs_manuel/CalibLineM44/outMatsimRoutes/ITERS/it.10/10.simCountCompareOccupancy.txt"));
		countCompareFileList.add(new Tuple<String, String>("Sim. After Manual Calibration", "../../runs_manuel/CalibLineM44/manualCalibration/walk10.0_dist0.0_tran240.0/ITERS/it.10/10.simCountCompareOccupancy.txt"));
		//countCompareFileList.add(new Tuple<String, String>("After Automatic Calibration 5x", "../../runs_manuel/CalibLineM44/automaticCalibration5x/cad1/output/ITERS/it.500/500.simBseCountCompareOccupancy.txt"));
		countCompareFileList.add(new Tuple<String, String>("Sim. After Automatic Calibration 10x", "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.simBseCountCompareOccupancy.txt"));
		
		//load data
		DataLoader dataLoader = new DataLoader();
		TransitSchedule schedule = dataLoader.readTransitSchedule(scheduleFile);
		Counts counts = dataLoader.readCounts(countsFile);
		MultiRouteStopGraphs multiRouteStopGraphs = new MultiRouteStopGraphs(counts);
		List<TransitStopFacility> stopFList = new ArrayList<TransitStopFacility>();
    	for (TransitRouteStop stop: dataLoader.getTransitRoute(strRouteId, schedule).getStops()){
    		stopFList.add(stop.getStopFacility());	
    	}
		
    	multiRouteStopGraphs.run(stopFList, countCompareFileList);
    	
    }
       
}