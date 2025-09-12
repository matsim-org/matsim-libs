package org.matsim.contrib.profiling.aop.trace;

import jdk.jfr.*;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;

/**
 * Event to record trace data from a {@link org.aspectj.lang.JoinPoint}
 */
@Label("Trace")
@Description("Event to record the duration and trace data of method calls")
@Category("MATSim")
@StackTrace(false) // if tracing all method calls, then stacktrace can be reconstructed and avoiding to additionally record it should improve performance, in all other cases, the stacktrace might be good to record
public class TraceProfilingEvent extends Event {

	/**
	 * {@link JoinPoint.StaticPart#getId()}
	 */
	@Unsigned
	int joinPointId;

	/**
	 * {@link JoinPoint.StaticPart#getKind()} likely always {@link JoinPoint#METHOD_CALL}
	 */
	String joinPointKind;

	/**
	 * Filename and line number of {@link JoinPoint.StaticPart#getSourceLocation()}
	 */
	String sourceLocation;

	/**
	 * {@link Signature#toLongString()}
	 */
	String longSignature;

	/**
	 * {@link JoinPoint#getThis()}
	 */
	Class<?> callerClass;

	/**
	 * {@link JoinPoint#getTarget()}
	 */
	Class<?> targetClass;

	/**
	 * {@link JoinPoint#getArgs()}
	 */
	String args;

	/**
	 * Return value of {@link org.aspectj.lang.ProceedingJoinPoint#proceed(Object[])}
	 */
	String returnValue;

	Class<?> errorClass;
	String errorMessage;


}
