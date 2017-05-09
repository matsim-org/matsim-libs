package playground.clruch.gfx;

import org.matsim.api.core.v01.Coord;

public enum LocationSpec {
    SIOUXFALLS_CITY( //
            ReferenceFrame.SIOUXFALLS, //
            new Coord(678365.311581, 4827050.237694), //
            50000.0), //
    ZURICH_CITY( //
            ReferenceFrame.SWITZERLAND, //
            new Coord(2683600.0, 1251400.0), //
            10000.0), //
    BASEL_CITY( //
            ReferenceFrame.SWITZERLAND, //
            new Coord(2612859.0, 1266343.0), //
            12000.0), //
    BASEL_REGION( //
            ReferenceFrame.SWITZERLAND, //
            new Coord(2612859.0, 1266343.0), //
            1e10), //
    ;

    public final ReferenceFrame referenceFrame;
    // increasing the first value goes right        
    // increasing the second value goes north
    public final Coord center;
    // TODO currently the criterion is "radius < 0"
    // when radius == null, the radius is unknown, and the scenario is as-is
    public final Double radius;

    private LocationSpec(ReferenceFrame referenceFrame, Coord center, Double radius) {
        this.referenceFrame = referenceFrame;
        this.center = center;
        this.radius = radius;
    }
}
