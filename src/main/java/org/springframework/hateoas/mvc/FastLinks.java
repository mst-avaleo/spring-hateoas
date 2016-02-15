package org.springframework.hateoas.mvc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.hateoas.core.DummyInvocationUtils.LastInvocationAware;
import org.springframework.hateoas.core.DummyInvocationUtils.MethodInvocation;
import org.springframework.util.Assert;

public class FastLinks {
	private static FastLinkTemplateCachingFactory LINK_FACTORY = new FastLinkTemplateCachingFactory();

	/**
	 * Simple bean for storing last invocation but without proxy overhead.
	 */
	public static class LastInvocationHolder {
		private final MethodInvocation lastInvocation;
		private final List<Object> objectParameters;

		public LastInvocationHolder(LastInvocationAware original) {
			lastInvocation = original.getLastInvocation();
			objectParameters = toList(original.getObjectParameters());
		}

		private List<Object> toList(Iterator<Object> ite) {
			List<Object> result = new ArrayList<Object>();
			while(ite.hasNext()) {
				result.add(ite.next());
			}
			return result;
		}

		public List<Object> getObjectParameters() {
			return objectParameters;
		}

		public MethodInvocation getLastInvocation() {
			return lastInvocation;
		}
	}

	public static String linkTo(Object invocationValue) {
		Assert.isInstanceOf(LastInvocationAware.class, invocationValue);
		LastInvocationHolder invocations = new LastInvocationHolder((LastInvocationAware) invocationValue);

		FastLinkTemplate linkTemplate = LINK_FACTORY.createLinkTemplate(invocations);
		return linkTemplate.build(invocations);
	}

}
