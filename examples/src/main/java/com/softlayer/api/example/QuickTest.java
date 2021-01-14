package com.softlayer.api.example;

import com.softlayer.api.ApiClient;
import com.softlayer.api.RestApiClient;
import com.softlayer.api.service.Account;


/** A quick example for testing if authentication works. 

cd softlayer-java/examples
mvn -e -q compile exec:java -Dexec.args="QuickTest Bearer eyJraWQ.....
*/
public class QuickTest extends Example {

    @Override
    public void run(ApiClient client) throws Exception {
        client.withLoggingEnabled();
        System.out.format("Authorization: %s\n", client.getCredentials());
        Account.Service service = Account.service(client);

        Account account = service.getObject();
        System.out.format("Account Name: %s\n", account.getCompanyName());
    }

    public static void main(String[] args) throws Exception {
        new QuickTest().start(args);
    }
}
