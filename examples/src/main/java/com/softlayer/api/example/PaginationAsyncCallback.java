package com.softlayer.api.example;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.softlayer.api.ApiClient;
import com.softlayer.api.ResponseHandlerWithHeaders;
import com.softlayer.api.ResultLimit;
import com.softlayer.api.http.HttpClientFactory;
import com.softlayer.api.http.ThreadPooledHttpClientFactory;
import com.softlayer.api.service.product.Package;

/** Asynchronous version of {@link Pagination} using the callback approach */
public class PaginationAsyncCallback extends Example {

    private static final int PAGE_SIZE = 10;

    @Override
    public void run(ApiClient client) throws Exception {
        // Let's give the default HTTP client factory a fixed thread pool of 3 threads
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        ((ThreadPooledHttpClientFactory) HttpClientFactory.getDefault()).setThreadPool(threadPool);

        // A thread-safe set is needed to hold the resulting packages ordered by name
        final NavigableSet<Package> packages = new ConcurrentSkipListSet<Package>(new Comparator<Package>() {
            @Override
            public int compare(Package pkg1, Package pkg2) {
                return pkg1.getName().compareToIgnoreCase(pkg2.getName());
            }
        });

        // To know how many calls have to be made to get all items, an initial call is required to get the
        //  first set of data AND the total count
        System.out.format("Retrieving first %d items\n", PAGE_SIZE);
        PackageHandler first = new PackageHandler(packages, 0);
        Package.Service service = Package.service(client);
        service.setResultLimit(new ResultLimit(0, PAGE_SIZE));
        service.asAsync().getAllObjects(first).get();

        // Once the total is obtained, a call for all pages after the first can happen
        for (int i = PAGE_SIZE; i < first.total; i += PAGE_SIZE) {
            // We create a new service each time because the result limit is specific to the service, not the call
            //  so it may be overwritten if we reused it
            System.out.format("Retrieving %d-%d of %d items\n", i + 1, PAGE_SIZE, first.total);
            service = Package.service(client);
            service.setResultLimit(new ResultLimit(i, PAGE_SIZE));
            service.asAsync().getAllObjects(new PackageHandler(packages, i));
        }

        // It can take a few seconds to obtain all the information, wait 3 minutes just to be safe
        threadPool.shutdown();
        threadPool.awaitTermination(3, TimeUnit.MINUTES);

        System.out.println("Packages:");
        for (Package pkg : packages) {
            System.out.println("  " + pkg.getName());
        }
    }

    /** Asynchronous handler for handling a subset of packages */
    static class PackageHandler extends ResponseHandlerWithHeaders<List<Package>> {
        private final int offset;
        private final Set<Package> packages;
        private Integer total;

        public PackageHandler(Set<Package> packages, int offset) {
            this.packages = packages;
            this.offset = offset;
        }

        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
        }

        @Override
        public void onSuccess(List<Package> value) {
            packages.addAll(value);
            total = getLastResponseTotalItemCount();
            System.out.format("Retrieved %d-%d of %d items\n", offset + 1, offset + value.size(), total);
        }
    }

    public static void main(String[] args) throws Exception {
        new PaginationAsyncCallback().start(args);
    }
}
