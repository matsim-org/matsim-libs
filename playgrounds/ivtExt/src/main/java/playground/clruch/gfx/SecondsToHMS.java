// code by jph
package playground.clruch.gfx;

/* package */ class SecondsToHMS {

    public final int h;
    public final int m;
    public final int s;

    public SecondsToHMS(double now) {
        int round = (int) Math.floor(now);
        h = round / 3600;
        round -= h * 3600;
        m = round / 60;
        round -= m * 60;
        s = round;
    }

    public String toDigitalWatch() {
        return String.format("%02d:%02d:%02d", h, m, s);
    }

}
