/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.vsp.flowEfficiency;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.ResponsePath;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class sets link attributes such that information on turning direction from each link to each possible outlink can be obtained.
 * This is done by converting Graphhopper routing instructions.
 *
 * @author rakow, tschlenther
 */
public class LinkTurnDirectionAttributesFromGraphHopper implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(LinkTurnDirectionAttributesFromGraphHopper.class);

    /**
     * Path to osm network.
     */
    private Path osm;

    /**
     * Path to MATSim network.
     */
    private Path input;

    /**
     * Graphhopper instance.
     */
    private GraphHopper hopper;

    /**
     * Transform MATSim to OSM network.
     */
    private CoordinateTransformation ct;

    /**
     * the coordinate reference system for the input network
     */
    private String networkCrs;

    /**
     * Output debug information for these links.
     */
    private Set<Id<Link>> debug = Set.of(
            Id.createLinkId("debugLink")
    );


    enum TurnDirection {ERROR, UNKNOWN, STRAIGHT, UTURN, LEFT, RIGHT}

    public LinkTurnDirectionAttributesFromGraphHopper(String osmFilePath) {
        this.osm = Path.of(osmFilePath);
    }

    private LinkTurnDirectionAttributesFromGraphHopper(String osmFilePath, String matsimNetworkFilePath, String networkCrs) {
        this.osm = Path.of(osmFilePath);
        this.input = Path.of(matsimNetworkFilePath);
        this.networkCrs = networkCrs;
    }

    public static void main(String[] args) throws Exception {
        LinkTurnDirectionAttributesFromGraphHopper main = new LinkTurnDirectionAttributesFromGraphHopper(args[0], args[1], args[2]);
        main.call();
    }

    @Override
    public Integer call() throws Exception {
       Network network = assignLinkTurnAttributes(input, networkCrs);
       String output =  input.toString().replace(".xml.gz", "-with-turns.xml.gz");
       NetworkUtils.writeNetwork(network,output);
       log.info("Done, writing to {}", output);

        return 0;
    }


    public Network assignLinkTurnAttributes(Path input, String networkCrs){
        Network network = NetworkUtils.readNetwork(input.toString());
        return assignLinkTurnAttributes(network, networkCrs);
    }

    public Network assignLinkTurnAttributes(Network network, String networkCrs){
        log.info("start assigning turn directions to links..");

        hopper = createGraphHopperInstance(osm.toString());
        ct = new GeotoolsTransformation(networkCrs, "EPSG:4326");
        AtomicInteger i = new AtomicInteger(0);

        network.getLinks().values().parallelStream().forEach(link -> {

            Map<String, String> map = new HashMap<>();

            // path from link to out
            for (Link out : link.getToNode().getOutLinks().values()) {

                TurnDirection result = routing(link, out);
                if (result != TurnDirection.ERROR) {
                    map.put(out.getId().toString(), result.toString());
                }
            }

            if (!map.isEmpty())
                link.getAttributes().putAttribute("turns", map);

            int n = i.incrementAndGet();
            if (n % 10000 == 0)
                log.info("Processed {} out of {} links", n, network.getLinks().size());

        });

        return network;
    }


    private static GraphHopper createGraphHopperInstance(String ghLoc) {
        GraphHopper hopper = new GraphHopper();
        hopper.setGraphHopperLocation(ghLoc);
        // specify where to store graphhopper files
        hopper.setGraphHopperLocation("target/routing-graph-cache");

        // see docs/core/profiles.md to learn more about profiles
		//fixme: "Graphhopper profiles have changed"
        //hopper.setProfiles(new Profile("car").setCustomModel(new CustomModel()).setWeighting("fastest").setTurnCosts(false));

        // this enables speed mode for the profile we called car
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car"));
        // explicitly allow that the calling code can disable this speed mode
        // NOTE: disabling_allowed config options were removed for GH 3.0, michalm, may'22
        // hopper.getRouterConfig().setCHDisablingAllowed(true);

        // now this can take minutes if it imports or a few seconds for loading of course this is dependent on the area you import
        hopper.importOrLoad();
        return hopper;
    }

    private TurnDirection routing(Link fromLink, Link toLink) {

        Link opposite = NetworkUtils.findLinkInOppositeDirection(fromLink);
        if(opposite != null && opposite.equals(toLink) ){
            return TurnDirection.UTURN;
        }

        Coord from = ct.transform(getRoutingCoordinate(fromLink, 0.75));
        Coord to = ct.transform(getRoutingCoordinate(toLink, 0.25));

        GHRequest req = new GHRequest(
                from.getY(), from.getX(),
                to.getY(), to.getX()
        )
                .setProfile("car")
                // define the language for the turn instructions
                .setLocale(Locale.US);

        GHResponse rsp = hopper.route(req);

        // handle errors
        if (rsp.hasErrors()) {
            return TurnDirection.ERROR;
        }

        // use the best path, see the GHResponse class for more possibilities.
        ResponsePath path = rsp.getBest();

        if (debug.contains(fromLink.getId())) {
            System.out.println("### FROM " + fromLink.getId());
            System.out.println("### TO " + toLink.getId() + " = " +  guessDirection(path.getInstructions()));
            System.out.println("\\/\\/\\/\\/\\/\\/\\/");

            for (Instruction inst : path.getInstructions()) {
                String msg = inst.getTurnDescription(path.getInstructions().getTr());
                System.out.println(msg);
            }

            System.out.println("###############");
        }

        return guessDirection(path.getInstructions());
    }

    /**
     * Retrieve coordinate used for routing given a link.
     * The coordinate is scaled between from and to node and given factor.
     */
    private Coord getRoutingCoordinate(Link link, double scale) {

        Coord from = link.getFromNode().getCoord();
        Coord to = link.getToNode().getCoord();

        return new Coord(
                from.getX() + scale * (to.getX() - from.getX()),
                from.getY() + scale * (to.getY() - from.getY())
        );
    }

    /**
     * Guess direction from instructions.
     */
    private TurnDirection guessDirection(InstructionList inst) {

        if (inst.isEmpty())
            return TurnDirection.STRAIGHT;

        boolean hasLeft = inst.stream().mapToInt(Instruction::getSign).anyMatch(s -> s == Instruction.TURN_LEFT || s == Instruction.TURN_SHARP_LEFT);
        boolean hasRight = inst.stream().mapToInt(Instruction::getSign).anyMatch(s -> s == Instruction.TURN_RIGHT || s == Instruction.TURN_SHARP_RIGHT);
        boolean hasRoundabout = inst.stream().mapToInt(Instruction::getSign).anyMatch(s -> s == Instruction.LEAVE_ROUNDABOUT || s == Instruction.USE_ROUNDABOUT);

        //we do deliberately not check for u-turns as the deviation in precision of the MATSim and the OSM network is too big. Most U-Turn instructions consist
        // of one or more turn instructions anyways, that way we should at least end up and not declare them as STRAIGHT.

        // conflicting directions
        if (hasLeft && hasRight)
            return TurnDirection.UNKNOWN;
        else if (hasLeft)
            return TurnDirection.LEFT;
        else if (hasRight || hasRoundabout)
            return TurnDirection.RIGHT;

        return TurnDirection.STRAIGHT;
    }
}
