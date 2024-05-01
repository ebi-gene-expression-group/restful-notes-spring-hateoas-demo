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

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
class RestNotesControllerAdvice {

	@ExceptionHandler(IllegalArgumentException.class)
	void handleIllegalArgumentException(IllegalArgumentException ex,
			HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
	}

	@ExceptionHandler(ResourceDoesNotExistException.class)
	void handleResourceDoesNotExistException(ResourceDoesNotExistException ex,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.sendError(HttpStatus.NOT_FOUND.value(),
				"The resource '" + request.getRequestURI() + "' does not exist");
	}

}
