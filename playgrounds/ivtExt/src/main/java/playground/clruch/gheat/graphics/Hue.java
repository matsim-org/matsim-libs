// code from Computer Graphics, by Donald Hearn and Pauline Baker
// adapted by jph
package playground.clruch.gheat.graphics;

import java.awt.Color;

public class Hue {
    public static final double red = 0;
    public static final double green = 1 / 3.;
    public static final double blue = 2 / 3.;
    // ---
    /** in [0,1] */
    public final double h;
    /** in [0,1] */
    public final double s;
    /** in [0,1] */
    public final double v;
    /** in [0,1] */
    public final double a;
    public final Color rgba;

    /**
     * @param h
     *            is periodically mapped to [0, 1)
     * @param s
     *            in [0, 1]
     * @param v
     *            in [0, 1]
     * @param a
     *            in [0, 1]
     */
    public Hue(double h, double s, double v, double a) {
        this.h = h;
        this.s = s;
        this.v = v;
        this.a = a;
        final double r;
        final double g;
        final double b;
        // ---
        if (s == 0) {
            r = g = b = v;
        } else {
            h %= 1;
            if (h < 0)
                h += 1;
            h *= 6;
            int i = (int) h;
            double f = h - i;
            double aa = v * (1 - s);
            double bb = v * (1 - s * f);
            double cc = v * (1 - s * (1 - f));
            switch (i) {
            case 0:
                r = v;
                g = cc;
                b = aa;
                break;
            case 1:
                r = bb;
                g = v;
                b = aa;
                break;
            case 2:
                r = aa;
                g = v;
                b = cc;
                break;
            case 3:
                r = aa;
                g = bb;
                b = v;
                break;
            case 4:
                r = cc;
                g = aa;
                b = v;
                break;
            case 5:
            default:
                r = v;
                g = aa;
                b = bb;
                break;
            }
        }
        rgba = new Color((float) r, (float) g, (float) b, (float) a);
    }

    private static final double fraction1o255 = 1. / 255.;

    public static Hue fromColor(final Color myColor) {
        int r = myColor.getRed();
        int g = myColor.getGreen();
        int b = myColor.getBlue();
        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));
        double del = max - min;
        final double s = max == 0 ? 0 : del / max;
        double h;
        if (s == 0) {
            h = 0;
        } else {
            if (r == max) {
                int dif = g - b;
                h = (dif < 0 ? 6 : 0) + dif / del;
            } else //
            if (g == max)
                h = 2 + (b - r) / del;
            else
                // b == max
                h = 4 + (r - g) / del;
            h /= 6;
        }
        return new Hue(h, s, max * fraction1o255, myColor.getAlpha() * fraction1o255);
    }

    @Override
    public String toString() {
        return "(" + h + "," + s + "," + v + "," + a + ")";
    }

    public String toFriendlyString() {
        return String.format("(%5.3f,%5.3f,%5.3f,%5.3f)", h, s, v, a);
    }

    public static void main(String[] args) {
        Hue hue = fromColor(Color.WHITE);
        System.out.println(hue);
    }
}
