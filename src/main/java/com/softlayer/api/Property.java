package com.softlayer.api;

import com.softlayer.api.Filter.SimpleFilter;
import com.softlayer.api.Filter.SimpleOperation;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Map;

public abstract class Property<T> {
    public final String name;
    private Filter filter;

    Property(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public void equalTo(T value) {
        if (value == null) {
            isNull();
        } else {
            setFilter(new SimpleFilter<T>(SimpleOperation.EQUAL_TO, value));
        }
    }

    public void notEqualTo(T value) {
        if (value == null) {
            notNull();
        } else {
            setFilter(new SimpleFilter<T>(SimpleOperation.NOT_EQUAL_TO, value));
        }
    }

    public void isNull() {
        setFilter(new SimpleFilter<T>(SimpleOperation.IS_NULL, null));
    }

    public void notNull() {
        setFilter(new SimpleFilter<T>(SimpleOperation.NOT_EQUAL_TO, null));
    }

    protected Map<String, ?> getFilterMap() {
        if (getFilter() == null) {
            return Collections.emptyMap();
        }
        return getFilter().getFilterMap();
    }

    public static class BooleanProperty extends Property<Boolean> {
        public BooleanProperty(String name) {
            super(name);
        }
    }

    public static class ByteArrayProperty extends Property<byte[]> {
        public ByteArrayProperty(String name) {
            super(name);
        }

        @Override
        public void setFilter(Filter filter) {
            throw new UnsupportedOperationException("Byte arrays do not support filters");
        }
    }

    public static class DateTimeProperty extends Property<GregorianCalendar> {
        protected DateTimeProperty(String name) {
            super(name);
        }
    }

    public static class NumberProperty extends Property<Number> {
        protected NumberProperty(String name) {
            super(name);
        }

        public void greaterThan(Number value) {
            setFilter(new SimpleFilter<Number>(SimpleOperation.GREATER_THAN, value));
        }

        public void greaterOrEqualTo(Number value) {
            setFilter(new SimpleFilter<Number>(SimpleOperation.GREATER_OR_EQUAL_TO, value));
        }

        public void lessThan(Number value) {
            setFilter(new SimpleFilter<Number>(SimpleOperation.LESS_THAN, value));
        }

        public void lessOrEqualTo(Number value) {
            setFilter(new SimpleFilter<Number>(SimpleOperation.LESS_OR_EQUAL_TO, value));
        }
    }

    public static class StringProperty extends Property<String> {
        public StringProperty(String name) {
            super(name);
        }

        public void startsWith(String value) {
            setFilter(new SimpleFilter<String>(SimpleOperation.STARTS_WITH, value));
        }

        public void notStartsWith(String value) {
            setFilter(new SimpleFilter<String>(SimpleOperation.NOT_STARTS_WITH, value));
        }

        public void endsWith(String value) {
            setFilter(new SimpleFilter<String>(SimpleOperation.ENDS_WITH, value));
        }

        public void notEndsWith(String value) {
            setFilter(new SimpleFilter<String>(SimpleOperation.NOT_ENDS_WITH, value));
        }

        public void contains(String value) {
            setFilter(new SimpleFilter<String>(SimpleOperation.CONTAINS, value));
        }

        public void notContains(String value) {
            setFilter(new SimpleFilter<String>(SimpleOperation.NOT_CONTAINS, value));
        }

        public void equalToIgnoreCase(String value) {
            setFilter(new SimpleFilter<String>(SimpleOperation.EQUAL_TO_IGNORE_CASE, value));
        }

        public void notEqualToIgnoreCase(String value) {
            setFilter(new SimpleFilter<String>(SimpleOperation.NOT_EQUAL_TO_IGNORE_CASE, value));
        }
    }
}
