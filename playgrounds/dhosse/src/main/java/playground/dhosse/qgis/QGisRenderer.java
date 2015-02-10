package playground.dhosse.qgis;

import java.util.ArrayList;
import java.util.List;

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
