/* *********************************************************************** *
 * project: org.matsim.*
 * CountsConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.counts;

import org.matsim.core.config.Module;

import playground.thibautd.initialdemandgeneration.counts.TimeFilter.Day;
import playground.thibautd.initialdemandgeneration.counts.TimeFilter.DayFilter;

/**
 * Config group to allow easy modification of the parameters
 * @author thibautd
 */
public class CountsConfigGroup extends Module {
	private static final long serialVersionUID = 1L;

	public static final String NAME = "countsGeneration";

	private static final String DATASETS = "datasetsListFile";
	private static final String NETMAP = "networkMappingFile";
	private static final String OUT = "outputDir";
	private static final String START_DAY = "startDay";
	private static final String END_DAY = "endDay";
	private static final String OUTLIERS = "removeOutliers";
	private static final String HOLIDAY = "removeSummerHolidays";
	private static final String XMAS = "removeXmasDays";
	private static final String ZERO_VOLUME = "removeZeroVolumes";

	private String datasetsListFile = null;
	private String networkMappingFile = null;
	private String outputDir = null;
	private Day startDay = Day.MONDAY;
	private Day endDay = Day.FRIDAY;
	private DayFilter dayFilter = new DayFilter( startDay , endDay );
	private boolean removeOutliers = false;
	private boolean removeSummerHolidays = false;
	private boolean removeXmasDays = true;
	private boolean removeZeroVolumes = true;

	public CountsConfigGroup() {
		super( NAME );
	}

	@Override
	public void addParam(
			final String name,
			final String value) {
		if ( name.equals( DATASETS ) ) {
			datasetsListFile = value;
		}
		else if ( name.equals( NETMAP ) ) {
			networkMappingFile = value;
		}
		else if ( name.equals( OUT ) ) {
			outputDir = value;
		}
		else if ( name.equals( START_DAY ) ) {
			startDay = getDay( value );
			dayFilter = new DayFilter( startDay , endDay );
		}
		else if ( name.equals( END_DAY ) ) {
			endDay = getDay( value );
			dayFilter = new DayFilter( startDay , endDay );
		}
		else if ( name.equals( OUTLIERS ) ) {
			removeOutliers = Boolean.parseBoolean( value );
		}
		else if ( name.equals( HOLIDAY ) ) {
			removeSummerHolidays = Boolean.parseBoolean( value );
		}
		else if ( name.equals( XMAS ) ) {
			removeXmasDays = Boolean.parseBoolean( value );
		}
		else if ( name.equals( ZERO_VOLUME ) ) {
			removeZeroVolumes = Boolean.parseBoolean( value );
		}
	}

	private Day getDay( final String day ) {
		if ( day.matches( "mon.*" ) ) return Day.MONDAY;
		if ( day.matches( "tue.*" ) ) return Day.TUESDAY;
		if ( day.matches( "wed.*" ) ) return Day.WEDNESDAY;
		if ( day.matches( "thu.*" ) ) return Day.THURSDAY;
		if ( day.matches( "fri.*" ) ) return Day.FRIDAY;
		if ( day.matches( "sat.*" ) ) return Day.SATURDAY;
		if ( day.matches( "sun.*" ) ) return Day.SUNDAY;
		throw new IllegalArgumentException( "invalid day "+day );
	}

	@Override
	public String getValue(final String name) {
		if ( name.equals( DATASETS ) ) {
			return datasetsListFile;
		}
		else if ( name.equals( NETMAP ) ) {
			return networkMappingFile;
		}
		else if ( name.equals( OUT ) ) {
			return outputDir;
		}
		else if ( name.equals( START_DAY ) ) {
			return startDay.toString().toLowerCase();
		}
		else if ( name.equals( END_DAY ) ) {
			return endDay.toString().toLowerCase();
		}
		else if ( name.equals( OUTLIERS ) ) {
			return ""+removeOutliers;
		}
		else if ( name.equals( HOLIDAY ) ) {
			return ""+removeSummerHolidays;
		}
		else if ( name.equals( XMAS ) ) {
			return ""+removeXmasDays;
		}
		else if ( name.equals( ZERO_VOLUME ) ) {
			return ""+removeZeroVolumes;
		}

		throw new IllegalArgumentException( "unknown parameter "+name );
	}

	// /////////////////////////////////////////////////////////////////////////
	// specific getters
	// /////////////////////////////////////////////////////////////////////////
	public String getDatasetsListFile() {
		return datasetsListFile;
	}
	public String getNetworkMappingFile() {
		return networkMappingFile;
	}
	public String getOutputDir() {
		return outputDir;
	}
	public boolean getRemoveOutliers() {
		 return removeOutliers;
	}
	public boolean getRemoveSummerHolidays() {
		return removeSummerHolidays;
	}
	public boolean getRemoveXmasDays() {
		 return removeXmasDays;
	}
	public boolean getRemoveZeroVolumes() {
		 return removeZeroVolumes;
	}
	public DayFilter getDayFilter() {
		return dayFilter;
	}
}
