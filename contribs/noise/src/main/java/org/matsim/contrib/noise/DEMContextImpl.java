package org.matsim.contrib.noise;

import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;
import org.locationtech.jts.geom.Point;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

import jakarta.inject.Inject;
import java.io.File;
import java.io.IOException;

/**
 * @author nkuehnel
 */
public class DEMContextImpl implements DEMContext {

    private GridCoverage2D coverage;

    @Inject
    DEMContextImpl(Config config) {
        NoiseConfigGroup noiseParams = ConfigUtils.addOrGetModule(config, NoiseConfigGroup.class);
        if (noiseParams.isUseDEM()) {
            File file = new File(noiseParams.getDEMFile());
            try {
                GeoTiffReader reader = new GeoTiffReader(file, new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));
                coverage = reader.read(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public float getElevation(Position point) {
        float[] sample =  (float[])coverage.evaluate(point);
        return sample[0];
    }
}
