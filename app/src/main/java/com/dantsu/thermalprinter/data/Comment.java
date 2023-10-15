package com.dantsu.thermalprinter.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Comment {
    @JsonProperty("comment")
    public String comment;
    @JsonProperty("uniqueId")
    public String uniqueId;
    @JsonProperty("userId")
    public String userId;
    @JsonProperty("nickname")
    public String nickname;
    @JsonProperty("secUid")
    public String secUid;
    @JsonProperty("followRole")
    public int followRole;
    @JsonProperty("userBadges")
    public List<Object> userBadges;
    @JsonProperty("profilePictureUrl")
    public String profilePictureUrl;
    @JsonProperty("userDetail")
    public UserDetail userDetail ;
}
