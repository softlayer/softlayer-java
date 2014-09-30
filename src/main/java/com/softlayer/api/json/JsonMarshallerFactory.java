package com.softlayer.api.json;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Base class for an JSON marshaller factory. By default the {@link GsonJsonMarshallerFactory} is used.
 * This can be overridden by a custom marshaller factory using the {@link ServiceLoader} pattern. It is
 * also cached (though not necessarily guaranteed to be a singleton or instantiated only once) upon first
 * access.
 */
public abstract class JsonMarshallerFactory {
    
    static volatile JsonMarshallerFactory defaultFactory = null;
    
    public static JsonMarshallerFactory getDefault() {
        return getDefault(true);
    }

    static JsonMarshallerFactory getDefault(boolean cache) {
        // We don't mind the race condition that can occur by possibly creating multiple factories. We make
        //  no guarantees that there is only one factory ever created even when cache is true
        JsonMarshallerFactory result = cache ? defaultFactory : null;
        if (result == null) {
            Iterator<JsonMarshallerFactory> iterator = ServiceLoader.load(JsonMarshallerFactory.class).iterator();
            if (!iterator.hasNext()) {
                // Default to Gson (which may not be present, but we just let NoClassDefFoundError throw)
                result = new GsonJsonMarshallerFactory();
            } else {
                result = iterator.next();
                if (iterator.hasNext()) {
                    throw new RuntimeException("Ambiguous JSON marshaller factories: " + result.getClass() +
                            ", " + iterator.next().getClass() + " and possibly more");
                }
            }
            if (cache) {
                defaultFactory = result;
            }
        }
        return result;
    }
    
    /**
     * Get a JSON marshaller for use for either marshalling or unmarshalling. The resulting marshaller is
     * only used once, but this method may be called from multiple threads. 
     */
    public abstract JsonMarshaller getJsonMarshaller();
}
