package com.softlayer.api;

/** Interface implemented by services who accept masks for some calls */
public interface Maskable<T extends Mask> {

    /** Overwrite the existing mask on this service with a new one and return it */
    public T withNewMask();

    /** Use the existing mask on this service or create it if not present */
    public T withMask();

    /** Set the mask to the given object */
    public void setMask(T mask);

    /** Set the mask to a string, formatted according to http://sldn.softlayer.com/article/Object-Masks */
    public void setMask(String mask);
}
