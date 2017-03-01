/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.michalm.poznan.demand.kbr;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

public class PoznanChainsProcessor {
	private Map<String, MutableInt> chainToOccurenceMap = new HashMap<>();
	private Sheet currentSheet;

	private void processFile(String xlsFile) {
		try (InputStream inp = new FileInputStream(xlsFile); Workbook wb = new HSSFWorkbook(inp)) {
			currentSheet = wb.getSheet("Arkusz1");

			int lastSurvey = 0;
			int lastRespondent = 0;
			int lastTrip = 0;
			int lastGoal = 0;
			int lastHour = 0;

			StringBuilder currentChain = new StringBuilder();

			for (int i = 1; i < 45393; i++) {
				int survey = getInt(i, 0, true);
				int respondent = getInt(i, 1, true);
				int trip = getInt(i, 2, false);
				int goal = getInt(i, 3, false);
				int hour = getInt(i, 4, false);

				if (lastSurvey < survey) {
					lastSurvey = survey;
					lastRespondent = -1;
				} else if (lastSurvey > survey) {
					error(i, 0, "last survey > current survey");
				}

				if (lastRespondent < respondent) {
					registerChain(currentChain);

					lastRespondent = respondent;
					lastTrip = 0;
					lastGoal = 0;
					lastHour = 0;

					currentChain = new StringBuilder();
				} else if (lastRespondent > respondent) {
					error(i, 1, "last respondent > current respondent");
				}

				if (trip == -1 && lastTrip > 0) {
					trip = lastTrip;// transfer
				}

				if (trip >= lastTrip) {
					if (trip > lastTrip + 1) {
						warning(i, 2, "last trip and current trip do not fit");
					}

					if (trip == lastTrip) {// possibly a transfer???
						if (goal != -1 && goal != lastGoal) {
							// treat this as a new trip!!!
							warning(i, 3, "motivation different than before transfer");
						} else {
							continue;// just a transfer
						}
					}

					lastTrip = trip;
					lastGoal = goal;
					lastHour = hour;
					currentChain.append(convertGoalNumberToChar(goal));

					if (lastHour != -1 && hour != -1 && lastHour > hour) {
						warning(i, 4, "last hour > current hour");

					}
				} else {
					error(i, 2, "last trip > current trip");
				}
			}

			if (currentChain.length() > 0) {
				registerChain(currentChain);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private int getInt(int row, int col, boolean obligatory) {
		Row rowObj = currentSheet.getRow(row);
		Cell cell = rowObj.getCell(col);

		if (cell == null) {
			if (obligatory) {
				error(row, col, "null");
			}
			return -1;
		}

		switch (cell.getCellType()) {
			case Cell.CELL_TYPE_BLANK:
				if (obligatory) {
					error(row, col, "blank");
				}
				return -1;
			case Cell.CELL_TYPE_NUMERIC:
				double v = cell.getNumericCellValue();

				if (v != Math.floor(v)) {
					error(row, col, "not integer");
				}

				return (int)v;
			default:
				error(row, col, "not a number");
				return -1;
		}
	}

	private void error(int row, int col, String cause) {
		warning(row, col, cause);
		throw new RuntimeException();
	}

	private void warning(int row, int col, String cause) {
		System.err.println("Error, row=" + (row + 1) + ", col=" + (col + 1) + ", cause=" + cause);
	}

	private void registerChain(StringBuilder currentChain) {
		String chain = currentChain.toString();

		MutableInt occurence = chainToOccurenceMap.get(chain);

		if (occurence == null) {
			chainToOccurenceMap.put(chain, new MutableInt(1));
		} else {
			occurence.increment();
		}

	}

	private char convertGoalNumberToChar(int number) {
		switch (number) {
			case 1:
				return 'J';
			case 2:
				return 'H';
			case 3:
				return 'E';
			case 4:
				return 'S';
			case 5:
				return 'O';
			default:
				return 'X';
		}
	}

	private void writeChainStats(String file) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(file)))) {
			for (Entry<String, MutableInt> e : chainToOccurenceMap.entrySet()) {
				bw.write(e.getKey() + "\t" + e.getValue());
				bw.newLine();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		String dir = "d:\\eTaxi\\Poznan_KBR\\ankiety\\";
		String survey = "STR5-WSZ v1.2.1.XLS";
		String chains = "chains.txt";

		PoznanChainsProcessor processor = new PoznanChainsProcessor();
		processor.processFile(dir + survey);
		processor.writeChainStats(dir + chains);
	}
}
