package com.softlayer.api.example;

import com.softlayer.api.ApiClient;
import com.softlayer.api.service.Account;
import com.softlayer.api.service.Hardware;
import com.softlayer.api.service.software.Description;
import com.softlayer.api.service.virtual.Guest;

/** List all physical and virtual servers on an account */
public class ListServers extends Example {

    @Override
    public void run(ApiClient client) throws Exception {
        Account.Service service = Account.service(client);
        
        // To get specific information on an account (servers in this case) a mask is provided
        Hardware.Mask hardwareMask = service.withMask().hardware();
        hardwareMask.fullyQualifiedDomainName();
        hardwareMask.primaryIpAddress();
        hardwareMask.primaryBackendIpAddress();
        Description.Mask descriptionMask = hardwareMask.operatingSystem().softwareLicense().softwareDescription();
        descriptionMask.manufacturer();
        descriptionMask.name();
        descriptionMask.version();
        Guest.Mask guestMask = service.withMask().virtualGuests();
        guestMask.fullyQualifiedDomainName();
        guestMask.primaryIpAddress();
        guestMask.primaryBackendIpAddress();
        descriptionMask = guestMask.operatingSystem().softwareLicense().softwareDescription();
        descriptionMask.manufacturer();
        descriptionMask.name();
        descriptionMask.version();

        // Calling getObject will now use the mask
        Account account = service.getObject();

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

    public static void main(String[] args) throws Exception {
        new ListServers().start(args);
    }
}
