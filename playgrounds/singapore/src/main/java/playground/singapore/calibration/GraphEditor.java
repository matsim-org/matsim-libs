package playground.singapore.calibration;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.matsim.core.utils.charts.ChartUtil;

import java.awt.*;
import java.util.ArrayList;


/**
 * This class defines/changes the appearance of the chart passed to it 
 * (Color schemes, fonts etc.)  
 *
 * @author artemc
 */

public class GraphEditor {

	/** Definition of fonts used in the graphs */

	private Font axisLabelFont = new Font ("Sans-serif", Font.BOLD, 18);
	private Font axisTickLabelFont = new Font ("Sans-serif", Font.PLAIN, 16);
	private Font legendFont = new Font ("Sans-serif", Font.BOLD, 18);

	ArrayList<Color> colorScheme = new ArrayList<Color>();

	private ChartUtil chart;
	private CategoryPlot plot;
	private CategoryAxis axis1;
	Integer tickUnit = 10;

	public GraphEditor(ChartUtil chart, Integer tickUnit, String colorSchemeChoice){
		this.chart = chart;
		this.plot = chart.getChart().getCategoryPlot();
		this.axis1 = plot.getDomainAxis();
		this.tickUnit = tickUnit;
		setColorScheme(colorSchemeChoice);	
		defineScheme();
	}

	public GraphEditor(ChartUtil chart, Integer tickUnit, Double yRangeMin, Double yRangeMax, String colorSchemeChoice){
		this.chart = chart;
		this.plot = chart.getChart().getCategoryPlot();
		this.axis1 = plot.getDomainAxis();
		this.tickUnit = tickUnit;
		setColorScheme(colorSchemeChoice);		
		defineScheme();
		plot.getRangeAxis().setRange(yRangeMin, yRangeMax);		
	}

	public void defineScheme () {

		axis1.setCategoryMargin(0.30);
		plot.setBackgroundPaint(Color.white);

		plot.getDomainAxis().setLabelFont(axisLabelFont);
		plot.getDomainAxis().setTickLabelFont(axisTickLabelFont);	

		for(int i=0;i<plot.getRangeAxisCount();i++){
			plot.getRangeAxis(i).setLabelFont(axisLabelFont);
			plot.getRangeAxis(i).setTickLabelFont(axisTickLabelFont);
			NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis(i);
			numberAxis.setTickUnit(new NumberTickUnit(tickUnit));
		}

		plot.setDomainGridlinesVisible(true);
		plot.setRangeGridlinesVisible(true);
		plot.setDomainGridlinePaint(Color.GRAY);
		plot.setRangeGridlinePaint(Color.GRAY);
		plot.setForegroundAlpha(0.9f);

		LegendTitle legend = chart.getChart().getLegend();
		legend.setPosition(RectangleEdge.BOTTOM);
		legend.setHorizontalAlignment(HorizontalAlignment.CENTER);
		legend.setItemFont(legendFont);
		legend.setPadding(10D, 10D, 10D, 10D);
		legend.setBorder(1.0D, 1.0D, 1.0D, 1.0D);
		legend.setMargin(20D, 1D, 15D, 1D);

		chart.addMatsimLogo();		
	}


	public void barRenderer(){
		BarRenderer.setDefaultBarPainter(new StandardBarPainter());
		BarRenderer renderer= new BarRenderer();
		renderer.setItemMargin(0.10);
		renderer.setShadowVisible(false);
		renderer.setSeriesPaint(0,colorScheme.get(0));
		renderer.setSeriesPaint(1,colorScheme.get(1));
		renderer.setSeriesPaint(2,colorScheme.get(2));
		renderer.setSeriesPaint(3,colorScheme.get(3));
		renderer.setSeriesPaint(4,colorScheme.get(4));
		plot.setRenderer(renderer);
	}

	public void stackedBarRenderer(){
		BarRenderer.setDefaultBarPainter(new StandardBarPainter());
		BarRenderer renderer= new StackedBarRenderer();
		renderer.setItemMargin(0.10);
		renderer.setShadowVisible(false);
		renderer.setSeriesPaint(0,colorScheme.get(0));
		renderer.setSeriesPaint(1,colorScheme.get(1));
		renderer.setSeriesPaint(2,colorScheme.get(2));
		renderer.setSeriesPaint(3,colorScheme.get(3));
		renderer.setSeriesPaint(4,colorScheme.get(4));
		plot.setRenderer(renderer);
	}


	//	public void layoutWithDefinedRangeAxis(ChartUtil chart, ) {
	//		
	//		//Fonts
	//		Font axisLabelFont = new Font (chart.getChart().getCategoryPlot().DEFAULT_VALUE_LABEL_FONT.getFamily(), Font.BOLD, 18);
	//		Font axisTickLabelFont = new Font (chart.getChart().getCategoryPlot().getDomainAxis().DEFAULT_TICK_LABEL_FONT.getFamily(), Font.PLAIN, 16);
	//		
	//		// leave a gap of 20% between categories (groups of bars)
	//		final CategoryPlot plot = chart.getChart().getCategoryPlot();
	//		final CategoryAxis axis1 = plot.getDomainAxis();
	//		axis1.setCategoryMargin(0.30);
	//		plot.setBackgroundPaint(Color.white);
	//		
	//		// leave a gap of 10% between individual bars within one category
	//		BarRenderer.setDefaultBarPainter(new StandardBarPainter());
	//		BarRenderer renderer= new StackedBarRenderer();
	//		renderer.setItemMargin(0.10);
	//		renderer.setShadowVisible(false);
	//		renderer.setSeriesPaint(0,new Color(49, 133, 77));
	//		renderer.setSeriesPaint(1,new Color(255, 245, 132));
	//		renderer.setSeriesPaint(2,new Color(0, 126, 106));
	//		renderer.setSeriesPaint(3,new Color(146, 169, 130));
	//		renderer.setSeriesPaint(4,new Color(149, 193, 31));
	//		plot.setRenderer(renderer);
	//		
	//		plot.getDomainAxis().setLabelFont(axisLabelFont);
	//		plot.getDomainAxis().setTickLabelFont(axisTickLabelFont);		
	//		
	//		for(int i=0;i<plot.getRangeAxisCount();i++){
	//			plot.getRangeAxis(i).setLabelFont(axisLabelFont);
	//			plot.getRangeAxis(i).setTickLabelFont(axisTickLabelFont);
	//			NumberAxis numberAxis = (NumberAxis) plot.getRangeAxis(i);
	//			numberAxis.setTickUnit(new NumberTickUnit(10));
	//		}
	//		
	//		plot.setDomainGridlinesVisible(true);
	//		plot.setRangeGridlinesVisible(true);
	//		plot.setDomainGridlinePaint(Color.GRAY);
	//		plot.setRangeGridlinePaint(Color.GRAY);
	//		plot.setForegroundAlpha(0.9f);
	//		
	//	//	chart.addMatsimLogo();		
	//	}


	/**
	 * Definitions of different color schemes (partially borrowed from kuler.adobe.com)
	 */

	private void setColorScheme(String scheme) {
		if(scheme.equals("M8_Colors")){
			colorScheme.add(new Color(49, 133, 77));
			colorScheme.add(new Color(255, 245, 132));
			colorScheme.add(new Color(0, 126, 106));
			colorScheme.add(new Color(146, 169, 130));
			colorScheme.add(new Color(149, 193, 31));	
		}
		else if(scheme.equals("French Girl")){
			colorScheme.add(new Color(88, 131, 140));
			colorScheme.add(new Color(191, 88, 65));
			colorScheme.add(new Color(218, 215, 199));
			colorScheme.add(new Color(191, 153, 107));

			colorScheme.add(new Color(166, 28, 28));	
		}
		else if(scheme.equals("Lollapalooza")){
			colorScheme.add(new Color(0, 38, 53));
			colorScheme.add(new Color(1, 52, 64));
			colorScheme.add(new Color(171, 26, 37));
			colorScheme.add(new Color(217, 121, 37));
			colorScheme.add(new Color(239, 231, 190));	
		}
		else if(scheme.equals("Muted Rainbow")){
			colorScheme.add(new Color(178, 165, 137));
			colorScheme.add(new Color(255, 184, 150));
			colorScheme.add(new Color(255, 249, 177));
			colorScheme.add(new Color(154, 178, 133));
			colorScheme.add(new Color(17, 146, 158));	
		}
		else if(scheme.equals("Autumn")){
			colorScheme.add(new Color(124, 67, 0));
			colorScheme.add(new Color(159, 57, 26));
			colorScheme.add(new Color(235, 102, 33));
			colorScheme.add(new Color(246, 173, 26));
			colorScheme.add(new Color(156, 0, 26));	
		}
		else if(scheme.equals("Red_Scheme")){
			colorScheme.add(new Color(131, 9, 1));
			colorScheme.add(new Color(223, 114, 89));
			colorScheme.add(new Color(189, 50, 32));
			colorScheme.add(new Color(207, 157, 40));
			colorScheme.add(new Color(115, 61, 2));	
		}
		else{
			colorScheme.add(new Color(131, 9, 1));
			colorScheme.add(new Color(223, 114, 89));
			colorScheme.add(new Color(189, 50, 32));
			colorScheme.add(new Color(207, 157, 40));
			colorScheme.add(new Color(115, 61, 2));	
		}
		
	}

}


