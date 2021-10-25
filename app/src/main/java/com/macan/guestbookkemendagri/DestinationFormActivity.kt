package com.macan.guestbookkemendagri

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.macan.guestbookkemendagri.databinding.ActivityDestinationFormBinding
import com.macan.guestbookkemendagri.helper.Helper
import com.macan.guestbookkemendagri.models.destination.DetailDestinationItem
import com.macan.guestbookkemendagri.models.destination.ListDestinationItem
import com.macan.guestbookkemendagri.models.destination.ModelDestination
import com.macan.guestbookkemendagri.models.destination.PrimaryDestinationItem
import com.macan.guestbookkemendagri.network.RetrofitClient
import okhttp3.ResponseBody
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException

class DestinationFormActivity : AppCompatActivity() {

    private val binding: ActivityDestinationFormBinding by lazy {
        ActivityDestinationFormBinding.inflate(layoutInflater)
    }

    private val base64Image: String? by lazy {
        intent.getStringExtra("image")
    }

    private val destinationAdapter: DestinationAdapter by lazy {
        DestinationAdapter(
                this,
                consolidatedDestinations,
                onDetailSelected = { data ->
                    selectedDetailId = data.id
                    Log.i("detail selected", "name = ${data.name}, id = ${data.id}")
                }
        )
    }

    private var listDestinations: ArrayList<ModelDestination> = ArrayList()
    private var consolidatedDestinations = mutableListOf<ListDestinationItem>()
    private var selectedDetailId: Int? = null
    private var nik : String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding){
            if(base64Image != null){
                rvDestination.also{
                    it.adapter = destinationAdapter
                    val linearLayoutManager = LinearLayoutManager(this@DestinationFormActivity)
                    linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
                    it.layoutManager = linearLayoutManager

                }

                RetrofitClient.instance.findGuest(base64Image!!).enqueue(object :
                        Callback<ResponseBody> {
                    override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            try {
                                val result = JSONObject(response.body()!!.string())
                                Log.i("resultData", result.toString())
                                val persons = result.getJSONObject("person").getJSONArray("data")
                                val person = persons.getJSONObject(0)
                                val destinations = result.getJSONArray("destination")
                                nik = person.getString("NIK")

                                listDestinations = ArrayList()
                                for (i in 0 until destinations.length()) {
                                    val destination = destinations.getJSONObject(i)
                                    listDestinations.add(ModelDestination(destination.getInt("id"), destination.getString("primary"), destination.getString("detail")))
                                }

                                val groupedDestinations: Map<String, List<ModelDestination>> = listDestinations.groupBy {
                                    it.getPrimary()
                                }

                                //start grouping here
                                consolidatedDestinations.clear()
                                for (primary: String in groupedDestinations.keys) {
                                    consolidatedDestinations.add(PrimaryDestinationItem(primary))
                                    val groupItems: List<ModelDestination>? = groupedDestinations[primary]
                                    groupItems?.forEach {
                                        consolidatedDestinations.add(DetailDestinationItem(it.getId(), it.getDetail()))
                                    }
                                }

                                val dos1Vaccinated = person.getInt("VAKSIN_1") == 1
                                val dos1VaccinationDate = if(dos1Vaccinated){Helper.convertDateTimeToReadable(person.getString("VAKSIN_1_TGL"), 2)}else{""}
                                val dos2Vaccinated = person.getInt("VAKSIN_2") == 1
                                val dos2VaccinationDate = if(dos2Vaccinated){Helper.convertDateTimeToReadable(person.getString("VAKSIN_2_TGL"), 2)}else{""}
                                val dos3Vaccinated = person.getInt("VAKSIN_3") == 1 //belum dimasukkin ke front end, tunggu permintaan dari client
                                val dos3VaccinationDate = if(dos3Vaccinated){Helper.convertDateTimeToReadable(person.getString("VAKSIN_3_TGL"), 2)}else{""}

                                //set the content
                                photoProfile.setImageBitmap(Helper.decodeImage(person.getString("FACE")))
                                tvNik.text = nik
                                tvNama.text = person.getString("NAMA_LGKP")
                                tvTtl.text = "${person.getString("TMPT_LHR")}, ${Helper.convertDateTimeToReadable(person.getString("TGL_LHR"), 2)}"
                                if(dos1Vaccinated){
                                    tvVaksin1.text = "$dos1VaccinationDate (SUDAH)"
                                }else{
                                    tvVaksin1.text = " - (BELUM)"
                                }

                                if(dos2Vaccinated){
                                    tvVaksin2.text = "$dos2VaccinationDate (SUDAH)"
                                }else{
                                    tvVaksin2.text = " - (BELUM)"
                                }


                                destinationAdapter.notifyDataSetChanged()
                                setLoading(false)
                            } catch (e: JSONException) {
                                Log.i("ERROR1", e.message.toString())
                                e.printStackTrace()
                                Helper.toastMessage(this@DestinationFormActivity, e.message.toString())
                                finish()
                            } catch (e: IOException) {
                                Log.i("ERROR2", e.message.toString())
                                e.printStackTrace()
                                Helper.toastMessage(this@DestinationFormActivity, e.message.toString())
                                finish()
                            }
                        } else {
                            try {
                                val err = JSONObject(response.errorBody()!!.string())
                                val message = err.getString("message")
                                Helper.alertDialog(this@DestinationFormActivity,
                                        message
                                )

                            } catch (e: JSONException) {
                                Log.i("ERROR3", e.message.toString())
                                e.printStackTrace()
                                Helper.toastMessage(this@DestinationFormActivity, e.message.toString())
                                finish()
                            } catch (e: IOException) {
                                Log.i("ERROR4", e.message.toString())
                                e.printStackTrace()
                                Helper.toastMessage(this@DestinationFormActivity, e.message.toString())
                                finish()
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, e: Throwable) {
                        Log.i("ERROR5", e.message.toString())
                        Helper.toastMessage(this@DestinationFormActivity, e.message.toString())
                        finish()
                    }
                })

                btnSubmit.setOnClickListener{
                    insertGuest()
                }
            }
        }
    }

    private fun setLoading(state: Boolean){
        with(binding){
            progressBar.visibility = if(state){View.VISIBLE}else{View.GONE}
            scrollViewWrapper.visibility = if(state){View.GONE}else{View.VISIBLE}
        }
    }


    private fun insertGuest(){
        with(binding){
            if(etNoTelp.text.toString().trim().isEmpty() || nik.isEmpty() || selectedDetailId == null){
                Helper.alertDialog(this@DestinationFormActivity, "Mohon mengisi nomor telepon dan memilih Tujuan sebelum melanjutkan")
            }else{
                val loadingDialog = Helper.loadingDialog(this@DestinationFormActivity)
                loadingDialog.show()
                val noTelp = etNoTelp.text.toString().trim()

                RetrofitClient.instance.insertGuest(nik, noTelp, selectedDetailId!!).enqueue(object:
                        Callback<ResponseBody> {
                    override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            try {
                                val result = JSONObject(response.body()!!.string())
                                Helper.toastMessage(this@DestinationFormActivity, "Data Berhasil Dimasukkan!")
                                finish()

                            } catch (e: JSONException) {
                                Log.i("ERROR1", e.message.toString())
                                e.printStackTrace()
                                Helper.toastMessage(this@DestinationFormActivity , e.message.toString())
                                loadingDialog.dismiss()
                            } catch (e: IOException) {
                                Log.i("ERROR2", e.message.toString())
                                e.printStackTrace()
                                Helper.toastMessage(this@DestinationFormActivity , e.message.toString())
                                loadingDialog.dismiss()
                            }
                        } else {
                            try {
                                val err = JSONObject(response.errorBody()!!.string())
                                val message = err.getString("message")
                                Helper.alertDialog(
                                        this@DestinationFormActivity,
                                        message
                                )
                                loadingDialog.dismiss()

                            } catch (e: JSONException) {
                                Log.i("ERROR3", e.message.toString())
                                e.printStackTrace()
                                Helper.toastMessage(this@DestinationFormActivity , e.message.toString())
                                loadingDialog.dismiss()
                            } catch (e: IOException) {
                                Log.i("ERROR4", e.message.toString())
                                e.printStackTrace()
                                Helper.toastMessage(this@DestinationFormActivity , e.message.toString())
                                loadingDialog.dismiss()
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, e: Throwable) {
                        Log.i("ERROR5", e.message.toString())
                        Helper.toastMessage(this@DestinationFormActivity , e.message.toString())
                        loadingDialog.dismiss()
                    }
                })
            }
        }
    }
}