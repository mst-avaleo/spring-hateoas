package org.springframework.hateoas.mvc;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.hateoas.core.DummyInvocationUtils;
import org.springframework.hateoas.mvc.FastLinks.LastInvocationHolder;

public class FastLinkCachingFactory {
	Map<String, FastLinkTemplate> templateCache = new ConcurrentHashMap<String, FastLinkTemplate>();
	FastLinkFactory linkFactory = new FastLinkFactory();

	private String key(DummyInvocationUtils.MethodInvocation methodInvocation) {
		Class<?> type = methodInvocation.getTargetType();
		Method method = methodInvocation.getMethod();

		StringBuilder buf = new StringBuilder();
		buf.append(type.getName());
		if (method != null) {
			buf.append(method.getName());
			for (Class<?> par: method.getParameterTypes()) {
				buf.append(par.getName());
			}
		}
		return buf.toString();
	}


	protected FastLinkTemplate createLinkTemplate(LastInvocationHolder invocations) {
		DummyInvocationUtils.MethodInvocation methodInvocation = invocations.getLastInvocation();
		String methodKey = key(methodInvocation);
		if (templateCache.containsKey(methodKey)) {
			return templateCache.get(methodKey);
		} else {
			FastLinkTemplate linkTemplate = linkFactory.createLinkTemplate(invocations);
			templateCache.put(methodKey, linkTemplate);
			return linkTemplate;
		}
	}
}
