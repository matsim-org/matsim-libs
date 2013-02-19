package playground.mmoyo.utils.calibration;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

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
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.counts.reader.CountsReader;
import playground.mmoyo.utils.DataLoader;

public class MultiRouteStopGraphs{
	final Counts counts;
	String outDir;	
	final String STR_PNG = ".png";
	
	public MultiRouteStopGraphs(final Counts counts)  {
		this.counts= counts;
	}

	public void run(final TransitRoute tRoute, List<Tuple<String,String>> coutCompareFileList, final String outDir){
		List<Tuple<String,CountsReader>> countsReaderList = new ArrayList<Tuple<String,CountsReader>>();
		this.outDir = outDir;
		
		//reads counts to translate tuple to real counts 
		for (Tuple<String, String> tuple : coutCompareFileList){
			CountsReader countsReader= new CountsReader(tuple.getSecond());
			countsReaderList.add(new Tuple<String, CountsReader>(tuple.getFirst(), countsReader));
		}
		
    	for (TransitRouteStop stop: tRoute.getStops()){
    		Id stopFactilityId = stop.getStopFacility().getId();
			DefaultCategoryDataset linesDataset = new DefaultCategoryDataset();			
		    
			for (Tuple<String,CountsReader> tuple: countsReaderList){
				CountsReader countsReader= tuple.getSecond();
				double[] realCountsFromccFile = countsReader.getRealValues(stopFactilityId);
				double[] simulatedScaledCounts = countsReader.getSimulatedScaled(stopFactilityId);

				for (int h = 0; h < 24; h++) {
					//validate that real counts match
					if (realCountsFromccFile[h] != this.counts.getCount(stopFactilityId).getVolume(h+1).getValue()) { 
						throw new RuntimeException("Different values for real counts, validate input files");
					}
					
					//handle simulatedScaled counts
					linesDataset.addValue(simulatedScaledCounts[h], tuple.getFirst(), Integer.toString(h + 1));
				}	
			}

			//save real counts first in barsDataset
			final String REALCOUNTS = "Real Counts";
			DefaultCategoryDataset barsDataset = new DefaultCategoryDataset();
			for (int h = 1; h <= 24; h++) {
				barsDataset.addValue(this.counts.getCount(stopFactilityId).getVolume(h).getValue() , REALCOUNTS, Integer.toString(h));  
			}
			createChart (this.counts.getCount(stopFactilityId) , barsDataset, linesDataset);
		}
	
    	//Put together all graphs in a image file
    	
    	BufferedImage bufferedImage = new BufferedImage(630* (tRoute.getStops().size()-1), 450 , BufferedImage.TRANSLUCENT);
    	Graphics graphics = bufferedImage.getGraphics();
    	//graphics.setColor(Color.white);
    	
    	for (int x=0; x< tRoute.getStops().size()-1; x++){   //the last stop won't be shown for OCCUPANCY comparisons, because all occupancies values are zero 
    		TransitRouteStop stop= tRoute.getStops().get(x);
    		String fileName = this.outDir + stop.getStopFacility().getId().toString() + STR_PNG;
    		File file = new File(fileName);
    		BufferedImage bufferedImageStop;
			try {
				bufferedImageStop = ImageIO.read(file);
				//bufferedImageStop.getGraphics().setColor(Color.white);
				graphics.drawImage(bufferedImageStop, 630*x, 0, null);
	
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			Graphics infoGraphImg = bufferedImage.getGraphics();
			infoGraphImg.setColor(Color.white);
    	}

    	/*
    	Graphics infoGraphImg = bufferedImage.getGraphics();
		infoGraphImg.setColor(Color.white);
		infoGraphImg.fillRect(400*(tRoute.getStops().size()),300,400,50);
		*/
    	graphics.dispose();
    	
		File outputFile = new File(this.outDir + "all" + this.STR_PNG);
		try {
			ImageIO.write(bufferedImage, "png", outputFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.print("done.");
	}
	
	private void createChart(final Count count, final DefaultCategoryDataset barsDataset, final DefaultCategoryDataset linesDataset){

		final String STR_ARIAL = "Arial";
		final String STR_HOUR = "hour"; 
		final String STR_MRE = "Mean rel error [%]"; 
		
		final JFreeChart chart = ChartFactory.createBarChart(
            count.getCsId(),  			// chart title
            STR_HOUR,         			// y axis label
            STR_MRE, 		// x axis label
            barsDataset,           		// primary dataset
            PlotOrientation.VERTICAL, 
            true,               		// include legend
            true,
            false
        );

        Color transparent = new Color(0, 0, 255, 0);
        chart.setBackgroundPaint(transparent);
        chart.getLegend().setVisible(false);
        chart.getTitle().setFont(new Font(STR_ARIAL, Font.BOLD, 19));
        
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
        linesRenderer.setSeriesPaint(3, Color.gray);
        linesRenderer.setSeriesPaint(4, Color.gray);
        linesRenderer.setSeriesPaint(5, Color.gray);
        

        plot.setRenderer(0, barRenderer);
        plot.setRenderer(1, linesRenderer);
        plot.setDataset(1, linesDataset);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
        plot.setBackgroundPaint(transparent);
        plot.setRangeGridlinePaint(transparent); //it is better transparent than omitting it		
		
        String chartFilePath = this.outDir + count.getLocId() + STR_PNG;
		try {
			ChartUtilities.saveChartAsPNG(new File(chartFilePath), chart , 650, 455);
		} catch (IOException e) {
			e.printStackTrace();
		}

    }

    public static void main(final String[] args) {
    	String scheduleFile = "../../berlin-bvg09/pt/nullfall_berlin_brandenburg/input/pt_transitSchedule.xml.gz";
    	String countsFile = "../..//berlin-bvg09/ptManuel/lines344_M44/counts/chen/counts_occupancy_M44.xml";
    	String strRouteId = "B-M44.101.901.H";
    	String outDir = "../../berlin-bvg09/ptManuel/lines344_M44/report/tasteVariations/";
    	
    	List<Tuple<String,String>> countCompareFileList = new ArrayList<Tuple<String,String>>();
    	countCompareFileList.add(new Tuple<String, String>("before calibration", "../../runs_manuel/CalibLineM44/outMatsimRoutes/ITERS/it.10/10.simCountCompareOccupancy.txt"));
    	countCompareFileList.add(new Tuple<String, String>("manual calibration", "../../runs_manuel/CalibLineM44/manualCalibration/walk10.0_dist0.0_tran240.0/ITERS/it.10/10.simCountCompareOccupancy.txt"));
    	countCompareFileList.add(new Tuple<String, String>("Cadyts influenced selection", "../../runs_manuel/CalibLineM44/automCalib10xTimeMutated/10xrun/it.500/500.simBseCountCompareOccupancy.txt"));

    	countCompareFileList.add(new Tuple<String, String>("Random seed 1", "../mmoyo/output/taste/seeds/1/10.simCountCompareOccupancy.txt"));
		countCompareFileList.add(new Tuple<String, String>("Random seed 2", "../mmoyo/output/taste/seeds/2/10.simCountCompareOccupancy.txt"));
		countCompareFileList.add(new Tuple<String, String>("Random seed 3", "../mmoyo/output/taste/seeds/3/10.simCountCompareOccupancy.txt"));
		
		//load data
		DataLoader dataLoader = new DataLoader();
		TransitSchedule schedule = dataLoader.readTransitSchedule(scheduleFile);
		Counts counts = dataLoader.readCounts(countsFile);
		TransitRoute tRoute = dataLoader.getTransitRoute(strRouteId, schedule);
		
		MultiRouteStopGraphs multiRouteStopGraphs = new MultiRouteStopGraphs(counts);
    	multiRouteStopGraphs.run(tRoute, countCompareFileList, outDir);
    	
    }
       
}