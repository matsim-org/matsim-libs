package org.matsim.contrib.pseudosimulation.distributed;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TimeBinUtils;
import org.matsim.vehicles.Vehicle;

public class SerializableLinkTravelTimes implements Serializable, TravelTime {

	private final double[][] times;
	private final Map<String, Integer> indices = new HashMap<>();
	private transient int[] rowsByIdIndex;
	private final double travelTimeBinSize;
	private final int endTime;

	public SerializableLinkTravelTimes(TravelTime linkTravelTimes,
			double traveltimeBinSize, int endTime,
			Collection<? extends Link> links) {
		this.travelTimeBinSize = traveltimeBinSize;
		this.endTime = endTime;
		endTime = endTime <= 0 ? 86400 : endTime;
		times = new double[links.size()][TimeBinUtils.getTimeBinCount(endTime, traveltimeBinSize)];
		rowsByIdIndex = createRowIndex(links.stream().map(Link::getId).toList());
		Iterator<? extends Link> iterator = links.iterator();
		for (int i = 0; i < times.length; i++) {
			Link link = iterator.next();
			indices.put(link.getId().toString(), i);
			rowsByIdIndex[link.getId().index()] = i;
			for (int j = 0; j < times[i].length; j++)
				times[i][j] = linkTravelTimes.getLinkTravelTime(link,
						traveltimeBinSize * j, null, null);
		}
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person,
			Vehicle vehicle) {
		time = time % 86400;
		try {
			int idIndex = link.getId().index();
			int row = idIndex < rowsByIdIndex.length ? rowsByIdIndex[idIndex] : -1;
			if (row < 0) {
				throw new NullPointerException("Unknown link " + link.getId());
			}
			return times[row][TimeBinUtils.getTimeBinIndex(time, travelTimeBinSize, endTime)];
		} catch (ArrayIndexOutOfBoundsException e) {
			// Preserve the legacy fallback to the normalized query time.
			e.printStackTrace();
		}
		return time;
	}

	private void readObject(ObjectInputStream input) throws IOException, ClassNotFoundException {
		input.defaultReadObject();
		List<Id<Link>> linkIds = indices.keySet().stream().map(Id::createLinkId).toList();
		rowsByIdIndex = createRowIndex(linkIds);
		for (Map.Entry<String, Integer> entry : indices.entrySet()) {
			rowsByIdIndex[Id.createLinkId(entry.getKey()).index()] = entry.getValue();
		}
	}

	private static int[] createRowIndex(Collection<Id<Link>> linkIds) {
		int maximumIndex = linkIds.stream().mapToInt(Id::index).max().orElse(-1);
		int[] rowIndex = new int[maximumIndex + 1];
		Arrays.fill(rowIndex, -1);
		return rowIndex;
	}

}
