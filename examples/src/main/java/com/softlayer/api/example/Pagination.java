package com.softlayer.api.example;

import java.util.List;

import com.softlayer.api.ApiClient;
import com.softlayer.api.ResultLimit;
import com.softlayer.api.service.product.Package;

/** Get all product packages in paginated form */
public class Pagination extends Example {

    private static final int PAGE_SIZE = 10;

    @Override
    public void run(ApiClient client) throws Exception {
        Package.Service service = Package.service(client);

        int offset = 0;
        int total;
        do {
            // Result limits can include a limit and a 0-based offset
            service.setResultLimit(new ResultLimit(offset, PAGE_SIZE));
            List<Package> packages = service.getAllObjects();

            // The service contains the total item count that would return if a result limit wasn't present
            total = service.getLastResponseTotalItemCount();

            System.out.format("Retrieved %d-%d of %d items\n", offset + 1, offset + packages.size(), total);
            for (Package pkg : packages) {
                System.out.println("Package: " + pkg.getName());
            }
            offset += packages.size();
        } while (offset < total);
    }

    public static void main(String[] args) throws Exception {
        new Pagination().start(args);
    }
}
