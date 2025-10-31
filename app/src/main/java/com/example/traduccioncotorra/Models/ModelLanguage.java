package com.example.traduccioncotorra.Models;

import android.graphics.ColorSpace;

public class ModelLanguage {
    private String languageCode;
    private String languageTitle;

    public ModelLanguage(String languageCode, String languageTitle) {
        this.languageCode = languageCode;
        this.languageTitle = languageTitle;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguageTitle() {
        return languageTitle;
    }

    public void setLanguageTitle(String languageTitle) {
        this.languageTitle = languageTitle;
    }

    @Override
    public String toString() {
        return languageTitle; // Esto es lo que se mostrará en el Spinner
    }
}
