package org.matsim.contrib.analysis.vsp.qgis.layerTemplates;

import java.awt.Color;

import org.matsim.contrib.analysis.vsp.qgis.GraduatedSymbolRenderer;
import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.QGisPointSymbolLayer;
import org.matsim.contrib.analysis.vsp.qgis.Range;
import org.matsim.contrib.analysis.vsp.qgis.VectorLayer;

public class AccessibilityDensitiesRenderer extends GraduatedSymbolRenderer {

	private Range[] ranges;
	
	private int symbolSize;
	private Integer populationThreshold;
	
//	public AccessibilityDensitiesRenderer(VectorLayer layer) {
//		super(layer.getHeader(),layer);
//		init();
//	}
	
	
	public AccessibilityDensitiesRenderer(VectorLayer layer, int populationThreshold, int symbolSize) {
		super(layer.getHeader(),layer);

		this.symbolSize = symbolSize;
		this.populationThreshold = populationThreshold;
		init();
	}
	
	
	@Override
	public void init(){
		
//		this.ranges = new Range[4];
//		this.ranges[0] = new Range(0, 50, "0 - 50");
//		this.ranges[1] = new Range(50, 100, "50 - 100");
//		this.ranges[2] = new Range(100, 200, "100 - 200");
//		this.ranges[3] = new Range(200, 2726, "> 200");
		
		this.ranges = new Range[2];
		this.ranges[0] = new Range(0, this.populationThreshold, "0 - " + this.populationThreshold.toString());
		this.ranges[1] = new Range(this.populationThreshold, 2726, "> " + this.populationThreshold.toString());

		
		double sizeMapUnitScale[] = {0,0};
//		int size = this.symbolSize;
		
		QGisPointSymbolLayer level0 = new QGisPointSymbolLayer();
		level0.setId(0);
		level0.setLayerTransparency(1.);
		level0.setColor(new Color(255,255,255,255));
		level0.setColorBorder(new Color(0,0,0,255));
		level0.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level0.setSize(this.symbolSize);
		level0.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level0.setSizeMapUnitScale(sizeMapUnitScale);
		level0.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(level0);
		
//		QGisPointSymbolLayer level1 = new QGisPointSymbolLayer();
//		level1.setId(1);
//		level1.setLayerTransparency(2./3);
//		level1.setColor(new Color(255,255,255,255));
//		level1.setColorBorder(new Color(0,0,0,255));
//		level1.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
//		level1.setSize(size);
//		level1.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
//		level1.setSizeMapUnitScale(sizeMapUnitScale);
//		level1.setPenStyle(QGisConstants.penstyle.no);
//		this.addSymbolLayer(level1);
//		
//		QGisPointSymbolLayer level2 = new QGisPointSymbolLayer();
//		level2.setId(2);
//		level2.setLayerTransparency(1./3);
//		level2.setColor(new Color(255,255,255,255));
//		level2.setColorBorder(new Color(0,0,0,255));
//		level2.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
//		level2.setSize(size);
//		level2.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
//		level2.setSizeMapUnitScale(sizeMapUnitScale);
//		level2.setPenStyle(QGisConstants.penstyle.no);
//		this.addSymbolLayer(level2);
		
		QGisPointSymbolLayer level3 = new QGisPointSymbolLayer();
		level3.setId(3);
		level3.setLayerTransparency(0.);
		level3.setColor(new Color(255,255,255,255));
		level3.setColorBorder(new Color(0,0,0,255));
		level3.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level3.setSize(this.symbolSize);
		level3.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level3.setSizeMapUnitScale(sizeMapUnitScale);
		level3.setPenStyle(QGisConstants.penstyle.no);
		this.addSymbolLayer(level3);
		
	}

	@Override
	public Range[] getRanges() {
		return this.ranges;
	}

}
