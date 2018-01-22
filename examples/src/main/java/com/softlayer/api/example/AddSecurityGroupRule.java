package com.softlayer.api.example;

import com.softlayer.api.ApiClient;
import com.softlayer.api.service.network.SecurityGroup;
import com.softlayer.api.service.network.securitygroup.Rule;

import java.util.ArrayList; 
import java.util.List;

/** Create a simple security group */
public class AddSecurityGroupRule extends Example {

    @Override
    public void run(ApiClient client) throws Exception {
        SecurityGroup.Service service = SecurityGroup.service(client);

        // create a new security group
        SecurityGroup sg = new SecurityGroup();
        sg.setName("javaTest");
        sg.setDescription("javaTestDescription");

        // create that security group
        SecurityGroup sgOut = service.createObject(sg);
        System.out.format("Created security group with ID: %s\n", sgOut.getId());

        // bind the service to the id of the newly created security group
        service = sgOut.asService(client);

        // Create a security group rule
        Rule rule = new Rule();
        rule.setDirection("ingress");
        rule.setProtocol("udp");

        List<Rule> newRules = new ArrayList<Rule>();
        newRules.add(rule);

        // Now add the rule(s) to the security group
        System.out.println("Adding rule(s) to security group");
        service.addRules(newRules);
    }

    public static void main(String[] args) throws Exception {
        new AddSecurityGroupRule().start(args);
    }
}
