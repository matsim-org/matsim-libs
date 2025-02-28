package org.matsim.contrib.profiling.aop;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.vehicles.PersonVehicles;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class AspectJTest {

	private final PrintStream standardOut = System.out;
	private final OutputStream outputStreamCaptor = new ByteArrayOutputStream();

	static class MyScoringListener implements ScoringListener {

		@Override
		public void notifyScoring(ScoringEvent event) {
			System.out.println("scoring event");
		}
	}

	static final class MyReplanningContext implements ReplanningContext {

		@Override
		public int getIteration() {
			return 6;
		}
	}

	@BeforeEach
	public void setUp() {
		System.setOut(new PrintStream(outputStreamCaptor));
	}

	@AfterEach
	public void resetSystemOut() {
		System.setOut(standardOut);
	}

	/**
	 * Testing aspectj compile-time weaving of project classes. (see post-compile time weaving for dependencies)
	 * The outputstream is expected to hold both the output of {@link ScoringListenerProfilingAspect} and the method above {@link MyScoringListener#notifyScoring(ScoringEvent)}.
	 */
	@Test
	public void testAspectJ_compileTime() {
		var scoringListener = new MyScoringListener();
		scoringListener.notifyScoring(new ScoringEvent(null, 1, false));
		assertThat(outputStreamCaptor.toString()).isEqualTo("AOP profiling: void org.matsim.contrib.profiling.aop.AspectJTest.MyScoringListener.notifyScoring(ScoringEvent)\nscoring event\n");
	}

	/**
	 * Testing aspectj post-compile-time weaving of an aspect into methods of dependencies.
	 * I.e. if the {@link PersonVehiclesAspect} is properly applied to the code of a library ({@link PersonVehicles}).
	 */
	@Test
	public void testAspectJ_postCompileTime() {
		var personVehicles = new PersonVehicles();
		Id<Vehicle> result = personVehicles.getVehicle("test");
		assertThat(outputStreamCaptor.toString()).isEqualTo("AOP profiling: PersonVehicles\nmode: test\n");
		assertThat(result).isNotNull();
		assertThat(result.toString()).isEqualTo("aspect");
	}

	@Test
	public void whenPostCompileTimeWeaving_usingAnnotatedAspect_expectPrintFromAdvice() {
		var factory = VehicleUtils.getFactory();
		assertThat(factory).isNotNull(); // ensure the original implementation was not changed
		assertThat(outputStreamCaptor.toString()).isEqualTo("aspect via annotations: VehiclesFactory org.matsim.vehicles.VehicleUtils.getFactory()\n");
	}

	@Test
	public void whenCompileTimeWeaving_usingAnnotatedAspect_expectPrintFromAdvice() {
		var myContext = new MyReplanningContext();
		assertThat(myContext.getIteration()).isEqualTo(7); // changed via the advice in aspect
		assertThat(outputStreamCaptor.toString()).isEqualTo("iteration before incrementing: 6 - int org.matsim.contrib.profiling.aop.AspectJTest.MyReplanningContext.getIteration()\n");
	}
}
