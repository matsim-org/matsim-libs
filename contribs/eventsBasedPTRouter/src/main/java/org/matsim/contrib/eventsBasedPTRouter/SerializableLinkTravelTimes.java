package org.matsim.contrib.eventsBasedPTRouter;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class SerializableLinkTravelTimes implements Serializable, TravelTime {

	private final double[][] times;
	private final Map<String, Integer> indices = new HashMap<>();
	private final int travelTimeBinSize;

	public SerializableLinkTravelTimes(TravelTime linkTravelTimes,
			int traveltimeBinSize, double endTime,
			Collection<? extends Link> links) {
		this.travelTimeBinSize = traveltimeBinSize;
		endTime = endTime <= 0 ? 86400 : endTime;
		times = new double[links.size()][(int) (endTime / traveltimeBinSize)];
		Iterator<? extends Link> iterator = links.iterator();
		for (int i = 0; i < times.length; i++) {
			Link link = iterator.next();
			indices.put(link.getId().toString(), i);
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
			return times[indices.get(link.getId().toString())][(int) (time / travelTimeBinSize)];
		} catch (ArrayIndexOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return time;
	}

}
