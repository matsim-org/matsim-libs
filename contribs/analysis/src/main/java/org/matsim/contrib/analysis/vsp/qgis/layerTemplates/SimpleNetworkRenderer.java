package org.matsim.contrib.analysis.vsp.qgis.layerTemplates;

import java.awt.Color;

import org.matsim.contrib.analysis.vsp.QGisConstants;
import org.matsim.contrib.analysis.vsp.QGisLineSymbolLayer;
import org.matsim.contrib.analysis.vsp.QGisPointSymbolLayer;
import org.matsim.contrib.analysis.vsp.qgis.rendering.QGisRenderer;

public class SimpleNetworkRenderer extends QGisRenderer {

	/**
	 * Instantiates a renderer for drawing a simple network (e.g. links).
	 * The specifications for the symbol layer are made in the private method {@code init()}; 
	 * 
	 * @param gType The type of geometry that is to be drawn.
	 */
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

}
