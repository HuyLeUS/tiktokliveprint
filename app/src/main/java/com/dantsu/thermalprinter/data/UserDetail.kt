package com.dantsu.thermalprinter.data

import com.fasterxml.jackson.annotation.JsonProperty

class UserDetail {
    @JsonProperty("createTime")
    var createTime = 0

    @JsonProperty("bioDescription")
    var bioDescription: String? = null

    @JsonProperty("profilePictureUrl")
    var profilePictureUrl: List<String>? = null
}