package com.example.touristguide

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        thread.start()
    }
//    lateinit var db: DBHelper

    val thread = Thread(){
        run {
            //pobieranie bazy danych
            Thread.sleep(3000);
        }
        runOnUiThread(){
            // uruchomienie mapy po zakończeniu pobierania danych
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            this.finish()
        }
    }
}