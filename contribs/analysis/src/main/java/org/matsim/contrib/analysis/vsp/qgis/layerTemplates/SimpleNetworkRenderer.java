package org.matsim.contrib.analysis.vsp.qgis.layerTemplates;

import java.awt.Color;

import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.QGisLineSymbolLayer;
import org.matsim.contrib.analysis.vsp.qgis.QGisPointSymbolLayer;
import org.matsim.contrib.analysis.vsp.qgis.QGisRenderer;
import org.matsim.contrib.analysis.vsp.qgis.VectorLayer;

public class SimpleNetworkRenderer extends QGisRenderer {

	private QGisConstants.geometryType geometryType;
	
	/**
	 * Instantiates a renderer for drawing a simple network (e.g. links).
	 * The specifications for the symbol layer are made in the private method {@code init()}; 
	 * 
	 * @param gType The type of geometry that is to be drawn.
	 */
	public SimpleNetworkRenderer(VectorLayer layer) {
		
		super(QGisConstants.renderingType.singleSymbol,layer);
		this.geometryType = layer.getGeometryType();
		
		init();
		
	}
	
	@Override
	public void init(){
		
		if(this.geometryType.equals(QGisConstants.geometryType.Point)){
			
			QGisPointSymbolLayer psl = new QGisPointSymbolLayer();
			psl.setId(0);
			psl.setColor(new Color(0,0,0,255));
			psl.setColorBorder(new Color(0,0,0,255));
			psl.setPenStyle(QGisConstants.penstyle.solid);
			psl.setLayerTransparency(1);
			psl.setSize(0.25);
			psl.setPointLayerSymbol(QGisConstants.pointLayerSymbol.circle);
			psl.setSizeUnits(QGisConstants.sizeUnits.MM);
			this.addSymbolLayer(psl);
			
		} else if(this.geometryType.equals(QGisConstants.geometryType.Line)){
		
			QGisLineSymbolLayer lsl = new QGisLineSymbolLayer();
			lsl.setId(0);
			lsl.setPenStyle(QGisConstants.penstyle.solid);
			lsl.setSizeUnits(QGisConstants.sizeUnits.MM);
			lsl.setColor(new Color(0,0,0,255));
			lsl.setLayerTransparency(0);
			lsl.setWidth(0.25);
			
			this.addSymbolLayer(lsl);
		
		} else{
			
			throw new RuntimeException("Unsupported geometry type!");
			
		}
			
	}

}
