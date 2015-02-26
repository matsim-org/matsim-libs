package playground.dhosse.qgis.rendering;

import java.util.ArrayList;
import java.util.List;

import playground.dhosse.qgis.QGisConstants;
import playground.dhosse.qgis.QGisSymbolLayer;

/**
 * 
 * This class provides minimum functionality for rendering a qgis symbol layer.
 * Can be extended by use cases that use a singleSymbolRenderer.
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
	
}
