package com.softlayer.api.example;

import java.util.concurrent.TimeUnit;

import com.softlayer.api.ApiClient;
import com.softlayer.api.service.Location;
import com.softlayer.api.service.container.virtual.guest.Configuration;
import com.softlayer.api.service.container.virtual.guest.configuration.Option;
import com.softlayer.api.service.software.component.Password;
import com.softlayer.api.service.virtual.Guest;
import com.softlayer.api.service.virtual.guest.block.Device;

/** Order an hourly virtual server and wait for it to complete provisioning */
public class OrderVirtualServer extends Example {

    @Override
    public void run(ApiClient client) throws Exception {
        Guest.Service service = Guest.service(client);

        // This call helps by giving possible options that can be placed on the order
        Configuration orderOptions = service.getCreateObjectOptions();

        // A guest object represents a virtual server. The code below is an order for a virtual server
        //  with 2 cores, billed hourly, with local disk and the latest Ubuntu distribution.
        // Ref: http://sldn.softlayer.com/reference/services/SoftLayer_Virtual_Guest/createObject
        Guest guest = new Guest();
        guest.setHostname("api-test");
        guest.setDomain("example.com");
        guest.setStartCpus(2L);
        guest.setHourlyBillingFlag(true);
        guest.setLocalDiskFlag(true);
        guest.setOperatingSystemReferenceCode("UBUNTU_LATEST");

        // By using the options we can get the smallest amount of memory without hardcoding it
        for (Option option : orderOptions.getMemory()) {
            if (guest.getMaxMemory() == null || guest.getMaxMemory() > option.getTemplate().getMaxMemory()) {
                guest.setMaxMemory(option.getTemplate().getMaxMemory());
            }
        }

        // Datacenters are represented by their name. Here the Amsterdam datacenter is used.
        guest.setDatacenter(new Location());
        guest.getDatacenter().setName("ams01");

        // Block devices are indexed starting at 0, but 1 is reserved for swap usage. Options can be used
        //  here also to set the smallest disk size allowed for the disk at index 0.
        Device device = null;
        for (Option option : orderOptions.getBlockDevices()) {
            for (Device candidate : option.getTemplate().getBlockDevices()) {
                if ("0".equals(candidate.getDevice()) && (device == null ||
                        device.getDiskImage().getCapacity() > candidate.getDiskImage().getCapacity())) {
                    device = candidate;
                }
            }
        }
        guest.getBlockDevices().add(device);

        System.out.println("Ordering virtual server");
        guest = service.createObject(guest);
        System.out.format("Order completed for virtual server with UUID: %s\n", guest.getGlobalIdentifier());

        // Asking for the service of a result binds the identifier (the "id" in this case) to the resulting
        //  service. Some calls require an ID which can also be done using the services constructor or
        //  withId/setId. This is the equivalent of Guest.service(client, guest.getId()).
        service = guest.asService(client);

        // Once guests are ordered, they take a couple of minutes to provision. This waits 15 minutes just
        //  to be safe.
        int minutesToWait = 15;
        System.out.format("Waiting for completion for a max of %d minutes\n", minutesToWait);
        int timesChecked = 0;
        service.withMask().status().name();
        service.withMask().provisionDate();
        do {
            TimeUnit.MINUTES.sleep(1);
            guest = service.getObject();
            System.out.format("Virtual server %d is not provisioned after %d minutes; Waiting one minute\n",
                    guest.getId(), timesChecked + 1);
        } while (++timesChecked < minutesToWait &&
                (!"Active".equals(guest.getStatus().getName()) || guest.getProvisionDate() == null));

        // Either the virtual server became active or we timed out
        if (!"Active".equals(guest.getStatus().getName()) || guest.getProvisionDate() == null) {
            System.out.format("Virtual server %d is still not provisioned after %d minutes; Quitting\n",
                guest.getId(), minutesToWait);
        } else {
            // Using a mask, we can ask for the operating system password
            Guest.Mask mask = service.withNewMask();
            mask.primaryIpAddress();
            mask.operatingSystem().passwords();
            guest = service.getObject();
            if (guest.getOperatingSystem() == null) {
                System.out.println("Unable to find operating system on completed guest");
            } else {
                Password root = null;
                for (Password password : guest.getOperatingSystem().getPasswords()) {
                    if ("root".equals(password.getUsername())) {
                        root = password;
                        break;
                    }
                }
                if (root == null) {
                    System.out.println("Unable to find root password");
                } else {
                    System.out.format("Virtual server done, can now login to %s with root password %s\n",
                        guest.getPrimaryIpAddress(), root.getPassword());
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new OrderVirtualServer().start(args);
    }
}
