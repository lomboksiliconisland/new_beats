package id.linov.beats

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST


/**
 * Created by Hayi Nukman at 2019-10-29
 * https://github.com/ha-yi
 */

interface BeatsService {
    @POST("assessment")
    fun addAssessment(@Body data: AssesmentPojo): Call<Any>

    @POST("block")
    fun addBlock(@Body data: BlockPojo): Call<Any>
}