package com.softlayer.api.example;

import com.softlayer.api.ApiClient;
import com.softlayer.api.ResponseHandler;
import com.softlayer.api.service.Account;
import com.softlayer.api.service.Hardware;
import com.softlayer.api.service.virtual.Guest;

/** Asynchronous version of {@link ListServers} */
public class ListServersAsync extends Example {

    @Override
    public void run(ApiClient client) throws Exception {
        Account.Service service = Account.service(client);

        // To get specific information on an account (servers in this case) a mask is provided
        service.withMask().hardware().
            fullyQualifiedDomainName().
            primaryIpAddress().
            primaryBackendIpAddress();
        service.withMask().hardware().operatingSystem().softwareLicense().softwareDescription().
            manufacturer().
            name().
            version();
        service.withMask().virtualGuests().
            fullyQualifiedDomainName().
            primaryIpAddress().
            primaryBackendIpAddress();
        service.withMask().virtualGuests().operatingSystem().softwareLicense().softwareDescription().
            manufacturer().
            name().
            version();

        // Calling getObject will now use the mask
        // By using asAsync this runs on a separate thread pool, and get() is called on the resulting future
        //  to wait until completion. In most cases other processing would continue before finishing.
        service.asAsync().getObject(new ResponseHandler<Account>() {
            
            @Override
            public void onSuccess(Account account) {
                System.out.format("\n%d physical servers\n", account.getHardware().size());
                for (Hardware hardware : account.getHardware()) {
                    System.out.format("Host: %s, IP: %s (%s)\n", hardware.getFullyQualifiedDomainName(),
                        hardware.getPrimaryIpAddress(), hardware.getPrimaryBackendIpAddress());
                }
                System.out.format("\n%d virtual servers\n", account.getVirtualGuests().size());
                for (Guest guest : account.getVirtualGuests()) {
                    System.out.format("Host: %s, IP: %s (%s)\n", guest.getFullyQualifiedDomainName(),
                        guest.getPrimaryIpAddress(), guest.getPrimaryBackendIpAddress());
                }
            }
            
            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
            }
        }).get();
    }

    public static void main(String[] args) throws Exception {
        new ListServersAsync().start(args);
    }
}
