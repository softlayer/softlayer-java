package com.softlayer.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.softlayer.api.ApiClient;
import com.softlayer.api.ResponseHandler;
import com.softlayer.api.ResultLimit;
import com.softlayer.api.annotation.ApiMethod;
import com.softlayer.api.annotation.ApiProperty;
import com.softlayer.api.annotation.ApiService;

public class TestThing extends Entity {

    @ApiProperty(canBeNullOrNotSet = true)
    protected Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        idSpecified = true;
        this.id = id;
    }

    protected boolean idSpecified;

    public boolean isIdSpecified() {
        return idSpecified;
    }

    public void unsetId() {
        id = null;
        idSpecified = false;
    }

    @ApiProperty("first")
    protected String first;

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    @ApiProperty("second")
    protected String second;

    public String getSecond() {
        return second;
    }

    public void setSecond(String second) {
        this.second = second;
    }

    @ApiProperty("testEntity")
    protected List<TestEntity> testEntity;

    public List<TestEntity> getTestEntity() {
        if (testEntity == null) {
            testEntity = new ArrayList<TestEntity>();
        }
        return testEntity;
    }

    public Service asService(ApiClient client) {
        return service(client, id);
    }

    public static Service service(ApiClient client) {
        return client.createService(Service.class, null);
    }

    public static Service service(ApiClient client, Long id) {
        return client.createService(Service.class, id == null ? null : id.toString());
    }

    @ApiService("SoftLayer_TestThing")
    public static interface Service extends com.softlayer.api.Service {
        public ServiceAsync asAsync();

        public Mask withNewMask();

        public Mask withMask();

        public void setMask(Mask mask);

        @ApiMethod("getObject")
        public TestThing getObject();

        @ApiMethod("getTestEntity")
        public List<TestEntity> getTestEntity();
    }

    public static interface ServiceAsync extends com.softlayer.api.ServiceAsync {
        public Mask withNewMask();

        public Mask withMask();

        public void setMask(Mask mask);

        public Future<TestThing> getObject();

        public Future<?> getObject(ResponseHandler<TestThing> handler);

        public Future<List<TestEntity>> getTestEntity();

        public Future<?> getTestEntity(ResponseHandler<List<TestEntity>> handler);
    }

    public static class Mask extends Entity.Mask {

        public Mask id() {
            withLocalProperty("id");
            return this;
        }

        public Mask first() {
            withLocalProperty("first");
            return this;
        }

        public Mask second() {
            withLocalProperty("second");
            return this;
        }

        public Mask testEntity() {
            return withSubMask("testEntity", Mask.class);
        }
    }
}
