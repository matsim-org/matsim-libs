package playground.dhosse.qgis.layerTemplates.networkSimple;

import java.awt.Color;

import playground.dhosse.qgis.QGisConstants;
import playground.dhosse.qgis.QGisLineSymbolLayer;
import playground.dhosse.qgis.QGisPointSymbolLayer;
import playground.dhosse.qgis.QGisRenderer;

public class SingleSymbolRenderer extends QGisRenderer {

	public SingleSymbolRenderer(QGisConstants.geometryType gType) {
		super(QGisConstants.renderingType.singleSymbol);
		
		init(gType);
		
	}
	
	private void init(QGisConstants.geometryType gType){
		
		if(gType.equals(QGisConstants.geometryType.Point)){
			
			QGisPointSymbolLayer psl = new QGisPointSymbolLayer();
			
			psl.setColor(new Color(0,0,0,255));
			psl.setLayerTransparency(0);
			psl.setColorBorder(new Color(0,0,0,255));
			psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.circle);
			psl.setSize(0.25);
			
			this.addSymbolLayer(psl);
			
		} else if(gType.equals(QGisConstants.geometryType.Line)){
			
			QGisLineSymbolLayer lsl = new QGisLineSymbolLayer();
			
			lsl.setColor(new Color(0,0,0,255));
			lsl.setLayerTransparency(0);
			lsl.setWidth(0.25);
			
			this.addSymbolLayer(lsl);
			
		}
		
	}

}
