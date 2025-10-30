package com.example.traduccioncotorra.Models;

import android.graphics.ColorSpace;

public class ModelLanguage {
    String languageCode;
    String languageTitle;

    public ModelLanguage(String languageCode, String languageTitle){
        this.languageCode = languageCode;
        this.languageTitle = languageTitle;
    }
    public String getLanguageCode(){
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
    public String  setLanguageTitle(){
        return languageTitle;
    }

    public void setLanguageTitle(String languageTitle) {
        this.languageTitle = languageTitle;
    }
}
