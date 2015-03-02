package playground.dhosse.qgis.layerTemplates;

import java.awt.Color;

import playground.dhosse.qgis.QGisConstants;
import playground.dhosse.qgis.QGisPointSymbolLayer;
import playground.dhosse.qgis.Range;
import playground.dhosse.qgis.rendering.GraduatedSymbolRenderer;

public class NoiseRenderer extends GraduatedSymbolRenderer {
	
	private String renderingAttribute;
	private Range[] ranges;
	
	public NoiseRenderer() {

		super();
		
		this.init();
		
	}
	
	private void init(){
		
		this.ranges = new Range[8];
		this.ranges[0] = new Range(0, 45, " 45");
		this.ranges[1] = new Range(45, 50, "45 - 50");
		this.ranges[2] = new Range(50, 55, "50 - 55");
		this.ranges[3] = new Range(55, 60, "55 - 60");
		this.ranges[4] = new Range(60, 65, "60 - 65");
		this.ranges[5] = new Range(65, 70, "65 - 70");
		this.ranges[6] = new Range(70, 75, "70 - 75");
		this.ranges[7] = new Range(75, 999, "> 75");

		double sizeMapUnitScale[] = {0,0};
		int size = 35;
		
		QGisPointSymbolLayer psl = new QGisPointSymbolLayer();
		psl.setId(0);
		psl.setColor(new Color(26,150,65,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		psl.setLayerTransparency(1);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(1);
		psl.setColor(new Color(137,203,97,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		psl.setLayerTransparency(1);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(2);
		psl.setColor(new Color(219,239,157,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		psl.setLayerTransparency(1);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(3);
		psl.setColor(new Color(254,222,154,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		psl.setLayerTransparency(1);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(4);
		psl.setColor(new Color(245,144,83,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		psl.setLayerTransparency(1);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(5);
		psl.setColor(new Color(215,25,28,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		psl.setLayerTransparency(1);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(6);
		psl.setColor(new Color(128,0,0,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		psl.setLayerTransparency(1);
		this.addSymbolLayer(psl);
		
		psl = new QGisPointSymbolLayer();
		psl.setId(7);
		psl.setColor(new Color(73,0,0,255));
		psl.setColorBorder(new Color(0,0,0,255));
		psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.rectangle);
		psl.setSize(size);
		psl.setSizeUnits(QGisConstants.sizeUnits.MapUnit);
		psl.setSizeMapUnitScale(sizeMapUnitScale);
		psl.setPenStyle(QGisConstants.penstyle.no);
		psl.setLayerTransparency(1);
		this.addSymbolLayer(psl);
		
	}
	
	@Override
	public Range[] getRanges(){
		return this.ranges;
	}
	
	@Override
	public String getRenderingAttribute(){
		return this.renderingAttribute;
	}
	
	@Override
	public void setRenderingAttribute(String attr){
		this.renderingAttribute = attr;
	}
	
}