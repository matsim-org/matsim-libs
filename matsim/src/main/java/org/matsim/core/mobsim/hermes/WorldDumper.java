package org.matsim.core.mobsim.hermes;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.api.core.v01.events.Event;

public class WorldDumper {

    public static String outputPrefix;
    private static BufferedWriter sim_events = null;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                close();
            }
        });
    }

    private static void close() {
        try {
            if (sim_events != null) {
                sim_events.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setup(String folder) {
        try {
            outputPrefix = folder + "/" + "ITERS" + "/it." + Hermes.iteration;

            close();

            sim_events = new BufferedWriter(new FileWriter(outputPrefix + "/sim_events"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void dumpRealm(Realm realm) throws Exception {
        BufferedWriter log = new BufferedWriter(new FileWriter(outputPrefix + "/hermes_realm"));
        log.write(String.format("<realm time=%d >\n", realm.time()));
        log.write("\t<delayed_links>\n");
        int time = 0;
        for (ArrayDeque<Link> links : realm.delayedLinks()) {
            for (Link link : links) {
                log.write(String.format("\t [time = %d] link %d\n", time, link.id()));
            }
            time++;
        }
        log.write("\t</delayed_links>\n");
        log.write("\t<delayed_agents>\n");
        time = 0;
        for (ArrayDeque<Agent> activity : realm.delayedAgents()) {
            for (Agent agent : activity) {
                log.write(String.format("\t [time = %d] agent %d\n", time, agent.id()));
            }
            time++;
        }
        log.write("\t</delayed_agents>\n");
        // TODO - print agents in stops?
        log.write("</realm>\n");
        log.close();
    }

    public static void dumpLinks(Link[] links) throws Exception {
        BufferedWriter log = new BufferedWriter(new FileWriter(outputPrefix + "/hermes_links"));
        log.write("<links>\n");
        for (Link link : links) {
            log.write(String.format("\t\t<link id=%d length=%d velocity=%d capacity=%d>\n",
                link.id(), link.length(), link.velocity(),
                link.capacity()));
            log.write("\t\t\t<agents>\n");
            for (Agent a : link.queue()) {
                log.write(String.format("\t\t\t\t%d \n", a.id()));
            }
            log.write("\t\t\t</agents>\n");
        }
        log.write("</links>\n");
        log.close();
    }

    public static void dumpAgents(Agent[] agents) throws Exception {
        BufferedWriter log = new BufferedWriter(new FileWriter(outputPrefix + "/hermes_agents"));
        log.write("<agents>\n");
        for (Agent agent : agents) {
        	if (agent == null) {
        		continue;
        	}
            log.write(String.format("\t<agent id=%d linkFinishTime=%d planIndex=%d capacity=%d>\n",
                agent.id(), agent.linkFinishTime(), agent.planIndex(), agent.capacity()));
            log.write("\t<plan>\n");
            for (int i = 0; i < agent.plan().size(); i++) {
                log.write(String.format("\t\t%s\n", Agent.toString(agent.plan().get(i))));
            }
            log.write("\t</plan>\n");
        }
        log.write("</agents>\n");
        log.close();
    }

    public static void dumpEvent(Event event) {
        try {
            sim_events.write(String.format("%s\n", event.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
