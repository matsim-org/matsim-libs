package org.matsim.contrib.emissions;

import org.matsim.api.core.v01.network.Link;

public class VspHbefaRoadTypeMapping extends HbefaRoadTypeMapping {

    // have this here, since the existing mappers have a build method as well.
    public static HbefaRoadTypeMapping build() {
        return new VspHbefaRoadTypeMapping();
    }

    public VspHbefaRoadTypeMapping() {
    }

    @Override
    protected String determineHbefaType(Link link) {

        var freespeed = link.getFreespeed() <= 13.888889 ? link.getFreespeed() * 2 : link.getFreespeed();

        if (freespeed <= 8.333333333) { //30kmh
            return "URB/Access/30";
        } else if (freespeed <= 11.111111111) { //40kmh
            return "URB/Access/40";
        } else if (freespeed <= 13.888888889) { //50kmh
            double lanes = link.getNumberOfLanes();
            if (lanes <= 1.0) {
                return "URB/Local/50";
            } else if (lanes <= 2.0) {
                return "URB/Distr/50";
            } else if (lanes > 2.0) {
                return "URB/Trunk-City/50";
            } else {
                throw new RuntimeException("NoOfLanes not properly defined");
            }
        } else if (freespeed <= 16.666666667) { //60kmh
            double lanes = link.getNumberOfLanes();
            if (lanes <= 1.0) {
                return "URB/Local/60";
            } else if (lanes <= 2.0) {
                return "URB/Trunk-City/60";
            } else if (lanes > 2.0) {
                return "URB/MW-City/60";
            } else {
                throw new RuntimeException("NoOfLanes not properly defined");
            }
        } else if (freespeed <= 19.444444444) { //70kmh
            return "URB/MW-City/70";
        } else if (freespeed <= 22.222222222) { //80kmh
            return "URB/MW-Nat./80";
        } else if (freespeed > 22.222222222) { //faster
            return "RUR/MW/>130";
        } else {
            throw new RuntimeException("No mapping specified for links with freespeed: " + link.getFreespeed() + " and " + link.getNumberOfLanes() + " lanes");
        }
    }
}
