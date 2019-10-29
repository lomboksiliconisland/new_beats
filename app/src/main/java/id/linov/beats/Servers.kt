package id.linov.beats

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


/**
 * Created by Hayi Nukman at 2019-10-29
 * https://github.com/ha-yi
 */
 
object Servers {
//    BeatsService
    val service: BeatsService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://103.129.222.97:8077/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create<BeatsService>(BeatsService::class.java)
    }

    fun addAssesment(data: AssesmentPojo) {
        GlobalScope.async {
            var trrNum = 0
            while (trrNum < 10) {
                trrNum++
                val resp = service.addAssessment(data).execute()
                if (resp.isSuccessful) break
                // retry on failed.
            }
        }
    }

    fun addBlock(data: BlockPojo) {
        GlobalScope.async {
            var trrNum = 0
            while (trrNum < 10) {
                trrNum++
                val resp = service.addBlock(data).execute()
                if (resp.isSuccessful) break
                // retry on failed.
            }
        }
    }
}