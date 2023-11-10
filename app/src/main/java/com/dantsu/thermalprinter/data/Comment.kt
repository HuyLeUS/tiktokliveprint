package com.dantsu.thermalprinter.data

import com.fasterxml.jackson.annotation.JsonProperty

class Comment {
    @JvmField
    @JsonProperty("comment")
    var comment: String? = null

    @JvmField
    @JsonProperty("uniqueId")
    var uniqueId: String? = null

    @JsonProperty("userId")
    var userId: String? = null

    @JvmField
    @JsonProperty("nickname")
    var nickname: String? = null

    @JsonProperty("secUid")
    var secUid: String? = null

    @JsonProperty("followRole")
    var followRole = 0

    @JsonProperty("userBadges")
    var userBadges: List<Any>? = null

    @JvmField
    @JsonProperty("profilePictureUrl")
    var profilePictureUrl: String? = null

    @JsonProperty("userDetail")
    var userDetail: UserDetail? = null
}