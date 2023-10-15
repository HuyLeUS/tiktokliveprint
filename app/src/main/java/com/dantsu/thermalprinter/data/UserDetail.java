package com.dantsu.thermalprinter.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UserDetail {
    @JsonProperty("createTime")
    public int createTime;
    @JsonProperty("bioDescription")
    public String bioDescription;
    @JsonProperty("profilePictureUrl")
    public List<String> profilePictureUrl;
}
