package playground.dhosse.qgis;

public class RasterLayer extends QGisLayer {

	public RasterLayer(String name, String path) {
		
		super(name, path);
		this.setType(QGisConstants.layerType.raster);
		
	}

}
