package org.matsim.contrib.analysis.vsp;

public class RasterLayer extends QGisLayer {

	/**
	 * Instantiates a new QGis layer that contains raster data.
	 * </p>
	 * Needs a {@code QGisRasterRenderer} for rendering the contained data.
	 * 
	 * @author dhosse
	 *
	 */
	public RasterLayer(String name, String path) {
		
		super(name, path);
		this.setType(QGisConstants.layerType.raster);
		
	}

}
