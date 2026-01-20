package com.example.usinggeminiexample;

public class BabyName {
    private String name;
    private String meaning;
    private String origin;
    private String details;
    private String alternatives; // New field for suggestions
    private boolean isExpanded;
    private boolean isLoading;

    public BabyName(String name) {
        this.name = name;
        this.meaning = "";
        this.origin = "";
        this.details = "";
        this.alternatives = "";
        this.isExpanded = false;
        this.isLoading = false;
    }

    public String getName() { return name; }

    public String getMeaning() { return meaning; }
    public void setMeaning(String meaning) { this.meaning = meaning; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getAlternatives() { return alternatives; }
    public void setAlternatives(String alternatives) { this.alternatives = alternatives; }

    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }

    public boolean isLoading() { return isLoading; }
    public void setLoading(boolean loading) { isLoading = loading; }

    public boolean hasDetails() {
        return meaning != null && !meaning.isEmpty();
    }
}