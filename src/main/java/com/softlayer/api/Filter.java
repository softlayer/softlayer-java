package com.softlayer.api;

import java.util.Collections;
import java.util.Map;

public abstract class Filter {
    protected abstract Map<String, ?> getFilterMap();

    public enum SimpleOperation {
        EQUAL_TO {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return value;
            }
        },
        NOT_EQUAL_TO {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "!= " + value;
            }
        },
        GREATER_THAN {
            String withValue(String value) {
                requireNotNull(value);
                return "> " + value;
            }
        },
        GREATER_OR_EQUAL_TO {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return ">= " + value;
            }
        },
        LESS_THAN {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "< " + value;
            }
        },
        LESS_OR_EQUAL_TO {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "<= " + value;
            }
        },
        STARTS_WITH {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "^= " + value;
            }
        },
        NOT_STARTS_WITH {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "!^= " + value;
            }
        },
        ENDS_WITH {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "$= " + value;
            }
        },
        NOT_ENDS_WITH {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "!$= " + value;
            }
        },
        CONTAINS {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "*= " + value;
            }
        },
        NOT_CONTAINS {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "!*= " + value;
            }
        },
        EQUAL_TO_IGNORE_CASE {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "_= " + value;
            }
        },
        NOT_EQUAL_TO_IGNORE_CASE {
            @Override
            String withValue(String value) {
                requireNotNull(value);
                return "!_= " + value;
            }
        },
        NOT_NULL {
            @Override
            String withValue(String value) {
                requireNull(value);
                return "not null";
            }
        },
        IS_NULL {
            @Override
            String withValue(String value) {
                requireNull(value);
                return "null";
            }
        };

        void requireNull(String value) {
            if (value != null) {
                throw new IllegalArgumentException("Null is required for operation " + this);
            }
        }

        void requireNotNull(String value) {
            if (value == null) {
                throw new IllegalArgumentException("Null is not allowed for operation " + this);
            }
        }

        abstract String withValue(String value);
    }

    public static class SimpleFilter<T> extends Filter {
        private final SimpleOperation operation;
        private final T value;

        public SimpleFilter(SimpleOperation operation, T value) {
            this.operation = operation;
            this.value = value;
        }

        public SimpleOperation getOperation() {
            return operation;
        }

        public T getValue() {
            return value;
        }

        @Override
        protected Map<String, ?> getFilterMap() {
            return Collections.singletonMap("operation", getOperation().withValue(
                    getValue() == null ? null : getValue().toString()
            ));
        }
    }
}
