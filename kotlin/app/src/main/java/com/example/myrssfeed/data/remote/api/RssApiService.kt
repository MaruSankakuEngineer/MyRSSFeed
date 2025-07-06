package com.example.myrssfeed.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Url

interface RssApiService {
    @GET
    suspend fun getRssFeed(@Url url: String): ResponseBody
} 