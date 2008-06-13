package org.pustefixframework.tutorial.usermanagement;

import java.net.URL;
import java.util.Date;

import org.pustefixframework.tutorial.caster.ToURL;

import de.schlund.pfixcore.generator.annotation.Caster;
import de.schlund.pfixcore.generator.annotation.IWrapper;
import de.schlund.pfixcore.generator.annotation.Param;
import de.schlund.pfixcore.generator.annotation.Transient;

@IWrapper(name="UserWrapper", ihandler=UserHandler.class)
public class User {
    private int id;
    private String name;
    private String email;
    private Date birthday;
    private boolean admin;
    private URL homepage;
    private String sex;

    @Transient
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Param(name="name", mandatory=true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Param(name="email", mandatory=true)
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Param(name="birthday", mandatory=true)
    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    @Param(name="admin", mandatory=false)
    public boolean getAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    @Param(name="homepage", mandatory=false)
    @Caster(type=ToURL.class)
    public URL getHomepage() {
        return homepage;
    }

    public void setHomepage(URL homepage) {
        this.homepage = homepage;
    }

    @Param(name="sex", mandatory=true)
    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
