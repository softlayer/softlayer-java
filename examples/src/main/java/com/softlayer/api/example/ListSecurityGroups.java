package com.softlayer.api.example;

import com.softlayer.api.ApiClient;
import com.softlayer.api.service.Account;
import com.softlayer.api.service.network.SecurityGroup;

/** List all security groups for an account */
public class ListSecurityGroups extends Example {

    @Override
    public void run(ApiClient client) throws Exception {
        // Get the Account service
        Account.Service service = Account.service(client);
        
        // To get specific information on an account (security groups in this case) a mask is provided
        service.withMask().securityGroups();
        
        // Calling getObject will now use the mask
        Account account = service.getObject();

        System.out.format("\nFound %d security groups\n", account.getSecurityGroups().size());
        
        for (SecurityGroup sg : account.getSecurityGroups()) {
            System.out.format("id: %s name: %s \n", sg.getId(), sg.getName());
        }
    }

    public static void main(String[] args) throws Exception {
        new ListSecurityGroups().start(args);
    }
}
