package org.matsim.dsim;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.*;

/**
 * Annotation to disbale test cases on the windows build of the github actions ci. We obverved that some tests for the
 * distributed simulation need a very long time on the windows build of the github actions ci. This is the case for tests
 * that simulate a distributed execution of the simulation. Since Windows is not really the target platform for distributed
 * simulations, we decided to disable these tests on the windows build.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledOnGitHubWindowsCI.Condition.class)
public @interface DisabledOnGitHubWindowsCI {

	class Condition implements ExecutionCondition {
		@Override
		public @NonNull ConditionEvaluationResult evaluateExecutionCondition(@NonNull ExtensionContext context) {
			boolean isCI = "true".equals(System.getenv("CI"));
			boolean isWindows = "Windows".equals(System.getenv("RUNNER_OS"));

			if (isCI && isWindows) {
				return ConditionEvaluationResult.disabled("Disabled on GitHub Actions Windows CI");
			}
			return ConditionEvaluationResult.enabled("Not running on GitHub Actions Windows CI");
		}
	}
}
