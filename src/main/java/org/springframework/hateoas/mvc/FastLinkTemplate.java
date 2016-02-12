package org.springframework.hateoas.mvc;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.hateoas.mvc.FastLinks.LastInvocationHolder;

class FastLinkTemplate {
	private enum Type {
		PATH_SEGMENT {
			@Override
			public boolean isAllowed(int c) {
				return isPchar(c);
			}
		},
		QUERY_PARAM {
			@Override
			public boolean isAllowed(int c) {
				if ('=' == c || '+' == c || '&' == c) {
					return false;
				} else {
					return isPchar(c) || '/' == c || '?' == c;
				}
			}
		};

		protected abstract boolean isAllowed(int c);

		public boolean isAllowed(String input) {
			for (int i = 0; i < input.length(); i++) {
				char c = input.charAt(i);
				if (!isAllowed(c)) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Indicates whether the given character is in the {@code ALPHA} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isAlpha(int c) {
			return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
		}

		/**
		 * Indicates whether the given character is in the {@code DIGIT} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isDigit(int c) {
			return c >= '0' && c <= '9';
		}

		/**
		 * Indicates whether the given character is in the {@code sub-delims} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isSubDelimiter(int c) {
			return '!' == c || '$' == c || '&' == c || '\'' == c || '(' == c || ')' == c || '*' == c || '+' == c ||
					',' == c || ';' == c || '=' == c;
		}

		/**
		 * Indicates whether the given character is in the {@code unreserved} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isUnreserved(int c) {
			return isAlpha(c) || isDigit(c) || '-' == c || '.' == c || '_' == c || '~' == c;
		}

		/**
		 * Indicates whether the given character is in the {@code pchar} set.
		 *
		 * @see <a href="http://www.ietf.org/rfc/rfc3986.txt">RFC 3986, appendix A</a>
		 */
		protected boolean isPchar(int c) {
			return isUnreserved(c) || isSubDelimiter(c) || ':' == c || '@' == c;
		}
	}

	private interface Encoder {
		String encode(Object par);
	}

	private interface ParamAccessor {
		Object getParam(LastInvocationHolder invocations);
	}

	static class ObjectParameterAccessor implements ParamAccessor {
		private int idx;

		public ObjectParameterAccessor(int idx) {
			this.idx = idx;
		}

		@Override
		public Object getParam(LastInvocationHolder invocations) {
			List<Object> objectParameters = invocations.getObjectParameters();
			if (objectParameters.size() > idx) {
				return objectParameters.get(idx);
			} else {
				throw new IllegalStateException("No object argument with given idx.");
			}
		}
	}

	static class MethodArgumentAccessor implements ParamAccessor {
		private int idx;

		public MethodArgumentAccessor(int idx) {
			this.idx = idx;
		}

		@Override
		public Object getParam(LastInvocationHolder invocations) {
			Object[] arguments = invocations.getLastInvocation().getArguments();
			if (arguments.length > idx) {
				return arguments[idx];
			} else {
				throw new IllegalStateException("No method argument with given idx.");
			}
		}
	}

	private static class ValueEncoder implements Encoder {
		private Type type;

		public ValueEncoder(Type type) {
			this.type = type;
		}

		public String encode(Object par) {
			String value = toString(par);
			verify(value);
			return value;
		}

		protected String toString(Object par) {
			if (par instanceof Collection) {
				return toString((Collection<Object>) par);
			} else {
				return par.toString();
			}
		}

		private String toString(Collection<Object> collection) {
			StringBuilder buf = new StringBuilder();
			for(Iterator<Object> i = collection.iterator(); i.hasNext() ;) {
				Object element = i.next();
				buf.append(element.toString());
				if (i.hasNext()) {
					buf.append(",");
				}
			}
			return buf.toString();
		}

		protected void verify(String value) {
			if (!type.isAllowed(value)) {
				throw new IllegalArgumentException("The value contains not allowed characters: " + value);
			}
		}
	}

	static abstract class Component {
		private Encoder encoder;

		public Component(Encoder encoder) {
			this.encoder = encoder;
		}

		protected String encode(Object par) {
			return encoder.encode(par);
		}

		public boolean append(StringBuilder buf, LastInvocationHolder invocation) {
			return doAppend(buf, invocation);
		}

		protected abstract boolean doAppend(StringBuilder buf, LastInvocationHolder invocation);
	}

	static class ParameterPathComponent extends Component {
		private ParamAccessor paramAccessor;

		public ParameterPathComponent(ParamAccessor paramAccessor) {
			super(new ValueEncoder(Type.PATH_SEGMENT));
			this.paramAccessor = paramAccessor;
		}

		@Override
		public boolean doAppend(StringBuilder buf, LastInvocationHolder invocation) {
			Object param = paramAccessor.getParam(invocation);
			if (param != null) {
				buf.append(encode(param));
				return true;
			} else {
				return false;
			}
		}
	}

	static class StaticPartPathComponent extends Component {
		private String part;

		public StaticPartPathComponent(String part) {
			super(new ValueEncoder(Type.PATH_SEGMENT));
			this.part = part;
		}

		@Override
		public boolean doAppend(StringBuilder buf, LastInvocationHolder invocation) {
			buf.append(part);
			return true;
		}
	}

	static class QueryParamComponent extends Component {
		private String paramName;
		private ParamAccessor paramAccessor;

		public QueryParamComponent(String paramName, int idx) {
			super(new ValueEncoder(Type.QUERY_PARAM));
			this.paramName = paramName;
			this.paramAccessor = new MethodArgumentAccessor(idx);
		}

		@Override
		public boolean doAppend(StringBuilder buf, LastInvocationHolder invocation) {
			Object paramValue = paramAccessor.getParam(invocation);

			if (paramValue != null) {
				String paramValueEncoded = encode(paramValue);
				buf.append(paramName).append("=").append(paramValueEncoded);
				return true;
			} else {
				return false;
			}
		}
	}

	private List<Component> linkComponents;
	private List<Component> queryComponents;

	public FastLinkTemplate(List<Component> linkComponents, List<Component> queryComponents) {
		this.linkComponents = linkComponents;
		this.queryComponents = queryComponents;
	}

	public String build(LastInvocationHolder invocation) {
		StringBuilder buf = new StringBuilder();

		URI baseUri = ControllerLinkBuilder.getBaseUri();
		buf.append(baseUri.toASCIIString());

		for (Component linkComponent : linkComponents) {
			boolean added = linkComponent.append(buf, invocation);
			if (!added) {
				throw new IllegalArgumentException("Something wrong. Parameters doesn't match the method?");
			}
		}

		buf.append("?");

		for (Component queryComponent : queryComponents) {
			boolean added = queryComponent.append(buf, invocation);
			if (added) {
				buf.append("&");
			}
		}

		// removing last ? or & character
		buf.setLength(buf.length() - 1);

		return buf.toString();
	}
}
