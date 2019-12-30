# SoftLayer API Client for Java

[![Build Status](https://travis-ci.org/softlayer/softlayer-java.svg)](https://travis-ci.org/softlayer/softlayer-java)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.softlayer.api/softlayer-api-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.softlayer.api/softlayer-api-client)
[![Javadocs](https://www.javadoc.io/badge/com.softlayer.api/softlayer-api-client.svg)](https://www.javadoc.io/doc/com.softlayer.api/softlayer-api-client)

## Introduction

This library provides a JVM client for the [SoftLayer API](http://sldn.softlayer.com/article/SoftLayer-API-Overview). It
has code generated and compiled via Maven. The client can work with any Java 8+ runtime. It uses the code generation
project in `gen/` to generate the service and type related code. Although likely to work in resource-constrained
environments (i.e. Android, J2ME, etc), using this is not recommended; Use the
[REST](http://sldn.softlayer.com/article/REST) API instead.

By default the HTTP client is the Java `HttpUrlConnection` and the JSON marshalling is done by
[Gson](https://code.google.com/p/google-gson/). Both of these pieces can be exchanged for alternative implementations
(see below).

The `examples/` project has sample uses of the API. It can be executed from Maven while inside the `examples/` folder
via a command:

    mvn -q compile exec:java -Dexec.args="EXAMPLE_NAME API_USER API_KEY"

Where `EXAMPLE_NAME` is the unqualified class name of an example in the `com.softlayer.api.example` package (e.g.
`ListServers`), `API_USER` is your API username, and `API_KEY` is your API key. NOTE: Some examples order virtual
servers and may charge your account.

## Using

Add the library as a dependency using your favorite build tooling.

Note that the published client library is built upon the state of the API at the time of the version's release.
It will contain the generated artifacts as of that time only.
See "Building" for more information on how to regenerate the artifacts to get regular
additions to the SoftLayer API.

### Maven

```xml
<dependency>
  <groupId>com.softlayer.api</groupId>
  <artifactId>softlayer-api-client</artifactId>
  <version>0.2.8</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.softlayer.api:softlayer-api-client:0.2.8'
```

### Kotlin

```kotlin
compile("com.softlayer.api:softlayer-api-client:0.2.8")
```

### Creating a Client

All clients are instances of `ApiClient`. Currently there is only one implementation, the `RestApiClient`. Simply
instantiate it and provide your credentials:

```java
import com.softlayer.api.*;

ApiClient client = new RestApiClient().withCredentials("my user", "my api key");
```

If the end point isn't at the normal SoftLayer API, you can provide the prefix to the constructor of the
`RestApiClient`. By default it is set to `https://api.softlayer.com/rest/v3.1/`.

### Making API Calls

Once a client is created, it can be used to access services. There are hundreds of services to control your SoftLayer
account. A simple one is the `Account` service. Here's a call to get all of the hardware on the account:

```java
import com.softlayer.api.service.Account;
import com.softlayer.api.service.Hardware;

for (Hardware hardware : Account.service(client).getHardware()) {
    System.out.println("Hardware: " + hardware.getFullyQualifiedDomainName());
}
```

Some calls on a service require an ID to know what object to act on. This can be obtained by passing in the numeric ID
into the `service` method or by calling `asService` on an object that has an ID. Here's an example of soft-rebooting a
virtual server with "reboot-test" as the hostname:

```java
import com.softlayer.api.service.virtual.Guest;

for (Guest guest : Account.service(client).getVirtualGuests()) {
    if ("reboot-test".equals(guest.getHostname())) {
        guest.asService(client).rebootSoft();
    }
}
```

Some calls require sending in data. This is done by just instantiating the object and populating the data. Here's an
example of ordering a new virtual server: (Note running this can charge your account)

```java
import com.softlayer.api.service.virtual.Guest;

Guest guest = new Guest();
guest.setHostname("myhostname");
guest.setDomain("example.com");
guest.setStartCpus(1);
guest.setMaxMemory(1024);
guest.setHourlyBillingFlag(true);
guest.setOperatingSystemReferenceCode("UBUNTU_LATEST");
guest.setLocalDiskFlag(false);
guest.setDatacenter(new Location());
guest.getDatacenter().setName("dal05");
guest = Guest.service(client).createObject(guest);
System.out.println("Virtual server ordered with ID: " + guest.getId());
```

### Using Object Masks

Object masks are a great way to reduce the number of API calls to traverse the data graph of an object. For example,
here's how by just asking for an account, you can retrieve all your VLANs, their datacenter, and the firewall rules that
are on them:

```java
import com.softlayer.api.service.Account;
import com.softlayer.api.service.network.Vlan;
import com.softlayer.api.service.network.vlan.firewall.Rule;

Account.Service service = Account.service(client);
service.withMask().networkVlans().vlanNumber();
service.withMask().networkVlans().primaryRouter().datacenter().longName();
service.withMask().networkVlans().firewallRules().
    orderValue().
    sourceIpAddress().
    sourceIpCidr();

for (Vlan vlan : service.getObject().getNetworkVlans()) {
    for (Rule rule : vlan.getFirewallRules()) {
        System.out.format("Rule %d on VLAN %d in %s has some restriction on subnet %s/%d\n",
            rule.getOrderValue(), vlan.getVlanNumber(),
            vlan.getPrimaryRouter().getDatacenter().getLongName(),
            rule.getSourceIpAddress(), rule.getSourceIpCidr());
    }
}
```

All values of a type can be masked upon. If a value represents a primitive or collection of primitives, the same mask
it is called on is returned. Otherwise the mask of the other type is given. These translate into SoftLayer's
[string-based object mask format](http://sldn.softlayer.com/article/Object-Masks). A string or an instance of a mask
can be given directly by calling `setMask` on the service. Note, when object masks are added on a service object, they
will be sent with every service call unless removed via `clearMask` or overwritten via `withNewMask` or `setMask`.

### Asynchronous Invocation

All services also provide an asynchronous interface. This can be obtained from a service by calling `asAsync`. Here's an
example of getting all top level billing items and listing when they were created:

```java
import java.util.List;
import com.softlayer.api.service.ResponseHandler;
import com.softlayer.api.service.Account;
import com.softlayer.api.service.billing.Item;

Account.service(client).asAsync().getAllTopLevelBillingItems(new ResponseHandler<List<Item>>() {
    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onSuccess(List<Item> items) {
        for (Item item : items) {
            System.out.format("Billing item %s created on %s\n", item.getDescription(), item.getCreateDate());
        }
    }
}).get();
```

Using the default HTTP client, this runs the call in a separate thread and calls the handler parameter upon completion.
The `get` at the end basically makes it wait forever so the application doesn't exit out from under us. With the default
HTTP client the asynchronous invocations are handled by a simple thread pool that defaults to a cached thread pool that
creates daemon threads. It can be changed:

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import com.softlayer.api.service.RestApiClient;
import com.softlayer.api.service.http.ThreadPoolHttpClientFactory;
import com.softlayer.api.service.billing.Item;

RestApiClient client = new RestApiClient();
ExecutorService threadPool = Executors.newFixedThreadPool(3);
((ThreadPoolHttpClientFactory) client.getHttpClientFactory()).setThreadPool(threadPool);
```

Unlike using the default thread pool, you will be responsible for shutting down this overridden thread pool as
necessary. Other HTTP client implementations may handle asynchrony differently and not use thread pools at all.

In addition to the callback-style above, can also get the response as a `Future`. Here's an example of waiting 10
seconds to get all top level billing items:

```java
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import com.softlayer.api.service.Account;
import com.softlayer.api.service.billing.Item;

Future<List<Item>> response = Account.service(client).asAsync().getAllTopLevelBillingItems();
List<Item> items = response.get(10, TimeUnit.SECONDS);
for (Item item : items) {
    System.out.format("Billing item %s created on %s\n", item.getDescription(), item.getCreateDate());
}
```

### Thread Safety

No class in this library is guaranteed to be thread-safe. Callers are expected to keep this in mind when developing
with the library and to never use the same `ApiClient` (or any other object created with it) concurrently across
threads.

### Pagination

Sometimes there is a need to get the responses from the SoftLayer API in a paginated way instead of all at once. This
can be done by utilizing result limits. A result limit can be passed in with the number of results requested and the
offset to start reading from. Requesting smaller amounts of data will increase the performance of the call. Here is an
example of obtaining the first 10 tickets and outputting the total:

```java
import com.softlayer.api.ResultLimit;
import com.softlayer.api.service.Account;
import com.softlayer.api.service.Ticket;

Account.Service service = Account.service(client);
service.setResultLimit(new ResultLimit(10));
for (Ticket ticket : service.getTickets()) {
    System.out.println("Got ticket " + ticket.getTitle());
}
System.out.println("Total tickets on the account: " + service.getLastResponseTotalItemCount());
```

The services are not guaranteed to be thread-safe on their own, so it is difficult to obtain the total with
`getLastResponseTotalItemCount` when using the service asynchronously. To assist with this when using the callback
style, the `ResponseHandlerWithHeaders` can be used instead of `ResponseHandler`. But the safest way is to only use a
single service per thread.

### Differences from the API

Due to restrictions on identifiers in Java, some properties, methods, classes, and packages will be named differently
from the naming used by the API. For example, an API property that starts with a number will be prepended with 'z'.
[Java keywords](https://docs.oracle.com/javase/specs/jls/se8/html/jls-3.html#jls-3.9) that appear in identifiers may
also be replaced.

## Building

This project is intentionally provided without all of the service code. Normal Maven `install` and `package` commands
work properly and will regenerate the client. To specifically regenerate the Java service-related files, run:

    mvn generate-sources

## Customization

### Logging

Logging the requests and response to stdout can be enabled by invoking `withLoggingEnabled` on the `RestApiClient`. In
order to log elsewhere, simply make your own implementation of `RestApiClient` with `logRequest` and `logResponse`
overridden.

### HTTP Client

The default HTTP client that is used is the JVM's native `HttpUrlConnection`. In order to create your own, alternative
implementation you must implement `com.softlayer.api.http.HttpClientFactory`. Once implemented, this can be explicitly
set on the `RestApiClient` by calling `setHttpClientFactory`. Instead of setting the factory manually, you can also
leverage Java's `ServiceLoader` mechanism to have it used by default. This involves adding the fully qualified class
name of your implementation on a single line in a file in the JAR at
`META-INF/com.softlayer.api.http.HttpClientFactory`.

### JSON Marshalling

The default JSON marshaller that is used is [Gson](https://github.com/google/gson). In order to create your own,
alternative implementation you must implement `com.softlayer.api.json.JsonMarshallerFactory`. Once implemented, this
can be explicitly set on the `RestApiClient` by calling `setJsonMarshallerFactory`. Instead of setting the factory
manually, you can also leverage Java's `ServiceLoader` mechanism to have it used by default. This involves adding the
fully qualified class name of your implementation on a single line in a file in the JAR at
`META-INF/com.softlayer.api.json.JsonMarshallerFactory`.

## Copyright

This software is Copyright (c) 2020 The SoftLayer Developer Network. See the bundled LICENSE file for more information.
