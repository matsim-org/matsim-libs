package org.matsim.contrib.analysis.vsp.qgis.layerTemplates;

import java.awt.Color;

import org.matsim.contrib.analysis.vsp.qgis.GraduatedSymbolRenderer;
import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.QGisPointSymbolLayer;
import org.matsim.contrib.analysis.vsp.qgis.Range;
import org.matsim.contrib.analysis.vsp.qgis.VectorLayer;

public class AccessibilityRenderer extends GraduatedSymbolRenderer {
	
	private Range[] ranges;

	public AccessibilityRenderer(VectorLayer layer) {
		super(layer.getHeader(),layer);
		init();
	}

	@Override
	public void init() {
		
		this.ranges = new Range[9];
		this.ranges[0] = new Range(-67.38258, 2, " 2");
		this.ranges[1] = new Range(2, 2.5, "2 - 2.5");
		this.ranges[2] = new Range(2.5, 3, "2 - 3");
		this.ranges[3] = new Range(3, 3.5, "3 - 3.5");
		this.ranges[4] = new Range(3.5, 4, "3.5 - 4");
		this.ranges[5] = new Range(4, 4.5, "4 - 4.5");
		this.ranges[6] = new Range(4.5, 5, "4.5 - 5");
		this.ranges[7] = new Range(5, 5.5, "5 - 5.5");
		this.ranges[8] = new Range(5.5, 5.863296, "> 5.5");
		
		double sizeMapUnitScale[] = {0,0};
		int size = 1010;
		
		QGisPointSymbolLayer level0 = new QGisPointSymbolLayer();
		level0.setId(0);
		level0.setColor(new Color(215,25,28,255));
		level0.setColorBorder(new Color(0,0,0,255));
		level0.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level0.setSize(size);
		level0.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level0.setSizeMapUnitScale(sizeMapUnitScale);
		level0.setPenStyle(QGisConstants.penstyle.no);
		level0.setLayerTransparency(1);
		this.addSymbolLayer(level0);
		
		QGisPointSymbolLayer level1 = new QGisPointSymbolLayer();
		level1.setId(1);
		level1.setColor(new Color(234,99,62,255));
		level1.setColorBorder(new Color(0,0,0,255));
		level1.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level1.setSize(size);
		level1.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level1.setSizeMapUnitScale(sizeMapUnitScale);
		level1.setPenStyle(QGisConstants.penstyle.no);
		level1.setLayerTransparency(1);
		this.addSymbolLayer(level1);
		
		QGisPointSymbolLayer level2 = new QGisPointSymbolLayer();
		level2.setId(2);
		level2.setColor(new Color(253,174,97,255));
		level2.setColorBorder(new Color(0,0,0,255));
		level2.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level2.setSize(size);
		level2.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level2.setSizeMapUnitScale(sizeMapUnitScale);
		level2.setPenStyle(QGisConstants.penstyle.no);
		level2.setLayerTransparency(1);
		this.addSymbolLayer(level2);
		
		QGisPointSymbolLayer level3 = new QGisPointSymbolLayer();
		level3.setId(3);
		level3.setColor(new Color(254,214,144,255));
		level3.setColorBorder(new Color(0,0,0,255));
		level3.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level3.setSize(size);
		level3.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level3.setSizeMapUnitScale(sizeMapUnitScale);
		level3.setPenStyle(QGisConstants.penstyle.no);
		level3.setLayerTransparency(1);
		this.addSymbolLayer(level3);
		
		QGisPointSymbolLayer level4 = new QGisPointSymbolLayer();
		level4.setId(4);
		level4.setColor(new Color(255,255,191,255));
		level4.setColorBorder(new Color(0,0,0,255));
		level4.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level4.setSize(size);
		level4.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level4.setSizeMapUnitScale(sizeMapUnitScale);
		level4.setPenStyle(QGisConstants.penstyle.no);
		level4.setLayerTransparency(1);
		this.addSymbolLayer(level4);
		
		QGisPointSymbolLayer level5 = new QGisPointSymbolLayer();
		level5.setId(5);
		level5.setColor(new Color(213,238,177,255));
		level5.setColorBorder(new Color(0,0,0,255));
		level5.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level5.setSize(size);
		level5.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level5.setSizeMapUnitScale(sizeMapUnitScale);
		level5.setPenStyle(QGisConstants.penstyle.no);
		level5.setLayerTransparency(1);
		this.addSymbolLayer(level5);
		
		QGisPointSymbolLayer level6 = new QGisPointSymbolLayer();
		level6.setId(5);
		level6.setColor(new Color(171,221,164,255));
		level6.setColorBorder(new Color(0,0,0,255));
		level6.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level6.setSize(size);
		level6.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level6.setSizeMapUnitScale(sizeMapUnitScale);
		level6.setPenStyle(QGisConstants.penstyle.no);
		level6.setLayerTransparency(1);
		this.addSymbolLayer(level6);
		
		QGisPointSymbolLayer level7 = new QGisPointSymbolLayer();
		level7.setId(5);
		level7.setColor(new Color(107,176,175,255));
		level7.setColorBorder(new Color(0,0,0,255));
		level7.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level7.setSize(size);
		level7.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level7.setSizeMapUnitScale(sizeMapUnitScale);
		level7.setPenStyle(QGisConstants.penstyle.no);
		level7.setLayerTransparency(1);
		this.addSymbolLayer(level7);
		
		QGisPointSymbolLayer level8 = new QGisPointSymbolLayer();
		level8.setId(5);
		level8.setColor(new Color(43,131,186,255));
		level8.setColorBorder(new Color(0,0,0,255));
		level8.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		level8.setSize(size);
		level8.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		level8.setSizeMapUnitScale(sizeMapUnitScale);
		level8.setPenStyle(QGisConstants.penstyle.no);
		level8.setLayerTransparency(1);
		this.addSymbolLayer(level8);
		
	}
	
	@Override
	public Range[] getRanges(){
		return this.ranges;
	}
	
}
