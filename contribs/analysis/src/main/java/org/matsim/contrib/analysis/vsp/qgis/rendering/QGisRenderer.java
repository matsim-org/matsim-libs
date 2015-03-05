package org.matsim.contrib.analysis.vsp.qgis.rendering;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.analysis.vsp.qgis.QGisConstants;
import org.matsim.contrib.analysis.vsp.qgis.QGisSymbolLayer;

/**
 * 
 * This class provides minimum functionality for rendering a qgis symbol layer.
 * Can be extended by use cases that use a singleSymbolRenderer.
 * </p>
 * The renderer describes how a layer is to be drawn. It contains 
 * 1...n {@code QgisSymbolLayer}s in which the symbols for the contained geometry
 * are specified (e.g. points as markers with size, color, transparency, ...).
 * 
 * @author dhosse
 *
 */
public abstract class QGisRenderer {
	
	private QGisConstants.renderingType renderingType;

	private List<QGisSymbolLayer> symbolLayers;
	
	public QGisRenderer(QGisConstants.renderingType renderingType){
		
		this.setRenderingType(renderingType);
		this.symbolLayers = new ArrayList<QGisSymbolLayer>();
		
	}
	
	public QGisConstants.renderingType getRenderingType() {
		return renderingType;
	}

	public void setRenderingType(QGisConstants.renderingType renderingType) {
		this.renderingType = renderingType;
	}

	public List<QGisSymbolLayer> getSymbolLayers(){
		return this.symbolLayers;
	}
	
	public void addSymbolLayer(QGisSymbolLayer sl){
		this.symbolLayers.add(sl);
	}

	public abstract void init();
	
}
