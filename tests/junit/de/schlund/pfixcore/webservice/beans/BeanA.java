package de.schlund.pfixcore.webservice.beans;

public class BeanA {

    int foo;
    int bar;
    int baz;
    int test;
    
    public int getFoo() {
        return foo;
    }
    
    public void setFoo(int foo) {
        this.foo=foo;
    }
                                                                                                                                   
    @Exclude
    public int getBar() {
        return bar;
    }
    
    public void setBar(int bar) {
        this.bar=bar;
    }
                                                                                                                                   
    public int getBaz() {
        return baz;
    }
    
    public void setBaz(int baz) {
        this.baz=baz;
    }
    
    @Alias("mytest")
    public int getTest() {
        return test;
    }
    
    public void setTest(int test) {
        this.test=test;
    }

}
