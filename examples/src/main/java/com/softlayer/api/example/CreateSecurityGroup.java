package com.softlayer.api.example;

import com.softlayer.api.ApiClient;
import com.softlayer.api.service.network.SecurityGroup;

/** Create security group example. */
public class CreateSecurityGroup extends Example {

    @Override
    public void run(ApiClient client) throws Exception {
        SecurityGroup.Service service = SecurityGroup.service(client);

        // Create a java object representing the new security group
        SecurityGroup sg = new SecurityGroup();
        sg.setName("javaTest");
        sg.setDescription("javaTestDescription");

        // Now call the security group service to create it
        System.out.println("Make call to create security group");
        SecurityGroup sgOut = service.createObject(sg);
        System.out.format("Created security group with name = %s\n", sgOut.getName());
    }

    public static void main(String[] args) throws Exception {
        new CreateSecurityGroup().start(args);
    }
}
