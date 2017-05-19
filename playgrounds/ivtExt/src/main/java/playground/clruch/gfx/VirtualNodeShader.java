package playground.clruch.gfx;

public enum VirtualNodeShader {
    None, //
    VehicleCount, //
    RequestCount, //
    MeanRequestDistance, //
    MeanRequestWaiting, //
    MedianRequestWaiting, //
    MaxRequestWaiting, //
    ;

    public boolean renderBoundary() {
        return !equals(None);
    }
}
