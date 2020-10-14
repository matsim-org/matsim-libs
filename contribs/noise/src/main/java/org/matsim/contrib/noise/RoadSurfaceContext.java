package org.matsim.contrib.noise;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Network;

class RoadSurfaceContext {

    static final String ROAD_SURFACE = "roadSurface";
    private final Network network;

    enum SurfaceType {
        /**
         * "nicht geriffelter Gussasphalt"
         */
        paved {
            @Override
            double getCorrection(RLS19VehicleType type, double v) {
                return 0;
            }
        },
        /**
         * "sonstiges Pflaster"
         */
        cobbleStone {
            @Override
            double getCorrection(RLS19VehicleType type, double v) {
                if(v >= 50) {
                    return 7;
                } else if( v >= 40) {
                    return 6;
                } else {
                    return 5;
                }
            }
        },
        /**
         * "Pflaster mit ebener Oberflaeche"
         */
        smoothCobbleStone {
            @Override
            double getCorrection(RLS19VehicleType type, double v) {
                if(v >= 50) {
                    return 3;
                } else if( v >= 40) {
                    return 2;
                } else {
                    return 1;
                }
            }
        };

        abstract double getCorrection(RLS19VehicleType type, double v);
    }

    @Inject
    RoadSurfaceContext(Network network) {
        this.network = network;
    }

    double calculateSurfaceCorrection(RLS19VehicleType type, NoiseLink link, double velocity) {
        SurfaceType surfaceType = SurfaceType.paved;
//        final Object surface = network.getLinks().get(link.getId()).getAttributes().getAttribute(ROAD_SURFACE);
//        if(surface != null) {
//            surfaceType = SurfaceType.valueOf((String) surface);
//        }
        return surfaceType.getCorrection(type, velocity);
    }
}