package com.softlayer.api.example;

import com.softlayer.api.ApiClient;
import com.softlayer.api.service.Account;
import com.softlayer.api.service.Hardware;

public class ListServers extends Example {

    @Override
    public void run(ApiClient client) throws Exception {
        Account.Service service = Account.service(client);
        service.withMask().companyName();
        service.withMask().hardware().fullyQualifiedDomainName();
        service.withMask().hardware();
        Account account = service.getObject();
        System.out.format("Got %d servers on account for: %s\n",
                account.getHardware().size(), account.getCompanyName());
        for (Hardware hardware : account.getHardware()) {
            System.out.format("Host: %s\n", hardware.getFullyQualifiedDomainName());
        }
    }

    public static void main(String[] args) throws Exception {
        new ListServers().start(args);
    }
}
