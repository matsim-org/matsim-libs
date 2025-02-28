package org.matsim.contrib.profiling.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.matsim.contrib.profiling.events.JFRMatsimEvent;

/**
 * Aspect example using annotations instead of the .aj file type
 */
@Aspect
public class AnnotationBasedExampleAspect {

	/* VehicleUtils to test post-compile time weaving (enhance code of a library) */

	/**
	 * VehicleUtils to test post-compile time weaving (enhance code of a library)
	 * <p>
	 * Example without separate pointcut declaration.
	 * Use the full canonical name (including package) in the pointcut declaration to ensure aspectj finds the targeted class.
	 * @apiNote Advice must be public.
	 * @param thisJoinPointStaticPart Needs to be a param to get access to it like in the aj files
	 */
	@Before("call(* org.matsim.vehicles.VehicleUtils.getFactory())")
	public void BeforeGetFactory(JoinPoint.StaticPart thisJoinPointStaticPart) {
		JFRMatsimEvent.create("Annotated aspect: " + thisJoinPointStaticPart.getSignature()).commit();
		System.out.println("aspect via annotations: " + thisJoinPointStaticPart.getSignature());
	}

	/* AspectJTest.MyReplanningContext to test compile time weaving (enhance code of this module during its compilation) */

	/**
	 * Limit the affected code to only within a given package.
	 */
	@Pointcut("call(int org.matsim.core.replanning.ReplanningContext.getIteration()) && within(org.matsim.contrib.profiling.aop.*)")
	void ReplanningContextGetIteration() {}

	@Around("ReplanningContextGetIteration()")
	public 	int AroundReplanningContextGetIteration(ProceedingJoinPoint p) throws Throwable {
		int iteration = (int) p.proceed();
		System.out.println("iteration before incrementing: " + iteration + " - " + p.getSignature());
		return iteration + 1;
	}


}
