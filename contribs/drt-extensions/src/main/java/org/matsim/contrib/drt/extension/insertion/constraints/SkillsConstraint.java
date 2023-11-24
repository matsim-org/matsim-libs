package org.matsim.contrib.drt.extension.insertion.constraints;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.extension.insertion.DrtInsertionConstraint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SkillsConstraint implements DrtInsertionConstraint {
	private final VehicleSkillsSupplier vehicleSkillsSupplier;
	private final RequestRequirementsSupplier requestRequirementsSupplier;

	public SkillsConstraint(VehicleSkillsSupplier vehicleSkillsSupplier,
			RequestRequirementsSupplier requestRequirementsSupplier) {
		this.vehicleSkillsSupplier = vehicleSkillsSupplier;
		this.requestRequirementsSupplier = requestRequirementsSupplier;
	}

	@Override
	public boolean checkInsertion(DrtRequest drtRequest, Insertion insertion, DetourTimeInfo detourTimeInfo) {
		Set<String> requestRequirements = requestRequirementsSupplier.getRequiredSkills(drtRequest);
		Set<String> vehicleSkills = vehicleSkillsSupplier.getAvailableSkills(insertion.vehicleEntry.vehicle);

		if (requestRequirements == null) {
			return true;
		}

		if (vehicleSkills == null) {
			return false;
		}

		return vehicleSkills.containsAll(requestRequirements);
	}

	static public interface VehicleSkillsSupplier {
		Set<String> getAvailableSkills(DvrpVehicle vehicle);
	}

	static public interface RequestRequirementsSupplier {
		Set<String> getRequiredSkills(DrtRequest request);
	}

	static public class VehicleSkillsBuilder {
		private final ImmutableMap.Builder<Id<DvrpVehicle>, ImmutableSet<String>> builder = ImmutableMap.builder();

		public VehicleSkillsBuilder addVehicle(Id<DvrpVehicle> vehicleId, Set<String> skills) {
			builder.put(vehicleId, ImmutableSet.copyOf(skills));
			return this;
		}

		public VehicleSkillsSupplier build() {
			var data = builder.build();
			return v -> data.get(v.getId());
		}
	}

	static public class PersonRequirementsBuilder {
		private final ImmutableMap.Builder<Id<Person>, ImmutableSet<String>> builder = ImmutableMap.builder();

		public PersonRequirementsBuilder addVehicle(Id<Person> personId, Set<String> skills) {
			builder.put(personId, ImmutableSet.copyOf(skills));
			return this;
		}

		public RequestRequirementsSupplier build() {
			var data = builder.build();

			return request -> {
				ImmutableSet.Builder<String> builder = ImmutableSet.builder();

				for (Id<Person> passengerId : request.getPassengerIds()) {
					var passengerRequirements = data.get(passengerId);

					if (passengerRequirements != null) {
						builder.addAll(passengerRequirements);
					}
				}

				return builder.build();
			};
		}
	}
}
