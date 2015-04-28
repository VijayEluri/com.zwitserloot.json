package com.zwitserloot.json;

import static com.zwitserloot.json.JSONParser.NULL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public final class JSON {
	private Object[] path;
	private final Object object;
	private final Object self;
	private static final Object UNDEFINED = new Object();
	
	private static String typeOf(Object o) {
		if (o == NULL) return "null";
		if (o == UNDEFINED) return "undefined";
		if (o instanceof String) return "string";
		if (o instanceof Number) return "number";
		if (o instanceof Boolean) return "boolean";
		if (o instanceof List<?>) return "array";
		if (o instanceof Map<?, ?>) return "object";
		
		return "?" + o.getClass() + "?";
	}
	
	private JSON(Object o) {
		this.object = o;
		this.self = o;
		this.path = new Object[0];
	}
	
	private JSON(Object[] path, Object o, Object self) {
		this.object = o;
		this.self = self;
		this.path = path;
	}
	
	private Object dig(int depth) {
		if (depth == path.length) return self;
		Object out = object;
		
		for (int i = 0; i < depth; i++) {
			if (out == NULL || out == UNDEFINED) return UNDEFINED;
			Object x = path[i];
			if (x instanceof Integer) {
				int idx = ((Integer) x).intValue();
				try {
					out = ((List<?>) out).get(idx);
				} catch (Exception e) {
					out = UNDEFINED;
				}
			} else {
				String key = (String) x;
				try {
					Map<?, ?> m = (Map<?, ?>) out;
					out = m.get(key);
					if (out == null) out = UNDEFINED;
				} catch (Exception e) {
					out = UNDEFINED;
				}
			}
		}
		
		return out;
	}
	
	/**
	 * Creates a new JSON object that represents a new empty object.
	 */
	public static JSON newMap() {
		return new JSON(new LinkedHashMap<Object, Object>());
	}
	
	/**
	 * Creates a new JSON object that represents a new empty list.
	 */
	public static JSON newList() {
		return new JSON(new ArrayList<Object>());
	}
	
	/**
	 * Creates a new JSON object by parsing JSON.
	 */
	public static JSON parse(String s) {
		return new JSON(new JSONParser(s).parseObject());
	}
	
	/**
	 * Creates a new JSON object where all collection (maps and lists) objects starting from this element and delving arbitrarily deep are copied.
	 * 
	 * The returned object is a root even if this object is not.
	 */
	public JSON deepCopy() {
		if (self instanceof Map<?, ?>) {
			return new JSON(deepCopyInternal(self, false));
		} else if (self instanceof List<?>) {
			return new JSON(deepCopyInternal(self, false));
		} else return new JSON(self);
	}
	
	private Object deepCopyInternal(Object o, boolean denull) {
		if (denull && o == NULL) return null;
		
		if (o instanceof List<?>) {
			List<Object> out = new ArrayList<Object>();
			for (Object elem : (List<?>) o) out.add(deepCopyInternal(elem, denull));
			return out;
		} else if (o instanceof Map<?, ?>) {
			Map<Object, Object> out = new LinkedHashMap<Object, Object>();
			for (Map.Entry<?, ?> elem : ((Map<?, ?>) o).entrySet()) out.put(elem.getKey(), deepCopyInternal(elem.getValue(), denull));
			return out;
		}
		
		return o;
	}
	
	/**
	 * Converts the this element to compact JSON representation.
	 */
	public String toJSON() {
		return JSONWriter.toJSON(self);
	}
	
	/**
	 * Checks if this element exists.
	 */
	public boolean exists() {
		return self != UNDEFINED;
	}
	
	/**
	 * Returns the 'path' (each key / index from the root, separated by slashes, starting with a ~) of this element.
	 */
	public String getPath() {
		return getPath(-1);
	}
	
	/**
	 * Returns the 'path' (each key / index from the root, separated by slashes, starting with a ~), of this element, but but no more path segments than {@code len} are printed.
	 */
	public String getPath(int len) {
		len = len == -1 ? path.length : len;
		StringBuilder out = new StringBuilder("~");
		for (int i = 0; i < len; i++) out.append("/").append(path[i]);
		return out.toString();
	}
	
	/**
	 * If this element exists, returns whether it is {@code null} or not.
	 * 
	 * @JSONException If this element does not exist.
	 */
	public boolean isNull() {
		if (self == UNDEFINED) {
			invalidType("null");
			return false;
		}
		
		return self == NULL;
	}
	
	/**
	 * If this element exists, returns whether it is {@code null} or not.
	 * 
	 * If this element does not exist, {@code defaultValue} is returned.
	 */
	public boolean isNull(boolean defaultValue) {
		if (self == UNDEFINED) return defaultValue;
		return self == NULL;
	}
	
	/**
	 * Returns this object if it exists.
	 * 
	 * @throws JSONException If this element does not exist.
	 */
	public Object asObject() {
		if (self == UNDEFINED) {
			invalidType("object");
			return null;
		}
		
		if (self == NULL) return null;
		if (self instanceof List<?>) {
			return deepCopyInternal(self, true);
		} else if (self instanceof Map<?, ?>) {
			return deepCopyInternal(self, true);
		}
		
		return self;
	}
	
	/**
	 * If this element is non existent or {@code null}, the {@code alt} value is returned. Otherwise, the object is returned as is.
	 */
	public Object asObject(Object alt) {
		if (self == UNDEFINED || self == NULL) return alt;
		return self;
	}
	
	/**
	 * If this element is a string or {@code null} it is returned; if it is a number or boolean, it is converted to a string and returned.
	 * 
	 * @throws JSONException If the element is non-existent or not convertible to a string.
	 */
	public String asString() {
		if (self == NULL) return null;
		if (self instanceof String) return (String) self;
		if (self instanceof Number || self instanceof Boolean) return String.valueOf(self);
		invalidType("string");
		return null;
	}
	
	/**
	 * If this element is a string it is returned; if it is a number or boolean, it is converted to a string and returned.
	 * 
	 * Otherwise (including if it is {@code null}), {@code alt} is returned.
	 */
	public String asString(String alt) {
		if (self == NULL) return alt;
		if (self instanceof String) return (String) self;
		if (self instanceof Number || self instanceof Boolean) return String.valueOf(self);
		return alt;
	}
	
	/**
	 * If this element is a number or is a string that can be parsed with {@code Double.parseDouble}, it is returned as double.
	 * 
	 * @throws JSONException If this key is null, or non-existent, or not numeric and not a string parseable as such.
	 */
	public double asDouble() {
		if (self instanceof Number) return ((Number) self).doubleValue();
		
		if (self instanceof String) try {
			return Double.parseDouble((String) self);
		} catch (NumberFormatException e) {
			// intentional fallthrough.
		}
		
		invalidType("double");
		return 0D;
	}
	
	/**
	 * If this element is a number or is a string that can be parsed with {@code Double.parseDouble}, it is returned as double.
	 * 
	 * Otherwise, {@code alt} is returned.
	 */
	public double asDouble(double alt) {
		if (self instanceof Number) return ((Number) self).doubleValue();
		
		if (self instanceof String) try {
			return Double.parseDouble((String) self);
		} catch (NumberFormatException e) {
			// intentional fallthrough.
		}
		
		invalidType("double");
		return 0D;
	}
	
	/**
	 * If this element is a number that has no floating point component, or
	 * is a string that can be parsed with {@code Double.parseDouble} that has no floating point component, it is returned as int.
	 * 
	 * @throws JSONException If this key is null, or non-existent, or not numeric and integral and not a string parseable as such.
	 */
	public int asInt() {
		double v;
		
		if (self instanceof Number) {
			v = ((Number) self).doubleValue();
		} else if (self instanceof String) try {
			v = Double.parseDouble((String) self);
		} catch (NumberFormatException e) {
			invalidType("int");
			return 0;
		} else {
			invalidType("int");
			return 0;
		}
		
		int v2 = (int) v;
		if (v != v2) notIntegral(v);
		return v2;
	}
	
	/**
	 * If this element is a number that has no floating point component, or
	 * is a string that can be parsed with {@code Double.parseDouble} that has no floating point component, it is returned as int.
	 * 
	 * Otherwise, {@code alt} is returned.
	 */
	public int asInt(int alt) {
		double v;
		
		if (self instanceof Number) {
			v = ((Number) self).doubleValue();
		} else if (self instanceof String) try {
			v = Double.parseDouble((String) self);
		} catch (NumberFormatException e) {
			return alt;
		} else {
			return alt;
		}
		
		int v2 = (int) v;
		if (v == v2) return v2;
		return alt;
	}
	
	private static final long MAXIMUM_PRECISION_DOUBLE = 1L << 52 -1;
	
	/**
	 * If this element is a number that has no floating point component, or has sufficient magnitude that a double cannot accurately represent it, or
	 * is a string that can be parsed with {@code Double.parseDouble} with those rules or {@code Long.parseLong}, it is returned as long.
	 * 
	 * @throws JSONException If this key is null, or non-existent, or not numeric and integral and not a string parseable as such.
	 */
	public long asLong() {
		double v;
		
		if (self instanceof Number) {
			v = ((Number) self).doubleValue();
		} else if (self instanceof String) try {
			v = Double.parseDouble((String) self);
		} catch (NumberFormatException e) {
			invalidType("long");
			return 0L;
		} else {
			invalidType("long");
			return 0L;
		}
		
		if (v <= MAXIMUM_PRECISION_DOUBLE && v >=-MAXIMUM_PRECISION_DOUBLE) {
			long v2 = (long) v;
			if (v != v2) notIntegral(v);
			return v2;
		}
		
		// At this point, checking is kinda pointless, and javascript's doubles mean that at this point we're in imprecise territory anyway.
		// Should we error here?
		
		if (self instanceof Number) return ((Number) self).longValue();
		if (self instanceof String) try {
			return Long.parseLong((String) self);
		} catch (NumberFormatException e) {
			//intentional
		}
		invalidType("long");
		return 0L;
	}
	
	/**
	 * If this element is a number that has no floating point component, or has sufficient magnitude that a double cannot accurately represent it, or
	 * is a string that can be parsed with {@code Double.parseDouble} with those rules or {@code Long.parseLong}, it is returned as long.
	 * 
	 * Otherwise, {@code alt} is returned.
	 */
	public long asLong(long alt) {
		try {
			return asLong();
		} catch (Exception e) {
			return alt;
		}
	}
	
	/**
	 * If this element be treated as a string (see {@see #asString()}), and that string has at least one character, the first character is returned.
	 * 
	 * @throws JSONException If this key is null, or non existent, or cannot be converted to a string, or the string is empty.
	 */
	public char asChar() {
		String s = asString();
		if (s.length() > 0) return s.charAt(0);
		throw new JSONException("Key " + getPath() + " contains the empty string, which is not convertable to a char");
	}
	
	/**
	 * If this element can be treated as a string, and that string has at least one character, the first character is returned.
	 * 
	 * Otherwise, {@code alt} is returned.
	 */
	public char asChar(char alt) {
		try {
			return asChar();
		} catch (Exception e) {
			return alt;
		}
	}
	
	/**
	 * If this element is a string that represents one of the {@code enumType}'s values (case sensitive first, then uppercased), it is returned.
	 * 
	 * @throws JSONException If this key is null, or non existent, or not a string, or not one of the enum's values.
	 */
	public <E extends Enum<E>> E asEnum(Class<E> enumType) {
		if (self == NULL) return null;
		if (!(self instanceof String)) {
			invalidType("enum");
			return null;
		}
		
		try {
			try {
				return Enum.valueOf(enumType, (String) self);
			} catch (IllegalArgumentException e) {
				return Enum.valueOf(enumType, ((String) self).toUpperCase(Locale.US));
			}
		} catch (Exception e) {
			throw new JSONException("Key " + getPath() + " contains '" + self + "' which is not a value for enum '" + enumType.getName());
		}
	}
	
	/**
	 * If this element is a string that represents one of {@code enumType}'s values (case sensitive first, then uppercased), returns that.
	 * 
	 * If this element is null, or doesn't exist, or isn't such a string, {@code alt} is returned instead.
	 */
	public <E extends Enum<E>> E asEnum(Class<E> enumType, E alt) {
		try {
			try {
				return Enum.valueOf(enumType, (String) self);
			} catch (IllegalArgumentException e) {
				return Enum.valueOf(enumType, ((String) self).toUpperCase(Locale.US));
			}
		} catch (Exception e) {
			return alt;
		}
	}
	
	/**
	 * Interprets this element as a boolean and returns its value.
	 * 
	 * <ul>
	 * <li>If this element is a boolean, it is returned.</li>
	 * <li>If this element is a number, {@code} false is returned if it is 0, otherwise {@code true} is returned.</li>
	 * <li>If this element is a string and equal to one of 'true yes 1 t y on', {@code true} is returned, if equal to one of 'false no 0 f n off', {@code false} is returned, otherwise an exception is thrown.
	 * </ul>
	 * 
	 * @throws JSONException If this element doesn't exist, is null, or can't be interpreted as a boolean.
	 */
	public boolean asBoolean() {
		if (self instanceof Boolean) return ((Boolean) self).booleanValue();
		if (self instanceof Number) return ((Number) self).doubleValue() != 0;
		if (self instanceof String) {
			String s = " " + self.toString().toLowerCase() + " ";
			if (" true yes 1 t y on ".indexOf(s) > -1) return true;
			if (" false no 0 f n off ".indexOf(s) > -1) return false;
			notABoolean(s);
			return false;
		}
		invalidType("boolean");
		return false;
	}
	
	/**
	 * Interprets this element as a boolean and returns its value.
	 * 
	 * Works as {@see #asBoolean()}, but returns {@code alt} instead of throwing a {@code JSONException}.
	 */
	public boolean asBoolean(boolean alt) {
		try {
			return asBoolean();
		} catch (Exception e) {
			return alt;
		}
	}
	
	private final static class JSONList implements List<JSON> {
		private final JSON json;
		private final int offset, limit;
		
		private JSONList(JSON json, int offset, int limit) {
			this.json = json;
			this.offset = offset;
			this.limit = limit;
		}
		
		public boolean add(JSON o) {
			throw new UnsupportedOperationException();
		}
		
		public void add(int index, JSON o) {
			throw new UnsupportedOperationException();
		}
		
		public boolean addAll(Collection<? extends JSON> c) {
			throw new UnsupportedOperationException();
		}
		
		public boolean addAll(int index, Collection<? extends JSON> c) {
			throw new UnsupportedOperationException();
		}
		
		public void clear() {
			throw new UnsupportedOperationException();
		}
		
		public boolean contains(Object o) {
			throw new UnsupportedOperationException();
		}
		
		public boolean containsAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}
		
		public JSON get(int index) {
			if (index < 0 || limit > -1 && index >= limit) throw new IndexOutOfBoundsException();
			Object o = json.self;
			
			if (o instanceof List<?>) {
				int s = size();
				if (index < s) return json.get(index + offset);
				throw new IndexOutOfBoundsException("" + index + " not available (list size is " + s + ")");
			} else if (o == UNDEFINED) {
				throw new IndexOutOfBoundsException("0 is not available (list is just a non-existent value interpreted as an empty list)");
			} else if (o == NULL) {
				throw new IndexOutOfBoundsException("0 is not available (list is just a null value interpreted as an empty list)");
			} else if (index == 0 && offset == 0) {
				return json;
			}
			
			throw new IndexOutOfBoundsException("" + index + " not available (this is just a single element treated as a list, only index 0 is valid.");
		}
		
		public int indexOf(Object o) {
			throw new UnsupportedOperationException();
		}
		
		public boolean isEmpty() {
			return size() == 0;
		}
		
		public Iterator<JSON> iterator() {
			return new Iterator<JSON>() {
				private int pos = 0;
				private int size = size();
				
				public boolean hasNext() {
					return pos < size;
				}
				
				public JSON next() {
					if (!hasNext()) throw new NoSuchElementException();
					return get(pos++);
				}
				
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
		
		public int lastIndexOf(Object o) {
			throw new UnsupportedOperationException();
		}
		
		public ListIterator<JSON> listIterator() {
			throw new UnsupportedOperationException();
		}
		
		public ListIterator<JSON> listIterator(int index) {
			throw new UnsupportedOperationException();
		}
		
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}
		
		public JSON remove(int index) {
			throw new UnsupportedOperationException();
		}
		
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}
		
		public boolean retainAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}
		
		public JSON set(int index, JSON element) {
			throw new UnsupportedOperationException();
		}
		
		public int size() {
			if (limit >= 0) return limit - offset;
			Object o = json.self;
			
			if (o instanceof List<?>) return ((List<?>) o).size();
			if (o == UNDEFINED || o == NULL) return 0;
			return 1;
		}
		
		public List<JSON> subList(int fromIndex, int toIndex) {
			if (toIndex < fromIndex) throw new IllegalArgumentException();
			if (fromIndex < 0) throw new IndexOutOfBoundsException();
			if (toIndex > size()) throw new IndexOutOfBoundsException();
			
			return new JSONList(json, offset + fromIndex, offset + toIndex);
		}
		
		public Object[] toArray() {
			throw new UnsupportedOperationException();
		}
		
		public <T> T[] toArray(T[] a) {
			throw new UnsupportedOperationException();
		}
		
		@Override public String toString() {
			Object o = json.self;
			if (o == UNDEFINED) return "[Empty List: UNDEFINED]";
			if (o == NULL) return "[Empty List: NULL]";
			if (o instanceof List<?>) {
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				int s = size();
				for (int i = 0; i < s; i++) {
					if (i > 0) sb.append(", ");
					sb.append(get(i));
				}
				sb.append("]");
			}
			if (limit == 0) return "[Empty list: Empty sublist]";
			if (offset > 0) return "[Empty List: sublist of a singleton wrapper]";
			return "[Singleton list: " + json.toString() + "]";
		}
	}
	
	/**
	 * Interprets this element as a list.
	 * 
	 * <ul>
	 * <li>If the element is null or non existent, returns an empty list.</li>
	 * <li>If the element is a list, returns it as a java List object.</li>
	 * <li>Otherwise, a singleton list (a list with 1 element) is returned.</li>
	 * </ul>
	 * 
	 * The list is read-only and does not support listIterator.
	 */
	public List<JSON> asList() {
		return new JSONList(this, 0, -1);
	}
	
	public List<String> asStringList() {
		if (self == UNDEFINED || self == NULL) return Collections.emptyList();
		if (self instanceof String || self instanceof Number || self instanceof Boolean) return Collections.singletonList(String.valueOf(self));
		if (self instanceof List<?>) {
			List<?> raw = (List<?>) self;
			List<String> out = new ArrayList<String>(raw.size());
			int idx = 0;
			for (Object o : raw) {
				if (o instanceof String || o instanceof Number || o instanceof Boolean) out.add(String.valueOf(o));
				else if (o == NULL) out.add(null);
				else throw new JSONException("List item at " + idx + " is not convertable to a string because it is a " + typeOf(o));
			}
			return Collections.unmodifiableList(out);
		}
		
		invalidType("list");
		return null;
	}
	
	/**
	 * Interprets the element as a map (javascript object) and then returns its keys.
	 * 
	 * <ul>
	 * <li>If the element is null or non existent, returns an empty set.</li>
	 * <li>If the element is a map (javascript object), its keys are returned.<li>
	 * </ul>
	 * 
	 * @throws JSONException If the element is some other type.
	 */
	public Set<String> keySet() throws JSONException {
		if (self instanceof Map<?, ?>) {
			Set<String> set = new LinkedHashSet<String>();
			for (Object k : ((Map<?, ?>) self).keySet()) set.add(String.valueOf(k));
			return Collections.unmodifiableSet(set);
		}
		
		if (self == NULL || self == UNDEFINED) return Collections.emptySet();
		
		invalidType("object");
		return null;
	}
	
	private static Object[] addToPath(Object[] in, Object add) {
		Object[] newPath = new Object[in.length +1];
		System.arraycopy(in, 0, newPath, 0, in.length);
		newPath[in.length] = add;
		return newPath;
	}
	
	/**
	 * If this element is a list, a pointer to the end of the list is returned. This doesn't change the underlying data yet (call .setT() on the returned object to do that).
	 * 
	 * If this element is undefined, this call still works; intermediate elements will be created if you call .setT().
	 * 
	 * @throws JSONException If this element exists is not a list.
	 */
	public JSON add() {
		int i = 0;
		if (self instanceof List<?>) i = ((List<?>) self).size();
		else if (self == UNDEFINED) i = 0;
		else structureError(path.length, "array");
		
		return new JSON(addToPath(path, i), object, UNDEFINED);
	}
	
	/**
	 * Travels 1 level up the path if possible. This is the opposite of the {@code get()} methods.
	 * 
	 * @throws JSONException If this is a root-level element.
	 */
	public JSON up() {
		if (path.length == 0) throw new JSONException("You're already at the root level");
		if (path.length == 1) return new JSON(object);
		
		Object[] newPath = Arrays.copyOf(path, path.length - 1);
		return new JSON(newPath, object, dig(newPath.length - 1));
	}
	
	/**
	 * Travels back to the top element.
	 * 
	 * This is equivalent to undoing all {@code get()} calls. If this is already a root level reference, this method returns itself.
	 */
	public JSON top() {
		if (path.length == 0) return this;
		return new JSON(object);
	}
	
	/**
	 * Treats the current element as a list and returns a new pointer to the element at the stated index.
	 * 
	 * If the index doesn't exist, a pointer to a non-existent place is returned. asX() calls return the default or throw an exception, and the
	 * various set methods will attempt to create the structure you have made if possible. If this element isn't a list, a non-existent pointer
	 * is returned and the various set methods will throw an exception.
	 */
	public JSON get(int idx) {
		if (idx < 0) {
			indexError(idx);
			return null;
		}
		
		Object newSelf = UNDEFINED;
		if (self instanceof List<?>) try {
			newSelf = ((List<?>) self).get(idx);
		} catch (Exception e) {
			newSelf = UNDEFINED;
		}
		
		return new JSON(addToPath(path, idx), object, newSelf);
	}
	
	/**
	 * Treats the current element as a map (javascript object) and returns a new pointer to the element at the stated key.
	 * 
	 * If the key doesn't exist, a pointer to a non-existent place is returned. asX() calls return the default or throw an exception, and the
	 * various set methods will attempt to create the structure you have made if possible. If this element isn't a list, a non-existent pointer
	 * is returned and the various set methods will throw an exception.
	 */
	public JSON get(String path) {
		Object newSelf = UNDEFINED;
		if (self instanceof Map<?, ?>) newSelf = ((Map<?, ?>) self).get(path);
		if (newSelf == null) newSelf = UNDEFINED;
		
		return new JSON(addToPath(this.path, path), object, newSelf);
	}
	
	/**
	 * Tries to set the current element according to the provided non-collection data type.
	 * 
	 * If this element is a root, this operation will fail.<br />
	 * If this element is a member of a list or map, it is updated.<br />
	 * If this element is non-existent, each parent element is created if possible.
	 * <p>
	 * Supported types: All primitive wrappers, strings, enums, and JSON.
	 * <br />
	 * Not supported: lists and maps (use {@code mixin()}).
	 * 
	 */
	public void setObject(Object value) {
		if (value == null) setNull();
		else if (value instanceof Short || value instanceof Byte) setInt(((Number) value).intValue());
		else if (value instanceof Float) setDouble(((Float) value).doubleValue());
		else if (value instanceof Integer || value instanceof Double || value instanceof String || value instanceof Boolean) createAndSet(value);
		else if (value instanceof Long) setLong(((Long) value).longValue());
		else if (value instanceof Character) setChar(((Character) value).charValue());
		else if (value instanceof Enum<?>) setEnum((Enum<?>) value);
		else if (value instanceof JSON) setWithJSON((JSON) value);
		else throw new JSONException("Unknown type: " + value.getClass());
	}
	
	public void setInt(int value) {
		createAndSet(value);
	}
	
	public void setDouble(double value) {
		createAndSet(value);
	}
	
	public void setLong(long value) {
		if (value <= -MAXIMUM_PRECISION_DOUBLE || value >= MAXIMUM_PRECISION_DOUBLE) {
			setString(String.valueOf(value));
		} else {
			setDouble(value);
		}
	}
	
	public void setString(String value) {
		createAndSet(value);
	}
	
	public void setBoolean(boolean value) {
		createAndSet(value);
	}
	
	public void setEnum(Enum<?> value) {
		if (value == null) {
			createAndSet(null);
		} else {
			setString(value.name());
		}
	}
	
	public void setChar(char value) {
		setString(String.valueOf(value));
	}
	
	public void setNull() {
		createAndSet(null);
	}
	
	public void setEmptyList() {
		createAndSet(new ArrayList<Object>());
	}
	
	public void setEmptyMap() {
		createAndSet(new LinkedHashMap<Object, Object>());
	}
	
	public void setWithJSON(JSON json) {
		createAndSet(json.self);
	}
	
	/**
	 * If this element and the provided element are both lists, or both maps, this list/map is updated with the data from {@code json}.
	 * 
	 * Existing elements are overwritten.
	 */
	@SuppressWarnings("unchecked")
	public void mixin(JSON json) {
		Object me = self;
		Object other = json.self;
		
		if (other == UNDEFINED) return;
		
		if (me instanceof Map<?, ?> && other instanceof Map<?, ?>) {
			((Map<Object, Object>) me).putAll((Map<?, ?>) other);
			return;
		}
		
		if (me instanceof List<?> && other instanceof List<?>) {
			((List<Object>) me).addAll((List<?>) other);
		}
		
		if (me == UNDEFINED) throw new JSONException("Key " + getPath() + " does not exist");
		
		throw new JSONException("Mixin only possible if both elements are the same collection type (both JSON objects or both JSON lists)");
	}
	
	@SuppressWarnings("unchecked")
	private void createAndSet(Object toSet) {
		if (toSet == null) toSet = NULL;
		int depth = path.length;
		if (depth == 0) throw new JSONException("Setting the root element is not possible.");
		
		Object o = object;
		Object k = path[0];
		boolean x = k instanceof Number;
		Object m = null;
		boolean y = false;
		for (int i = 1; i <= depth; i++) {
			if (i < depth) {
				m = path[i];
				y = m instanceof Number;
				
				if (o instanceof List<?>) {
					if (!x) {
						structureError(i, "array");
						return;
					}
					List<?> list = (List<?>) o;
					int idx = ((Number) k).intValue();
					if (idx < -1 || idx > list.size()) {
						listTooSmallError(i - 1);
						return;
					}
					if (idx < list.size()) {
						o = list.get(idx);
						k = m; x = y;
						continue;
					}
				} else if (o instanceof Map<?, ?>) {
					if (x) {
						structureError(i, "object");
						return;
					}
					
					String key = String.valueOf(k);
					Map<?, ?> map = (Map<?, ?>) o;
					Object v = map.get(key);
					if (v != null) {
						o = v;
						k = m; x = y;
						continue;
					}
				}
			}
			
			Object z = i < depth ? (y ? new ArrayList<Object>() : new LinkedHashMap<Object, Object>()) : toSet;
			if (x) {
				int len = ((List<?>) o).size();
				int idx = ((Number) k).intValue();
				if (idx == -1 || idx == len) {
					path[i - 1] = len;
					((List<Object>) o).add(z);
				} else ((List<Object>) o).set(idx, z);
			} else ((Map<Object, Object>) o).put(String.valueOf(k), z);
			o = z;
			k = m; x = y;
		}
	}
	
	@Override public String toString() {
		if (self == UNDEFINED) return getPath() + ": UNDEFINED";
		
		return getPath() + ": " + JSONWriter.toJSON(self);
	}
	
	// Error throwing utility methods
	
	private void indexError(int index) {
		throw new JSONException("Not a valid list index: " + index);
	}
	
	private void structureError(int ct, String expected) {
		throw new JSONException("Key " + getPath(ct) + " contains a " +
				typeOf(dig(ct)) + " while a " + expected + " was expected");
	}
	
	private void listTooSmallError(int ct) {
		throw new JSONException("Key " + getPath(ct) + " contains a list that is too small");
	}
	
	private void notABoolean(String s) {
		throw new JSONException("Key " + getPath() + " contains '" + s + "' which is not a known boolean value.");
	}
	
	private void invalidType(String targetType) {
		if (self == UNDEFINED) throw new JSONException("Key " + getPath() + " does not exist");
		else {
			String jsType = typeOf(self);
			throw new JSONException("Key " + getPath() + " contains a " + jsType + " which is not convertable to a " + targetType);
		}
	}
	
	private void notIntegral(double v) {
		throw new JSONException("Key " + getPath() + " contains " + v + ", which is not an integral number");
	}
}
