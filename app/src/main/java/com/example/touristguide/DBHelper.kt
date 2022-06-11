package com.example.przewodnikpotoruniu;

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.touristguide.Category
import com.example.touristguide.Spot
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException


class DBHelper(context: Context):SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VER) {
    companion object {
        private val DATABASE_NAME = "tourist.db"
        private val DATABASE_VER = 4
        private val SPOT_TABLE_NAME = "spots"
        private val SPOT_ID = "id"
        private val SPOT_NAME = "name"
        private val SPOT_LATITUDE = "latitude"
        private val SPOT_LONGITUDE = "longitude"
        private val SPOT_AVGTIME = "avgtime"
        private val SPOT_CATEGORY = "category"
        private val SPOT_URL = "url"
        private val CATEGORY_TABLE_NAME = "categories"
        private val CATEGORY_ID = "id"
        private val CATEGORY_NAME = "category"
        private val CATEGORY_POLISH = "polish"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        print(DATABASE_NAME)
        var CREATE_TABLE_QUERY = ("CREATE TABLE $SPOT_TABLE_NAME ($SPOT_ID INTEGER PRIMARY KEY, $SPOT_NAME TEXT, $SPOT_LATITUDE TEXT, $SPOT_LONGITUDE TEXT, $SPOT_AVGTIME TEXT, $SPOT_CATEGORY TEXT, $SPOT_URL TEXT)")
        db!!.execSQL(CREATE_TABLE_QUERY)
        CREATE_TABLE_QUERY = ("CREATE TABLE $CATEGORY_TABLE_NAME ($CATEGORY_ID INTEGER PRIMARY KEY, $CATEGORY_NAME TEXT, $CATEGORY_POLISH TEXT)")
        db.execSQL(CREATE_TABLE_QUERY)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $SPOT_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $CATEGORY_TABLE_NAME")
    }

    private fun addSpot(id: Int, name: String, latitude: Double, longitude: Double, avgtime: Double, category: String, url: String){
        val db = this.writableDatabase
        try {
            val values = ContentValues()
            values.put(SPOT_ID, id)
            values.put(SPOT_NAME, name)
            values.put(SPOT_LATITUDE, latitude.toString())
            values.put(SPOT_LONGITUDE, longitude.toString())
            values.put(SPOT_AVGTIME, avgtime.toString())
            values.put(SPOT_CATEGORY, category)
            values.put(SPOT_URL, url)

            db.insertOrThrow(SPOT_TABLE_NAME, null, values)
        } catch (e : SQLiteConstraintException){

        }
    }

    private fun addCategory(id: Int, name: String, polish: String){
        val db = this.writableDatabase
        try {
            val values = ContentValues()
            values.put(CATEGORY_ID, id)
            values.put(CATEGORY_NAME, name)
            values.put(CATEGORY_POLISH, polish)
            db.insertOrThrow(CATEGORY_TABLE_NAME, null, values)
        } catch (e : SQLiteConstraintException){

        }
    }

    fun getSpotsFromJSONFile(context: Context){
        val json: String
        try {
            val iS = context.assets.open("spots.json")
            val size = iS.available()
            val buffer = ByteArray(size)
            iS.read(buffer)
            iS.close()

            json = String(buffer)
            val jsonArray = JSONArray(json)
            for(i in 0 until jsonArray.length()){
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getInt("id")
                val name = obj.getString("name")
                val latitude = obj.getDouble("latitude")
                val longitude = obj.getDouble("longitude")
                val avgtime = obj.getDouble("avgtime")
                val category = obj.getString("category")
                val url = obj.getString("url")
                addSpot(id, name, latitude, longitude, avgtime, category, url)
            }

        } catch (e: IOException){
            e.printStackTrace()
        } catch (e: JSONException){
            e.printStackTrace()
        }
    }

    fun getCategoriesFromJSONFile(context: Context){
        val json: String
        try {
            val iS = context.assets.open("categories.json")
            val size = iS.available()
            val buffer = ByteArray(size)
            iS.read(buffer)
            iS.close()

            json = String(buffer)
            val jsonArray = JSONArray(json)
            for(i in 0 until jsonArray.length()){
                val obj = jsonArray.getJSONObject(i)
                val id = obj.getInt("id")
                val name = obj.getString("category")
                val polish = obj.getString("polish")
                addCategory(id, name, polish)
            }

        } catch (e: IOException){
            e.printStackTrace()
        } catch (e: JSONException){
            e.printStackTrace()
        }
    }

    val spots:ArrayList<Spot>
        @SuppressLint("Range", "Recycle")
        get(){
            val spots = ArrayList<Spot>()
            val selectQuery = "SELECT $SPOT_NAME FROM $SPOT_TABLE_NAME ORDER BY $SPOT_ID"
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if(cursor.moveToFirst()){
                do {
                    val obj = Spot()
                    obj.name = cursor.getString(cursor.getColumnIndex(SPOT_NAME))
                    spots.add(obj)
                } while (cursor.moveToNext())
            }
            db.close()
            return spots
        }

    @SuppressLint("Range", "Recycle")
    fun getSpotNamesByCategory(category : String): ArrayList<String>{
        val spotNames = ArrayList<String>()
        val selectQuery: String = if (category == "all"){
            "SELECT $SPOT_NAME FROM $SPOT_TABLE_NAME ORDER BY $SPOT_ID"
        } else {
            "SELECT $SPOT_NAME FROM $SPOT_TABLE_NAME WHERE $SPOT_CATEGORY = '$category' ORDER BY $SPOT_ID"
        }
        val db = this.writableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        if(cursor.moveToFirst()){
            do {
                spotNames.add(cursor.getString(cursor.getColumnIndex(SPOT_NAME)))
            } while (cursor.moveToNext())
        }
        db.close()
        return spotNames
    }

    val spotNames:ArrayList<String>
        @SuppressLint("Range", "Recycle")
        get(){
            val spotNames = ArrayList<String>()
            val selectQuery = "SELECT $SPOT_NAME FROM $SPOT_TABLE_NAME ORDER BY $SPOT_ID"
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if(cursor.moveToFirst()){
                do {
                    spotNames.add(cursor.getString(cursor.getColumnIndex(SPOT_NAME)))
                } while (cursor.moveToNext())
            }
            db.close()
            return spotNames
        }

    @SuppressLint("Range", "Recycle")
    fun getSpotByName(name : String): Spot? {
        val selectQuery = "SELECT * FROM $SPOT_TABLE_NAME WHERE $SPOT_NAME = '$name'"
        val db = this.writableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        return if(cursor.moveToFirst()){
            val obj = Spot()
            obj.id = cursor.getInt(cursor.getColumnIndex(SPOT_ID))
            obj.name = cursor.getString(cursor.getColumnIndex(SPOT_NAME))
            obj.latitude = cursor.getString(cursor.getColumnIndex(SPOT_LATITUDE)).toDouble()
            obj.longitude = cursor.getString(cursor.getColumnIndex(SPOT_LONGITUDE)).toDouble()
            obj.url = cursor.getString(cursor.getColumnIndex(SPOT_URL))
            obj.avgtime = cursor.getDouble(cursor.getColumnIndex(SPOT_AVGTIME))
            obj.category = cursor.getString(cursor.getColumnIndex(SPOT_CATEGORY))
            obj
        } else null
    }

    val categoriesPolishNames:ArrayList<String>
        @SuppressLint("Range", "Recycle")
        get(){
            val categoriesPolishNames = ArrayList<String>()
            val selectQuery = "SELECT $CATEGORY_POLISH FROM $CATEGORY_TABLE_NAME ORDER BY $CATEGORY_ID"
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if(cursor.moveToFirst()){
                do {
                    categoriesPolishNames.add(cursor.getString(cursor.getColumnIndex(CATEGORY_POLISH)))
                } while (cursor.moveToNext())
            }
            db.close()
            return categoriesPolishNames
        }

    @SuppressLint("Range", "Recycle")
    fun getCategoryByPolishName(name : String): Category? {
        val selectQuery = "SELECT * FROM $CATEGORY_TABLE_NAME WHERE $CATEGORY_POLISH = '$name'"
        val db = this.writableDatabase
        val cursor = db.rawQuery(selectQuery, null)
        return if(cursor.moveToFirst()){
            val obj = Category()
            obj.id = cursor.getInt(cursor.getColumnIndex(CATEGORY_ID))
            obj.category = cursor.getString(cursor.getColumnIndex(CATEGORY_NAME))
            obj.polish = cursor.getString(cursor.getColumnIndex(CATEGORY_POLISH))
            obj
        } else null
    }

}