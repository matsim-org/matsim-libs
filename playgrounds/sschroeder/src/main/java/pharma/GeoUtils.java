package pharma;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * Created by schroeder on 02/11/15.
 */
public class GeoUtils {

    private static GeotoolsTransformation transformation = new GeotoolsTransformation(TransformationFactory.WGS84,TransformationFactory.DHDN_GK4);

    public static Coord transform(double lon, double lat){
        return transformation.transform(new Coord(lon,lat));
    }
}
