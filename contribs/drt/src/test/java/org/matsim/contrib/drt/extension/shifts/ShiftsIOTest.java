package org.matsim.contrib.drt.extension.shifts;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.shifts.io.DrtShiftsReader;
import org.matsim.contrib.drt.extension.shifts.io.DrtShiftsWriter;
import org.matsim.contrib.drt.extension.shifts.operationFacilities.OperationFacility;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShift;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftSpecification;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftsSpecification;
import org.matsim.contrib.drt.extension.shifts.shift.DrtShiftsSpecificationImpl;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public class ShiftsIOTest extends MatsimTestCase {

	private static final String TESTSHIFTSINPUT  = "testShifts.xml";
	private static final String TESTXMLOUTPUT  = "testShiftsOut.xml";

	private final Id<DrtShift> sid11 = Id.create("11", DrtShift.class);
	private final Id<DrtShift> sid22 = Id.create("22", DrtShift.class);
	private final Id<DrtShift> sid33 = Id.create("33", DrtShift.class);
	private final Id<OperationFacility> oid1 = Id.create("op1", OperationFacility.class);
	private final Id<OperationFacility> oid2 = Id.create("op2", OperationFacility.class);


	public void testBasicReaderWriter() {

		DrtShiftsSpecification shiftsSpecification = new DrtShiftsSpecificationImpl();

		DrtShiftsReader reader = new DrtShiftsReader(shiftsSpecification);
		reader.readFile(this.getPackageInputDirectory() + TESTSHIFTSINPUT);
		checkContent(shiftsSpecification);

		DrtShiftsWriter writer = new DrtShiftsWriter(shiftsSpecification);
		String outfilename = this.getOutputDirectory() +  TESTXMLOUTPUT;
		writer.writeFile(outfilename);

		File outFile = new File(outfilename);
		assertTrue(outFile.exists());

		//read it again to check if the same is read as at the very first beginning of test
		shiftsSpecification = new DrtShiftsSpecificationImpl();
		reader = new DrtShiftsReader(shiftsSpecification);
		reader.readFile(outfilename);
		checkContent(shiftsSpecification);
	}

	private void checkContent(DrtShiftsSpecification shiftsSpecification) {
		assertEquals(3, shiftsSpecification.getShiftSpecifications().size());

		DrtShiftSpecification shiftSpecification1 = shiftsSpecification.getShiftSpecifications().get(sid11);
		assertNotNull(shiftSpecification1);
		assertEquals(sid11, shiftSpecification1.getId());
		assertEquals(14400., shiftSpecification1.getStartTime());
		assertEquals(45000., shiftSpecification1.getEndTime());
		assertTrue(shiftSpecification1.getOperationFacilityId().isPresent());
		assertEquals(oid1, shiftSpecification1.getOperationFacilityId().get());
		assertEquals(1800., shiftSpecification1.getBreak().getDuration());
		assertEquals(28800., shiftSpecification1.getBreak().getEarliestBreakStartTime());
		assertEquals(32400., shiftSpecification1.getBreak().getLatestBreakEndTime());

		DrtShiftSpecification shiftSpecification2 = shiftsSpecification.getShiftSpecifications().get(sid22);
		assertNotNull(shiftSpecification2);
		assertEquals(sid22, shiftSpecification2.getId());
		assertEquals(18400., shiftSpecification2.getStartTime());
		assertEquals(49000., shiftSpecification2.getEndTime());
		assertTrue(shiftSpecification2.getOperationFacilityId().isPresent());
		assertEquals(oid2, shiftSpecification2.getOperationFacilityId().get());
		assertEquals(3600., shiftSpecification2.getBreak().getDuration());
		assertEquals(29200., shiftSpecification2.getBreak().getEarliestBreakStartTime());
		assertEquals(32800., shiftSpecification2.getBreak().getLatestBreakEndTime());

		DrtShiftSpecification shiftSpecification3 = shiftsSpecification.getShiftSpecifications().get(sid33);
		assertNotNull(shiftSpecification3);
		assertEquals(sid33, shiftSpecification3.getId());
		assertEquals(22400., shiftSpecification3.getStartTime());
		assertEquals(53000., shiftSpecification3.getEndTime());
		assertFalse(shiftSpecification3.getOperationFacilityId().isPresent());
		assertEquals(Optional.empty(), shiftSpecification3.getOperationFacilityId());
		assertEquals(1800., shiftSpecification3.getBreak().getDuration());
		assertEquals(29600., shiftSpecification3.getBreak().getEarliestBreakStartTime());
		assertEquals(33200., shiftSpecification3.getBreak().getLatestBreakEndTime());
	}
}
