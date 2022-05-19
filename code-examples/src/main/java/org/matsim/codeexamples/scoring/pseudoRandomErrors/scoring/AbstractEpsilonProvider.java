package org.matsim.codeexamples.scoring.pseudoRandomErrors.scoring;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public abstract class AbstractEpsilonProvider implements EpsilonProvider {
	private final MessageDigest digest;
	private final double maximumValue;
	private final long randomSeed;

	public AbstractEpsilonProvider(long randomSeed) {
		try {
			this.digest = MessageDigest.getInstance("SHA-512");
			this.maximumValue = BigInteger.valueOf(2).pow(digest.getDigestLength() * 8).doubleValue();
			this.randomSeed = randomSeed;
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Cannot find SHA-512 algorithm. Providing epsilons is not possible.");
		}
	}

	protected double getUniformEpsilon(Id<Person> personId, int tripIndex, Object alternative) {
		digest.reset();

		digest.update(ByteBuffer //
				.allocate(8 + 3 * 4) // long + 3 * int
				.putLong(randomSeed) //
				.putInt(personId.index()) //
				.putInt(tripIndex) //
				.putInt(alternative.hashCode()) //
				.array() //
		);

		return new BigInteger(1, digest.digest()).doubleValue() / maximumValue;
	}
}
