package de.schlund.pfixcore.example.iwrapper;

import de.schlund.pfixcore.generator.annotation.IWrapper;
import de.schlund.pfixcore.generator.annotation.Param;

@IWrapper(beanRef="enctest_handler")
public class EncodingTest {
    
    private String text;
    private String encoding;
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getText() {
        return text;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Param(mandatory=false)
    public String getEncoding() {
        return encoding;
    }

}
