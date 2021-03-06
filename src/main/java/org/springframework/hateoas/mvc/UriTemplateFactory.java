package org.springframework.hateoas.mvc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.util.UriTemplate;

/**
 * Builds and caches UriTemplates.
 */
public class UriTemplateFactory {
	Map<String, UriTemplate> templateCache = new ConcurrentHashMap<String, UriTemplate>();
	public UriTemplate templateFor(String mapping) {
		if (templateCache.containsKey(mapping)) {
			return templateCache.get(mapping);
		} else {
			UriTemplate template = new UriTemplate(mapping);
			templateCache.put(mapping, template);
			return template;
		}
	}
}
