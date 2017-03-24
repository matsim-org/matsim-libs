package playground.clruch.gfx;

import org.matsim.api.core.v01.Coord;

import playground.clruch.gfx.helper.WGS84toSiouxFalls;

class Test {

    public static void main(String[] args) {
        // SFtoWGS84 asd = new SFtoWGS84();
        // Coord res;
        // res = asd.transform(new Coord(678253.4, 4831005.));
        // System.out.println(res);

        WGS84toSiouxFalls asd = new WGS84toSiouxFalls();
        Coord res;
        res = asd.transform(new Coord(43.50030884149,-96.68137192726));
        System.out.println(res);
    }

}
