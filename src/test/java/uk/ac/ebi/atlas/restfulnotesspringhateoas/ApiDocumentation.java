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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.constraints.ConstraintDescriptions;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper.document;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(RestDocumentationExtension.class)
public class ApiDocumentation {

	@Autowired
	private NoteRepository noteRepository;

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp(RestDocumentationContextProvider restDocumentation) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(restDocumentation))
				.alwaysDo(document("{method-name}/{step}/",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint())))
				.build();
	}

	@Test
	void headersExample() throws Exception {
		this.mockMvc
			.perform(get("/"))
			.andExpect(status().isOk())
			.andDo(document("{method-name}",
				responseHeaders(
					headerWithName("Content-Type").description("The Content-Type of the payload, e.g. `application/hal+json`"))));
	}

	@Test
	void errorExample() throws Exception {
		this.mockMvc
			.perform(get("/error")
				.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
				.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/notes")
				.requestAttr(RequestDispatcher.ERROR_MESSAGE, "The tag 'http://localhost:8080/tags/123' does not exist"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("error", is("Bad Request")))
			.andExpect(jsonPath("timestamp", is(notNullValue())))
			.andExpect(jsonPath("status", is(400)))
			.andExpect(jsonPath("path", is(notNullValue())))
			.andDo(document("{method-name}",
				responseFields(
					fieldWithPath("error").description("The HTTP error that occurred, e.g. `Bad Request`"),
					fieldWithPath("message").description("A description of the cause of the error"),
					fieldWithPath("path").description("The path to which the request was made"),
					fieldWithPath("status").description("The HTTP status code, e.g. `400`"),
					fieldWithPath("timestamp").description("The time, in milliseconds, at which the error occurred"))));
	}

	@Test
	void indexExample() throws Exception {
		this.mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andDo(document("{method-name}",
				links(
					linkWithRel("notes").description("The <<resources_notes,Notes resource>>"),
					linkWithRel("tags").description("The <<resources_tags,Tags resource>>")),
				responseFields(
					subsectionWithPath("_links").description("<<resources_index_access_links,Links>> to other resources"))));
	}

	@Test
	void notesListExample() throws Exception {
		this.noteRepository.deleteAll();

		createNote("REST maturity model", "https://martinfowler.com/articles/richardsonMaturityModel.html");
		createNote("Hypertext Application Language (HAL)", "https://github.com/mikekelly/hal_specification");
		createNote("Application-Level Profile Semantics (ALPS)", "https://github.com/alps-io/spec");

		this.mockMvc
			.perform(get("/notes"))
			.andExpect(status().isOk())
			.andDo(document("{method-name}",
				responseFields(
					subsectionWithPath("_embedded.notes").description("An array of <<resources_note, Note resources>>"))));
	}

	@Test
	void notesCreateExample() throws Exception {
		Map<String, String> tag = new HashMap<>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		Map<String, Object> note = new HashMap<>();
		note.put("title", "REST maturity model");
		note.put("body", "https://martinfowler.com/articles/richardsonMaturityModel.html");
		note.put("tags", Collections.singletonList(tagLocation));

		ConstrainedFields fields = new ConstrainedFields(NoteInput.class);

		this.mockMvc
			.perform(post("/notes")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(note)))
			.andExpect(
				status().isCreated())
			.andDo(document("{method-name}",
				requestFields(
					fields.withPath("title").description("The title of the note"),
					fields.withPath("body").description("The body of the note"),
					fields.withPath("tags").description("An array of tag resource URIs"))));
	}

	@Test
	void noteGetExample() throws Exception {
		Map<String, String> tag = new HashMap<>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		Map<String, Object> note = new HashMap<>();
		note.put("title", "REST maturity model");
		note.put("body", "https://martinfowler.com/articles/richardsonMaturityModel.html");
		note.put("tags", Collections.singletonList(tagLocation));

		String noteLocation = this.mockMvc
			.perform(post("/notes")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(note)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		this.mockMvc
			.perform(get(noteLocation))
			.andExpect(status().isOk())
			.andExpect(jsonPath("title", is(note.get("title"))))
			.andExpect(jsonPath("body", is(note.get("body"))))
			.andExpect(jsonPath("_links.self.href", is(noteLocation)))
			.andExpect(jsonPath("_links.note-tags", is(notNullValue())))
			.andDo(document("{method-name}",
				links(
					linkWithRel("self").description("This <<resources_note,note>>"),
					linkWithRel("note-tags").description("This note's tags")),
				responseFields(
					fieldWithPath("title").description("The title of the note"),
					fieldWithPath("body").description("The body of the note"),
					subsectionWithPath("_links").description("<<resources_note_links,Links>> to other resources"))));

	}

	@Test
	void tagsListExample() throws Exception {
		this.noteRepository.deleteAll();
		this.tagRepository.deleteAll();

		createTag("REST");
		createTag("Hypermedia");
		createTag("HTTP");

		this.mockMvc
			.perform(get("/tags"))
			.andExpect(status().isOk())
			.andDo(document("{method-name}",
				responseFields(
					subsectionWithPath("_embedded.tags").description("An array of <<resources_tag,Tag resources>>"))));
	}

	@Test
	void tagsCreateExample() throws Exception {
		Map<String, String> tag = new HashMap<>();
		tag.put("name", "REST");

		ConstrainedFields fields = new ConstrainedFields(TagInput.class);

		this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andDo(document("{method-name}",
				requestFields(
					fields.withPath("name").description("The name of the tag"))));
	}

	@Test
	void noteUpdateExample() throws Exception {
		Map<String, Object> note = new HashMap<>();
		note.put("title", "REST maturity model");
		note.put("body", "https://martinfowler.com/articles/richardsonMaturityModel.html");

		String noteLocation = this.mockMvc
			.perform(post("/notes")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(note)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		this.mockMvc
			.perform(get(noteLocation))
			.andExpect(status().isOk())
			.andExpect(jsonPath("title", is(note.get("title"))))
			.andExpect(jsonPath("body", is(note.get("body"))))
			.andExpect(jsonPath("_links.self.href", is(noteLocation)))
			.andExpect(jsonPath("_links.note-tags", is(notNullValue())));

		Map<String, String> tag = new HashMap<>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		Map<String, Object> noteUpdate = new HashMap<>();
		noteUpdate.put("tags", Collections.singletonList(tagLocation));

		ConstrainedFields fields = new ConstrainedFields(NotePatchInput.class);

		this.mockMvc
			.perform(patch(noteLocation)
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(noteUpdate)))
			.andExpect(status().isNoContent())
			.andDo(document("{method-name}",
				requestFields(
					fields.withPath("title")
						.description("The title of the note")
						.type(JsonFieldType.STRING)
						.optional(),
					fields.withPath("body")
						.description("The body of the note")
						.type(JsonFieldType.STRING)
						.optional(),
					fields.withPath("tags")
						.description("An array of tag resource URIs"))));
	}

	@Test
	void tagGetExample() throws Exception {
		Map<String, String> tag = new HashMap<>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		this.mockMvc
			.perform(get(tagLocation))
			.andExpect(status().isOk())
			.andExpect(jsonPath("name", is(tag.get("name"))))
			.andDo(document("{method-name}",
				links(
					linkWithRel("self").description("This <<resources_tag,tag>>"),
					linkWithRel("tagged-notes").description("The notes that have this tag")),
				responseFields(
					fieldWithPath("name").description("The name of the tag"),
					subsectionWithPath("_links").description("<<resources_tag_links,Links>> to other resources"))));
	}

	@Test
	void tagUpdateExample() throws Exception {
		Map<String, String> tag = new HashMap<>();
		tag.put("name", "REST");

		String tagLocation = this.mockMvc
			.perform(post("/tags")
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tag)))
			.andExpect(status().isCreated())
			.andReturn().getResponse().getHeader("Location");

		Map<String, Object> tagUpdate = new HashMap<>();
		tagUpdate.put("name", "RESTful");

		ConstrainedFields fields = new ConstrainedFields(TagPatchInput.class);

		this.mockMvc
			.perform(patch(tagLocation)
				.contentType(MediaTypes.HAL_JSON)
				.content(this.objectMapper.writeValueAsString(tagUpdate)))
			.andExpect(status().isNoContent())
			.andDo(document("{method-name}",
				requestFields(
					fields.withPath("name").description("The name of the tag"))));
	}

	private void createNote(String title, String body) {
		Note note = new Note();
		note.setTitle(title);
		note.setBody(body);

		this.noteRepository.save(note);
	}

	private void createTag(String name) {
		Tag tag = new Tag();
		tag.setName(name);
		this.tagRepository.save(tag);
	}

	private static class ConstrainedFields {

		private final ConstraintDescriptions constraintDescriptions;

		ConstrainedFields(Class<?> input) {
			this.constraintDescriptions = new ConstraintDescriptions(input);
		}

		private FieldDescriptor withPath(String path) {
			return fieldWithPath(path).attributes(key("constraints").value(StringUtils
					.collectionToDelimitedString(this.constraintDescriptions
							.descriptionsForProperty(path), ". ")));
		}
	}

}
