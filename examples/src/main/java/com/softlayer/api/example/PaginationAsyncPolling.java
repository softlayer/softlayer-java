package com.softlayer.api.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.softlayer.api.ApiClient;
import com.softlayer.api.ResultLimit;
import com.softlayer.api.http.HttpClientFactory;
import com.softlayer.api.http.ThreadPooledHttpClientFactory;
import com.softlayer.api.service.product.Package;

/** Asynchronous version of {@link Pagination} using the polling approach */
public class PaginationAsyncPolling extends Example {

    private static final int PAGE_SIZE = 10;

    @Override
    public void run(ApiClient client) throws Exception {
        // Let's give the default HTTP client factory a fixed thread pool of 3 threads
        ExecutorService threadPool = Executors.newFixedThreadPool(3);
        ((ThreadPooledHttpClientFactory) HttpClientFactory.getDefault()).setThreadPool(threadPool);

        // Asynchronous responses are held so they can be waited on once all are submitted
        List<PackageResponseWrapper> responses = new ArrayList<PackageResponseWrapper>();

        // To know how many calls have to be made to get all items, an initial call is required to get the
        //  first set of data AND the total count
        System.out.format("Retrieving first %d items\n", PAGE_SIZE);
        Package.ServiceAsync service = Package.service(client).asAsync();
        service.setResultLimit(new ResultLimit(0, PAGE_SIZE));
        PackageResponseWrapper first = new PackageResponseWrapper(0, service.getAllObjects(), service);
        responses.add(first);
        first.response.get();

        // Once the total is obtained, a call for all pages after the first can happen
        for (int i = PAGE_SIZE; i < first.service.getLastResponseTotalItemCount(); i += PAGE_SIZE) {
            // We create a new service each time because the result limit is specific to the service, not the call
            //  so it may be overwritten if we reused it
            System.out.format("Retrieving %d-%d of %d items\n", i + 1, i + PAGE_SIZE,
                first.service.getLastResponseTotalItemCount());
            service = Package.service(client).asAsync();
            service.setResultLimit(new ResultLimit(i, PAGE_SIZE));
            responses.add(new PackageResponseWrapper(i, service.getAllObjects(), service));
        }

        // The thread pool needs to be closed so nothing more can be added to it and it terminates after last call
        threadPool.shutdown();

        // A set is needed to hold the resulting packages ordered by name
        final NavigableSet<Package> packages = new TreeSet<Package>(new Comparator<Package>() {
            @Override
            public int compare(Package pkg1, Package pkg2) {
                return pkg1.getName().compareToIgnoreCase(pkg2.getName());
            }
        });

        // Unlike the callback approach, this approach guarantees they come in the order requested since a blocking
        //  call to get() is in the request order
        for (PackageResponseWrapper response : responses) {
            packages.addAll(response.response.get());
            System.out.format("Retrieved %d-%d of %d items\n", response.offset + 1, response.offset +
                response.response.get().size(), first.service.getLastResponseTotalItemCount());
        }

        System.out.println("Packages:");
        for (Package pkg : packages) {
            System.out.println("  " + pkg.getName());
        }
    }

    /** Simple wrapper to hold the response and the offset */
    static class PackageResponseWrapper {
        public final Future<List<Package>> response;
        public final int offset;
        public final Package.ServiceAsync service;

        public PackageResponseWrapper(int offset, Future<List<Package>> response, Package.ServiceAsync service) {
            this.offset = offset;
            this.response = response;
            this.service = service;
        }
    }

    public static void main(String[] args) throws Exception {
        new PaginationAsyncPolling().start(args);
    }
}
