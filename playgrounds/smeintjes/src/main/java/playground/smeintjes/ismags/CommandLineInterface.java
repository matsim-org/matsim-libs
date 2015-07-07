package playground.smeintjes.ismags;

/* 
 * Copyright (C) 2013 Maarten Houbraken
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Software available at https://github.com/mhoubraken/ISMAGS
 * Author : Maarten Houbraken (maarten.houbraken@intec.ugent.be)
 */

import playground.smeintjes.algorithm.MotifFinder;
import java.io.*;
import java.util.*;
import playground.smeintjes.motifs.Motif;
import playground.smeintjes.motifs.MotifInstance;
import org.apache.commons.cli.*;
import playground.smeintjes.network.*;

public class CommandLineInterface {

    public static boolean print = true;

    public static void main(String[] args) throws IOException {
        String folder = null, files = null, motifspec = null, output = null;

        Options opts = new Options();
        opts.addOption("folder", true, "Folder name");
        opts.addOption("linkfiles", true, "Link files seperated by spaces (format: linktype[char] directed[d/u] filename)");
        opts.addOption("motif", true, "Motif description by two strings (format: linktypes)");
        opts.addOption("output", true, "Output file name");

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(opts, args);
            if (cmd.hasOption("folder")) {
                folder = cmd.getOptionValue("folder");
            }
            if (cmd.hasOption("linkfiles")) {
                files = cmd.getOptionValue("linkfiles");
            }
            if (cmd.hasOption("motif")) {
                motifspec = cmd.getOptionValue("motif");
            }
            if (cmd.hasOption("output")) {
                output = cmd.getOptionValue("output");
            }
        } catch (ParseException e) {
            Die("Error: Parsing error");
        }

        if (print) {
            printBanner(folder, files, motifspec, output);
        }

        if (folder == null || files == null || motifspec == null || output == null) {
            Die("Error: not all options are provided");
        } else {
            ArrayList<String> linkfiles = new ArrayList<String>();
            ArrayList<String> linkTypes = new ArrayList<String>();
            ArrayList<String> sourcenetworks = new ArrayList<String>();
            ArrayList<String> destinationnetworks = new ArrayList<String>();
            ArrayList<Boolean> directed = new ArrayList<Boolean>();
            StringTokenizer st = new StringTokenizer(files, " ");
            while (st.hasMoreTokens()) {
                linkTypes.add(st.nextToken());
                directed.add(st.nextToken().equals("d"));
                sourcenetworks.add(st.nextToken());
                destinationnetworks.add(st.nextToken());
                linkfiles.add(folder + st.nextToken());
            }
            ArrayList<LinkType> allLinkTypes = new ArrayList<LinkType>();
            HashMap<Character, LinkType> typeTranslation = new HashMap<Character, LinkType>();
            for (int i = 0; i < linkTypes.size(); i++) {
                String n = linkTypes.get(i);
                char nn = n.charAt(0);
                LinkType t = typeTranslation.get(nn);
                if (t == null) {
                    t = new LinkType(directed.get(i), n, i, nn, sourcenetworks.get(i), destinationnetworks.get(i));
                }
                allLinkTypes.add(t);
                typeTranslation.put(nn, t);
            }
            if (print) {
                System.out.println("Reading network..");
            }
            Network network = NetworkReader.readNetworkFromFiles(linkfiles, allLinkTypes);


            Motif motif = getMotif(motifspec, typeTranslation);

            if (print) {
                System.out.println("Starting the search..");
            }
            MotifFinder mf = new MotifFinder(network);
            Set<MotifInstance> motifs = mf.findMotif(motif);
            if (print) {
                System.out.println("Completed search");
            }
            if(print) {
                System.out.println("Found " + motifs.size() + " instances of " + motifspec + " motif");
            }
            if (print) {
                System.out.println("Writing instances to file: " + output);
            }
            printMotifs(motifs, output);
            if (print) {
                System.out.println("Done.");
            }
//            Set<MotifInstance> motifs=null;
//            MotifFinder mf=null;
//            System.out.println("Starting the search..");
//            long tstart = System.nanoTime();
//            for (int i = 0; i < it; i++) {
//
//                mf = new MotifFinder(network, allLinkTypes, true);
//                motifs = mf.findMotif(motif);
//            }
//
//            long tend = System.nanoTime();
//            double time_in_ms = (tend - tstart) / 1000000.0;
//            System.out.println("Found " + mf.totalFound + " motifs, " + time_in_ms + " ms");
////        System.out.println("Evaluated " + mf.totalNrMappedNodes+ " search nodes");
////        System.out.println("Found " + motifs.size() + " motifs, " + time_in_ms + " ms");
//            printMotifs(motifs, output);

        }

    }

    public static void Die(String msg) {
        System.out.println(msg);
        System.exit(1);
    }

    public static void printBanner(String folder, String files, String motifspec, String output) {
        System.out.println("");
        System.out.println("The Index-based Subgraph Matching Algorithm with General Symmetries");
        System.out.println("--------------------------------------------------------------");
        System.out.println("Version 1.0");
        System.out.println("Copyright (c) 2013-2014 Maarten Houbraken");
        System.out.println("");
        System.out.println("--------------------------------------------------------------\n"
                + "folder\t\tFolder name\n"
                + "linkfiles\tLink files separated by spaces \n\t\t(format: linktype[char] directed[d/u] filename)\n"
                + "motif\t\tMotif description by two strings \n\t\t(format: linktypes directed)\n"
                + "output\t\tOutput file name\n"
                + "--------------------------------------------------------------\n"
                + "folder\t\t" + folder + "\n"
                + "linkfiles\t" + files + "\n"
                + "motif\t\t" + motifspec + "\n"
                + "output\t\t" + output + "\n");
    }

    public static Motif getMotif(String motifspec, HashMap<Character, LinkType> typeTranslation) {
        int l = motifspec.length();
        int nrNodes = (int) Math.ceil(Math.sqrt(2 * l));
        int l2 = nrNodes * (nrNodes - 1) / 2;
        if (l != l2) {
            Die("Error: motif \"" + motifspec + "\" has invalid length");
        }
        int counter = 0;
        Motif m = new Motif(nrNodes);
        for (int i = 1; i < nrNodes; i++) {
            for (int j = 0; j < i; j++) {
//                System.out.println("("+(1+i)+","+(1+j)+")");
                char c = motifspec.charAt(counter);
                counter++;
                if (c == '0') {
                    continue;
                }
                LinkType lt = typeTranslation.get(Character.toUpperCase(c));
                if (Character.isUpperCase(c)) {
                    m.addMotifLink(j, i, lt);
                } else {
                    m.addMotifLink(i, j, lt);
                }
            }
        }
        m.finaliseMotif();
        return m;
    }

    private static void printMotifs(Set<MotifInstance> motifs, String output) throws IOException {
        PrintWriter out = new PrintWriter(new File(output));
        for (MotifInstance m : motifs) {
            out.println(m);
        }
        out.close();
    }
}
