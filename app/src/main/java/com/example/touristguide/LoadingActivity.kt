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
    lateinit var db: DBHelper

    private val thread = Thread {
        run {
            //pobieranie bazy danych
            db = DBHelper(this)
            db.getSpotsFromJSONFile(this)
            db.getCategoriesFromJSONFile(this)
        }
        runOnUiThread {
            // uruchomienie mapy po zakończeniu pobierania danych
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            this.finish()
        }
    }
}