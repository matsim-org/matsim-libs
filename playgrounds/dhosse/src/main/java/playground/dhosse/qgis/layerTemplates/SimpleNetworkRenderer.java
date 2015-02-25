package playground.dhosse.qgis.layerTemplates;

import java.awt.Color;

import playground.dhosse.qgis.ColorRamp;
import playground.dhosse.qgis.QGisConstants;
import playground.dhosse.qgis.QGisLineSymbolLayer;
import playground.dhosse.qgis.QGisPointSymbolLayer;
import playground.dhosse.qgis.QGisRenderer;
import playground.dhosse.qgis.Range;

public class SimpleNetworkRenderer extends QGisRenderer {

	public SimpleNetworkRenderer(QGisConstants.geometryType gType) {
		
		super(QGisConstants.renderingType.singleSymbol);
		
		init(gType);
		
	}
	
	private void init(QGisConstants.geometryType gType){
		
		if(gType.equals(QGisConstants.geometryType.Point)){
			
			QGisPointSymbolLayer psl = new QGisPointSymbolLayer();
			
			psl.setPenStyle(QGisConstants.penstyle.solid);
			psl.setSizeUnits(QGisConstants.sizeUnits.MM);
			psl.setColor(new Color(0,0,0,255));
			psl.setLayerTransparency(0);
			psl.setColorBorder(new Color(0,0,0,255));
			psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.circle);
			psl.setSize(0.25);
			
			this.addSymbolLayer(psl);
			
		} else if(gType.equals(QGisConstants.geometryType.Line)){
			
			QGisLineSymbolLayer lsl = new QGisLineSymbolLayer();

			lsl.setPenStyle(QGisConstants.penstyle.solid);
			lsl.setSizeUnits(QGisConstants.sizeUnits.MM);
			lsl.setColor(new Color(0,0,0,255));
			lsl.setLayerTransparency(0);
			lsl.setWidth(0.25);
			
			this.addSymbolLayer(lsl);
			
		}
		
	}

	@Override
	public Range[] getRanges() {
		return null;
	}

	@Override
	public String getRenderingAttribute() {
		return null;
	}

	@Override
	public void setRenderingAttribute(String attr) {
		
	}

	@Override
	public ColorRamp getColorRamp() {
		return null;
	}

	@Override
	public void setColorRamp(ColorRamp colorRamp) {
		
	}

}
