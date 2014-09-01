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
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.counts.algorithms.graphs.CountsGraph;
import playground.mmoyo.io.MRE_reader;

/** reads a number of biasErrorGraphDataOccupancy files and creates a single plot*/
public final class MultiBiasErrorGraph extends CountsGraph{
	private List<Tuple<String,double[]>> mreDblFileList;   //stores multiple mre values and their descriptions
	
	public MultiBiasErrorGraph(final List<Tuple<String,String>> mreFileList){
		init(mreFileList);
	}

	void init(final List<Tuple<String,String>> mreFileList){
		MRE_reader mre_reader= new MRE_reader();
		mreDblFileList = new ArrayList<Tuple<String,double[]>>();
		for (Tuple<String, String> tuple : mreFileList){
			try {
				mre_reader.readFile(tuple.getSecond());
			} catch (IOException e) {
				e.printStackTrace();
			}
			mreDblFileList.add(new Tuple<String, double[]>(tuple.getFirst(), mre_reader.getMRE()));
		}
	}
	
	@Override
	public JFreeChart createChart(int nbr) {
		DefaultCategoryDataset dataset0 = new DefaultCategoryDataset();
		//DefaultCategoryDataset dataset1 = new DefaultCategoryDataset();   bias		
		
		for (Tuple<String, double[]> tuple : this.mreDblFileList){
			for (int h = 0; h < 24; h++) {
				dataset0.addValue(tuple.getSecond()[h], tuple.getFirst(), Integer.toString(h + 1));
			}
		}
		
		this.chart_ = ChartFactory.createLineChart("", "Hour", "Mean rel error [%]", dataset0, PlotOrientation.VERTICAL,
				true, // legend?
				true, // tooltips?
				false // URLs?
				);
		
		CategoryPlot plot = this.chart_.getCategoryPlot();
		plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);

		final LineAndShapeRenderer renderer = new LineAndShapeRenderer();
		renderer.setSeriesPaint(0, Color.RED);
		renderer.setSeriesPaint(1, Color.GREEN);
		renderer.setSeriesPaint(2, Color.BLUE);
		renderer.setSeriesToolTipGenerator(0, new StandardCategoryToolTipGenerator());
		plot.setRenderer(0, renderer);
		
		Color transparent = new Color(0, 0, 255, 0);
		this.chart_.setBackgroundPaint(transparent); // pink : Color.getHSBColor((float) 0.0, (float) 0.0, (float) 0.93)
		plot.setBackgroundPaint(Color.getHSBColor((float) 0.0, (float) 0.0, (float) 0.93)); 	     
		plot.setRangeGridlinePaint(Color.gray);		
		plot.setRangeGridlinesVisible(true);      
		
		final CategoryAxis axisX = new CategoryAxis("Hour");
		axisX.setTickLabelFont(new Font("SansSerif", Font.BOLD, 15));
		plot.setDomainAxis(axisX);

		//final ValueAxis axis2 = new NumberAxis("Mean abs bias [agent/h]");
		//plot.setRangeAxis(1, axis2);

		final ValueAxis axisY = plot.getRangeAxis(0);
		axisY.setRange(0.0, 100.0);
		axisY.setTickLabelFont(new Font("SansSerif", Font.BOLD, 21));
		
		final LineAndShapeRenderer renderer2 = new LineAndShapeRenderer();
		renderer2.setSeriesToolTipGenerator(0, new StandardCategoryToolTipGenerator());
		renderer2.setSeriesToolTipGenerator(1, new StandardCategoryToolTipGenerator());
		plot.setRenderer(1, renderer2);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
		
		this.chart_.getLegend().setItemFont(new Font("SansSerif", Font.BOLD, 17));
		this.chart_.getLegend().setVisible(false);
		
		return this.chart_ ;
	}
	
	private void writeChart(final String outFile, final JFreeChart chart){
		try {
			ChartUtilities.saveChartAsPNG(new File(outFile), chart , 650, 455);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		List<Tuple<String,String>> mreFileList = new ArrayList<Tuple<String,String>>();
		
		mreFileList.add(new Tuple<String, String>("Before Calibration", "../../runs_manuel/CalibLineM44/outMatsimRoutes/ITERS/it.10/biasErrorGraphDataOccupancy.txt"));
		mreFileList.add(new Tuple<String, String>("After Manual Calibration", "../../runs_manuel/CalibLineM44/manualCalibration/walk10.0_dist0.0_tran240.0/ITERS/it.10/biasErrorGraphDataOccupancy.txt"));
		//mreFileList.add(new Tuple<String, String>("After Automatic Calibration 5x", "../../runs_manuel/CalibLineM44/automaticCalibration5x/cad1/output/ITERS/it.500/biasErrorGraphDataOccupancy.txt"));
		mreFileList.add(new Tuple<String, String>("After Automatic Calibration 10x", "../../input/tmp/biasErrorGraphDataOccupancy.txt"));
		
		MultiBiasErrorGraph multiBiasErrorGraph = new MultiBiasErrorGraph(mreFileList);
		multiBiasErrorGraph.writeChart("../../input/tmp/multiBiasErrorGraph.png", multiBiasErrorGraph.createChart(0));
	}

}
