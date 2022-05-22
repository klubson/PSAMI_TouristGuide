package com.example.touristguide

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.przewodnikpotoruniu.DBHelper

class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        thread.start()
    }
    lateinit var db: DBHelper

    val thread = Thread(){
        run {
            //pobieranie bazy danych
            db = DBHelper(this)
            db.getJSONFile(this)
        }
        runOnUiThread(){
            // uruchomienie mapy po zakończeniu pobierania danych
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            this.finish()
        }
    }
}