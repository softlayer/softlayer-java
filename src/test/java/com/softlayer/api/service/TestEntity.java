package com.softlayer.api.service;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Future;

import com.softlayer.api.ApiClient;
import com.softlayer.api.Mask;
import com.softlayer.api.ResponseHandler;
import com.softlayer.api.ResultLimit;
import com.softlayer.api.annotation.ApiMethod;
import com.softlayer.api.annotation.ApiProperty;
import com.softlayer.api.annotation.ApiService;
import com.softlayer.api.annotation.ApiType;

@ApiType("SoftLayer_TestEntity")
public class TestEntity extends Entity {
    
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
    
    @ApiProperty("bar")
    protected String foo;
    
    public String getFoo() {
        return foo;
    }
    
    public void setFoo(String foo) {
        this.foo = foo;
    }
    
    @ApiProperty(canBeNullOrNotSet = true)
    protected String baz;
    protected boolean bazSpecified;
    
    public String getBaz() {
        return baz;
    }
    
    public void setBaz(String baz) {
        bazSpecified = true;
        this.baz = baz;
    }
    
    public boolean isBazSpecified() {
        return bazSpecified;
    }
    
    public void unsetBaz() {
        baz = null;
        bazSpecified = false;
    }
    
    @ApiProperty
    protected GregorianCalendar date;
    
    public GregorianCalendar getDate() {
        return date;
    }
    
    public void setDate(GregorianCalendar date) {
        this.date = date;
    }
    
    protected String notApiProperty;
    
    public String getNotApiProperty() {
        return notApiProperty;
    }
    
    public void setNotApiProperty(String notApiProperty) {
        this.notApiProperty = notApiProperty;
    }
    
    @ApiProperty
    protected TestEntity child;
    
    public TestEntity getChild() {
        return child;
    }
    
    public void setChild(TestEntity child) {
        this.child = child;
    }
    
    @ApiProperty
    protected List<TestEntity> moreChildren;
    
    public List<TestEntity> getMoreChildren() {
        if (moreChildren == null) {
            moreChildren = new ArrayList<TestEntity>();
        }
        return moreChildren;
    }

    @ApiProperty
    protected List<TestEntity> recursiveProperty;

    public List<TestEntity> getrecursiveProperty() {
        if (recursiveProperty == null) {
            recursiveProperty = new ArrayList<TestEntity>();
        }
        return recursiveProperty;
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
    
    @ApiService("SoftLayer_TestEntity")
    public static interface Service extends com.softlayer.api.Service {

        public ServiceAsync asAsync();
        public Mask withNewMask();
        public Mask withMask();
        public void setMask(Mask mask);
        
        @ApiMethod
        public String doSomethingStatic(Long param1, TestEntity param2);
        
        @ApiMethod("actualName")
        public List<TestEntity> fakeName();
        
        @ApiMethod(instanceRequired = true)
        public Void doSomethingNonStatic(GregorianCalendar param1);

        @ApiMethod("getRecursiveProperty")
        public String getRecursiveProperty();
    }
    
    public static interface ServiceAsync extends com.softlayer.api.ServiceAsync {

        public Mask withNewMask();
        public Mask withMask();
        public void setMask(Mask mask);
        
        public Future<String> doSomethingStatic(Long param1, TestEntity param2);
        public Future<?> doSomethingStatic(Long param1, TestEntity param2, ResponseHandler<String> handler);
        
        public Future<String> fakeName();
        public Future<?> fakeName(ResponseHandler<String> handler);
        
        public Future<Void> doSomethingNonStatic(GregorianCalendar param1);
        public Future<?> doSomethingNonStatic(GregorianCalendar param1, ResponseHandler<String> handler);
    }
    
    public static class Mask extends Entity.Mask {
        
        public Mask foo() {
            withLocalProperty("foo");
            return this;
        }
        
        public Mask baz() {
            withLocalProperty("baz");
            return this;
        }
        
        public Mask date() {
            withLocalProperty("date");
            return this;
        }
        
        public Mask child() {
            return withSubMask("child", Mask.class);
        }
        
        public Mask moreChildren() {
            return withSubMask("moreChildren", Mask.class);
        }

        public Mask recursiveProperty() {
            return withSubMask("recursiveProperty", Mask.class);
        }
    }

    public class TestService implements Service {

        @Override
        public ServiceAsync asAsync() {
            return null;
        }

        @Override
        public Mask withNewMask() {
            return null;
        }

        @Override
        public Mask withMask() {
            return null;
        }

        @Override
        public void setMask(com.softlayer.api.Mask mask) {

        }

        @Override
        public void setMask(String mask) {

        }

        @Override
        public void clearMask() {

        }

        @Override
        public void setMask(Mask mask) {

        }

        @Override
        public String doSomethingStatic(Long param1, TestEntity param2) {
            return null;
        }

        @Override
        public List<TestEntity> fakeName() {
            return null;
        }

        @Override
        public Void doSomethingNonStatic(GregorianCalendar param1) {
            return null;
        }

        @Override
        public String getRecursiveProperty() {
            System.out.print("This is playing");
            return "Thats playing to win";
        }

        @Override
        public ResultLimit getResultLimit() {
            return null;
        }

        @Override
        public ResultLimit setResultLimit(ResultLimit limit) {
            return null;
        }

        @Override
        public Integer getLastResponseTotalItemCount() {
            return null;
        }
    }
}