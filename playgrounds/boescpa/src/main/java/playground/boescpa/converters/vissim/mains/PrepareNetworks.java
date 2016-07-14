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

package playground.boescpa.converters.vissim.mains;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.io.IOUtils;

import playground.boescpa.converters.vissim.ConvEvents;
import playground.boescpa.converters.vissim.tools.BaseGridCreator;
import playground.boescpa.converters.vissim.tools.InpNetworkMapper;
import playground.boescpa.converters.vissim.tools.MsNetworkMapper;

/**
 * Creates key maps for a matsim and a visum/vissim network and writes the key maps to files.
 * Provides also a method to read the key maps back in from the files.
 *
 * @author boescpa
 */
public class PrepareNetworks {

	private static final String delimiter = ", ";

	public static void main(String[] args) {
		String path2VissimZoneShp = args[0];
		String path2MATSimNetwork = args[1];
		String path2VissimNetworkAnm = args[2];
		String path2WriteKeyMapMatsim = args[3];
		String path2WriteKeyMapVisum = args[4];
		String path2VissimNetworkInp = args[5];
		String path2WriteKeyMapVissim = args[6];

		ConvEvents.BaseGridCreator baseGridCreator = new BaseGridCreator();
		Network mutualBaseGrid = baseGridCreator.createMutualBaseGrid(path2VissimZoneShp);

		ConvEvents.NetworkMapper msNetworkMapper = new MsNetworkMapper();
		//ConvEvents2Anm.NetworkMapper amNetworkMapper = new AmNetworkMapper();
		ConvEvents.NetworkMapper inpNetworkMapper = new InpNetworkMapper();
		HashMap<Id<Link>, Id<Node>[]> keyMsNetwork = msNetworkMapper.mapNetwork(path2MATSimNetwork, mutualBaseGrid, path2VissimZoneShp);
		//HashMap<Id, Id[]> keyAmNetwork = amNetworkMapper.mapNetwork(path2VissimNetworkAnm, mutualBaseGrid, "");
		HashMap<Id<Link>, Id<Node>[]> keyInpNetwork = inpNetworkMapper.mapNetwork(path2VissimNetworkInp, mutualBaseGrid, "");

		writeKeyMaps(keyMsNetwork, path2WriteKeyMapMatsim);
		//writeKeyMaps(keyAmNetwork, path2WriteKeyMapVisum);
		writeKeyMaps(keyInpNetwork, path2WriteKeyMapVissim);
	}

	public static void writeKeyMaps(HashMap<Id<Link>, Id<Node>[]> keyNetwork, String path2WriteKeyMap) {
		try {
			final String header = "LinkId, ZoneIds...";
			final BufferedWriter out = IOUtils.getBufferedWriter(path2WriteKeyMap);
			out.write(header); out.newLine();
			for (Id<Link> linkId : keyNetwork.keySet()) {
				String line = linkId.toString();
				for (Id<Node> zoneId : keyNetwork.get(linkId)) {
					line = line + delimiter + zoneId.toString();
				}
				out.write(line); out.newLine();
			}
			out.close();
		} catch (IOException e) {
			System.out.println("Writing of " + path2WriteKeyMap + " failed.");
			e.printStackTrace();
		}
	}

	public static HashMap<Id<Link>, Id<Node>[]> readKeyMaps(String path2KeyMap) {
		HashMap<Id<Link>, Id<Node>[]> keyMap = new HashMap<>();
		try {
			final BufferedReader in = IOUtils.getBufferedReader(path2KeyMap);
			in.readLine(); // header
			String line = in.readLine();
			while (line != null) {
				String[] keys = line.split(delimiter);
				Id<Link> linkId = Id.create(keys[0], Link.class);
				Id<Node>[] zoneId = new Id[keys.length-1];
				for (int i = 1; i < keys.length; i++) {
					zoneId[i-1] = Id.create(Long.parseLong(keys[i]), Node.class);
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
