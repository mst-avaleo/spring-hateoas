package org.springframework.hateoas.mvc;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.mvc.FastLinks.LastInvocationHolder;

public class FastLinkTemplateCachingFactory {
	private Map<Method, FastLinkTemplate> templateCache = new ConcurrentHashMap<Method, FastLinkTemplate>();
	private FastLinkTemplateFactory linkFactory = new FastLinkTemplateFactory();

	protected FastLinkTemplate createLinkTemplate(LastInvocationHolder invocations) {
		DummyInvocationUtils.MethodInvocation methodInvocation = invocations.getLastInvocation();
		Method method = methodInvocation.getMethod();

		if (templateCache.containsKey(method)) {
			return templateCache.get(method);
		} else {
			FastLinkTemplate linkTemplate = linkFactory.createLinkTemplate(invocations);
			templateCache.put(method, linkTemplate);
			return linkTemplate;
		}
	}
}
