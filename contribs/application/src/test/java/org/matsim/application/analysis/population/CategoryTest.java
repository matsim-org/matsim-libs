package org.matsim.application.analysis.population;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

	@Test
	void standard() {

		Category c = new Category(Set.of("a", "b", "c"));

		assertThat(c.categorize("a")).isEqualTo("a");
		assertThat(c.categorize("b")).isEqualTo("b");
		assertThat(c.categorize("c")).isEqualTo("c");
		assertThat(c.categorize("d")).isNull();

	}

	@Test
	void ranges() {

		Category c = new Category(Set.of("1-2", "2-4", "4+"));

		assertThat(c.categorize("1")).isEqualTo("1-2");
		assertThat(c.categorize(1)).isEqualTo("1-2");
		assertThat(c.categorize(1.0)).isEqualTo("1-2");

		assertThat(c.categorize("2")).isEqualTo("2-4");
		assertThat(c.categorize("3")).isEqualTo("2-4");
		assertThat(c.categorize("5")).isEqualTo("4+");
		assertThat(c.categorize(5)).isEqualTo("4+");
		assertThat(c.categorize(5.0)).isEqualTo("4+");

	}

	@Test
	void grouped() {

		Category c = new Category(Set.of("a,b", "101,102"));

		assertThat(c.categorize("a")).isEqualTo("a,b");
		assertThat(c.categorize("b")).isEqualTo("a,b");
		assertThat(c.categorize(101)).isEqualTo("101,102");
		assertThat(c.categorize(102)).isEqualTo("101,102");

	}

	@Test
	void bool() {

		Category c = new Category(Set.of("y", "n"));

		assertThat(c.categorize("y")).isEqualTo("y");
		assertThat(c.categorize("yes")).isEqualTo("y");
		assertThat(c.categorize("1")).isEqualTo("y");

		assertThat(c.categorize(true)).isEqualTo("y");
		assertThat(c.categorize(false)).isEqualTo("n");

	}
}
