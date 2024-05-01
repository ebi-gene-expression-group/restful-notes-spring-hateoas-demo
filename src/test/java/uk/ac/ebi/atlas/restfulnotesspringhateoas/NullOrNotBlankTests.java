/*
 * Copyright 2014-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.ebi.atlas.restfulnotesspringhateoas;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NullOrNotBlankTests {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void nullValue() {
		Set<ConstraintViolation<Constrained>> violations = validator.validate(new Constrained(null));
		assertThat(violations).isEmpty();
	}

	@Test
	void zeroLengthValue() {
		Set<ConstraintViolation<Constrained>> violations = validator.validate(new Constrained(""));
		assertThat(violations).hasSize(2);
	}

	@Test
	void blankValue() {
		Set<ConstraintViolation<Constrained>> violations = validator.validate(new Constrained("   "));
		assertThat(violations).hasSize(2);
	}

	@Test
	void nonBlankValue() {
		Set<ConstraintViolation<Constrained>> violations = validator.validate(new Constrained("test"));
		assertThat(violations).isEmpty();
	}

	static class Constrained {

		@NullOrNotBlank
		private final String value;

		public Constrained(String value) {
			this.value = value;
		}

	}

}
