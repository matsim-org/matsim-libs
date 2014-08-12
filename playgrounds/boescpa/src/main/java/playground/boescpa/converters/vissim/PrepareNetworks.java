/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.vissim;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.io.IOUtils;
import playground.boescpa.converters.vissim.tools.DefaultBaseGridCreator;
import playground.boescpa.converters.vissim.tools.DefaultNetworkMatcher;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * Creates key maps for a matsim and a visum/vissim network and writes the key maps to files.
 * Provides also a method to read the key maps back in from the files.
 *
 * @author boescpa
 */
public class PrepareNetworks {

	public static void main(String[] args) {
		String path2VissimZoneShp = args[0];
		String path2MATSimNetwork = args[1];
		String path2VissimNetworkAnm = args[2];
		String path2WriteKeyMapMatsim = args[3];
		String path2WriteKeyMapVissim = args[4];

		ConvEvents2Anm.BaseGridCreator baseGridCreator = new DefaultBaseGridCreator();
		Network mutualBaseGrid = baseGridCreator.createMutualBaseGrid(path2VissimZoneShp);

		ConvEvents2Anm.NetworkMatcher networkMatcher = new DefaultNetworkMatcher();
		HashMap<Id, Id[]> keyMsNetwork = networkMatcher.mapMsNetwork(path2MATSimNetwork, mutualBaseGrid, path2VissimZoneShp);
		HashMap<Id, Id[]> keyAmNetwork = networkMatcher.mapAmNetwork(path2VissimNetworkAnm, mutualBaseGrid);

		writeKeyMaps(keyMsNetwork, path2WriteKeyMapMatsim);
		writeKeyMaps(keyAmNetwork, path2WriteKeyMapVissim);
	}

	public static void writeKeyMaps(HashMap<Id, Id[]> keyNetwork, String path2WriteKeyMap) {
		try {
			final String header = "LinkId, ZoneIds...";
			final BufferedWriter out = IOUtils.getBufferedWriter(path2WriteKeyMap);
			out.write(header); out.newLine();
			for (Id linkId : keyNetwork.keySet()) {
				String line = linkId.toString();
				for (Id zoneId : keyNetwork.get(linkId)) {
					line = line + ", " + zoneId.toString();
				}
				out.write(line); out.newLine();
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Writing of " + path2WriteKeyMap + " failed.");
			e.printStackTrace();
		}
	}

	public static HashMap<Id, Id[]> readKeyMaps(String path2KeyMap) {
		HashMap<Id, Id[]> keyMap = new HashMap<Id, Id[]>();
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2KeyMap);
			in.readLine(); // header
			String line = in.readLine();
			while (line != null) {
				String[] keys = line.split(", ");
				Id linkId = new IdImpl(Long.parseLong(keys[0]));
				Id[] zoneId = new Id[keys.length-1];
				for (int i = 1; i < keys.length; i++) {
					zoneId[i-1] = new IdImpl(Long.parseLong(keys[i]));
				}
				keyMap.put(linkId, zoneId);
				line = in.readLine();
			}
		} catch (IOException e) {
			System.out.println("Reading of " + path2KeyMap + " failed.");
			e.printStackTrace();
		}
		return keyMap;
	}
}
