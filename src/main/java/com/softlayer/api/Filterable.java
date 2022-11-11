package com.softlayer.api;

/**
 * Interface implemented by services that accept filters for some calls.
 */
public interface Filterable {
    /** Overwrite the existing filter on this object with a new one and return it. */
    public Mask withNewFilter();

    /** Use the existing filter on this object or create it if not present. */
    public Mask withFilter();

    /** Set the filter to the given object. */
    public void setFilter(Mask filter);

    /** Set the filter to a string. */
    public void setFilter(String filter);

    /** Removes the filter from this object. */
    public void clearFilter();
}
