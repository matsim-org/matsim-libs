// code by jph
package playground.clruch.gfx;

/* package */ enum VirtualNodeShader {
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
