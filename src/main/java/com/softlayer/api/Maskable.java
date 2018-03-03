package com.softlayer.api;

/** Interface implemented by services who accept masks for some calls */
public interface Maskable {

    /** Overwrite the existing mask on this service with a new one and return it */
    Mask withNewMask();

    /** Use the existing mask on this service or create it if not present */
    Mask withMask();

    /** Set the mask to the given object */
    void setMask(Mask mask);

    /** Set the mask to a string, formatted according to http://sldn.softlayer.com/article/Object-Masks */
    void setMask(String mask);
    
    /** Removes the mask from the service */
    void clearMask();
}
