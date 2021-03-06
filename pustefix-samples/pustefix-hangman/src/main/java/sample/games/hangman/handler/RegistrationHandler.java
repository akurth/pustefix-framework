package sample.games.hangman.handler;


import org.springframework.beans.factory.annotation.Autowired;

import sample.games.hangman.context.ContextUser;
import sample.games.hangman.wrapper.Registration;
import de.schlund.pfixcore.generator.IHandler;
import de.schlund.pfixcore.generator.IWrapper;
import de.schlund.pfixcore.workflow.Context;

public class RegistrationHandler implements IHandler {

    private ContextUser user;

    public void handleSubmittedData(Context context, IWrapper wrapper) throws Exception {

        Registration registration = (Registration)wrapper;
        user.setName(registration.getName());
    }

    public boolean isActive(Context context) throws Exception {
        return user.getName() == null;
    }

    public boolean needsData(Context context) throws Exception {
        return user.getName() == null;
    }

    public boolean prerequisitesMet(Context context) throws Exception {
        return true;
    }

    public void retrieveCurrentStatus(Context context, IWrapper wrapper) throws Exception {
        if(user.getName() != null) {
            Registration registration = (Registration)wrapper;
            registration.setName(user.getName());
        }
    }

    @Autowired
    public void setUser(ContextUser user) {
        this.user = user;
    }
    
}
