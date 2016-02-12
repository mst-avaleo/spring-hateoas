/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.mvc;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateMidnight;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;
import org.springframework.hateoas.TestUtils;
import org.springframework.hateoas.mvc.ControllerLinkBuilderUnitTest.ControllerWithMethods;
import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class FastLinksUnitTest extends TestUtils {

	@Test
	public void dateParams() {
		DateMidnight now = new DateMidnight();
		Object invocationValue = methodOn(SampleController.class).sampleMethod(now);

		String link = FastLinks.linkTo(invocationValue);
		assertThat(link, endsWith("/sample/" + ISODateTimeFormat.date().print(now)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void spaceIsInvalidPathSegment() {
		FastLinks.linkTo(methodOn(ControllerWithMethods.class).methodWithPathVariable("with blank"));
	}

	@Test
	public void noParamNames() {
		String link = FastLinks.linkTo(methodOn(SampleController.class).noParamNames(1L, 2L, 3L));
		assertThat(link, endsWith("/sample/2/1?id3=3"));
	}

	@Test
	public void optionalParameters() {
		String link = FastLinks.linkTo(methodOn(SampleController.class).sampleMethod(1L, 2L, null));

		assertThat(link, endsWith("/sample/1?id1=2"));
	}


	@Test
	public void listParameters() {
		List<Long> idList = Arrays.asList(2L, 3L, 4L);
		String link = FastLinks.linkTo(methodOn(SampleController.class).listParam(1L, idList));

		assertThat(link, endsWith("/sample/1?ids=2,3,4"));
	}

	@Test
	public void createsLinkToControllerMethodWithMapRequestParam() {

		Map<String, String> queryParams = new LinkedHashMap<String, String>();
		queryParams.put("firstKey", "firstValue");
		queryParams.put("secondKey", "secondValue");

		String link = FastLinks.linkTo(methodOn(SampleController.class).sampleMethodWithMap(queryParams));

		assertPointsToMockServer(link);
		assertThat(link, endsWith("/sample/mapsupport?firstKey=firstValue&secondKey=secondValue"));
	}

	@Test
	public void createsLinkToControllerMethodWithMultiValueMapRequestParam() {

		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
		queryParams.put("key1", Arrays.asList("value1a", "value1b"));
		queryParams.put("key2", Arrays.asList("value2a", "value2b"));

		String link = FastLinks.linkTo(methodOn(SampleController.class).sampleMethodWithMap(queryParams));

		assertPointsToMockServer(link);
		assertThat(link,
				endsWith("/sample/multivaluemapsupport?key1=value1a&key1=value1b&key2=value2a&key2=value2b"));
	}

	protected void assertPointsToMockServer(String link) {
		assertThat(link, startsWith("http://localhost"));
	}

	static interface SampleController {

		@RequestMapping("/sample/{id}")
		HttpEntity<?> listParam(@PathVariable("id") Long id, @RequestParam("ids") List<Long> ids);

		@RequestMapping("/sample/{id}")
		HttpEntity<?> sampleMethod(@PathVariable("id") Long id, @RequestParam("id1") Long id1, @RequestParam("id2") Long id2);

		@RequestMapping("/sample/{time}")
		HttpEntity<?> sampleMethod(@PathVariable("time") DateMidnight time);

		@RequestMapping("/sample/{id2}/{id1}")
		HttpEntity<?> noParamNames(@PathVariable Long id1, @PathVariable Long id2, @RequestParam Long id3);

		@RequestMapping("/sample/mapsupport")
		HttpEntity<?> sampleMethodWithMap(@RequestParam Map<String, String> queryParams);

		@RequestMapping("/sample/multivaluemapsupport")
		HttpEntity<?> sampleMethodWithMap(@RequestParam MultiValueMap<String, String> queryParams);
	}

}
