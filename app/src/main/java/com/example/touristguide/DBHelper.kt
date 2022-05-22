package com.example.przewodnikpotoruniu;

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context;
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException


class DBHelper(context: Context):SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VER) {
    companion object{
        private val DATABASE_NAME = "tourist.db"
        private val DATABASE_VER = 1
        private val OBJECT_TABLE_NAME = "objects"
        private val OBJECT_ID = "id"
        private val OBJECT_NAME = "name"
        private val OBJECT_LATITUDE = "latitude"
        private val OBJECT_LONGITUDE = "longitude"
        private val OBJECT_AVGTIME = "avgtime"
        private val OBJECT_CATEGORY = "category"
        private val OBJECT_URL = "url"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLE_QUERY = ("CREATE TABLE $OBJECT_TABLE_NAME ($OBJECT_ID INTEGER PRIMARY KEY, $OBJECT_NAME TEXT, $OBJECT_LATITUDE TEXT, $OBJECT_LONGITUDE TEXT, $OBJECT_AVGTIME TEXT, $OBJECT_CATEGORY TEXT, $OBJECT_URL TEXT)")
        db!!.execSQL(CREATE_TABLE_QUERY)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $OBJECT_TABLE_NAME")
    }

    private fun addObject(id: Int, name: String, latitude: Double, longitude: Double, avgtime: Double, category: String, url: String){
        val db = this.writableDatabase
        try {
            val values = ContentValues()
            values.put(OBJECT_ID, id)
            values.put(OBJECT_NAME, name)
            values.put(OBJECT_LATITUDE, latitude.toString())
            values.put(OBJECT_LONGITUDE, longitude.toString())
            values.put(OBJECT_AVGTIME, avgtime.toString())
            values.put(OBJECT_CATEGORY, category)
            values.put(OBJECT_URL, url)

            db.insertOrThrow(OBJECT_TABLE_NAME, null, values)
        } catch (e : SQLiteConstraintException){

        }
    }

    fun getJSONFile(context: Context){
        val json: String
        try {
            val iS = context.assets.open("objects.json")
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
                addObject(id, name, latitude, longitude, avgtime, category, url)
            }

        } catch (e: IOException){
            e.printStackTrace()
        } catch (e: JSONException){
            e.printStackTrace()
        }
    }

    val objects:ArrayList<Object>
        @SuppressLint("Range", "Recycle")
        get(){
            val objects = ArrayList<Object>()
            val selectQuery = "SELECT $OBJECT_NAME FROM $OBJECT_TABLE_NAME ORDER BY $OBJECT_ID"
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if(cursor.moveToFirst()){
                do {
                    val obj = Object()
                    obj.name = cursor.getString(cursor.getColumnIndex(OBJECT_NAME))
                    objects.add(obj)
                } while (cursor.moveToNext())
            }
            db.close()
            return objects
        }

    val objectNames:ArrayList<String>
        @SuppressLint("Range", "Recycle")
        get(){
            val objectNames = ArrayList<String>()
            val selectQuery = "SELECT $OBJECT_NAME FROM $OBJECT_TABLE_NAME ORDER BY $OBJECT_ID"
            val db = this.writableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            if(cursor.moveToFirst()){
                do {
                    objectNames.add(cursor.getString(cursor.getColumnIndex(OBJECT_NAME)))
                } while (cursor.moveToNext())
            }
            db.close()
            return objectNames
        }

    @SuppressLint("Recycle", "Range")
    fun getObjectsByName(name: String): ArrayList<Object> {
        val objects = ArrayList<Object>()
        val splittedName = name.split(" ")
        val db = this.writableDatabase
        for (word : String in splittedName){
            val selectQuery = "SELECT * FROM $OBJECT_TABLE_NAME WHERE $OBJECT_NAME LIKE '%$name%'"
            val cursor = db.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()){
                do {
                    val obj = Object()
                    obj.id = cursor.getInt(cursor.getColumnIndex(OBJECT_ID))
                    obj.name = cursor.getString(cursor.getColumnIndex(OBJECT_NAME))
                    obj.latitude = cursor.getString(cursor.getColumnIndex(OBJECT_LATITUDE)).toDouble()
                    obj.longitude = cursor.getString(cursor.getColumnIndex(OBJECT_LONGITUDE)).toDouble()
                    obj.avgtime = cursor.getString(cursor.getColumnIndex(OBJECT_AVGTIME)).toDouble()
                    obj.category = cursor.getString(cursor.getColumnIndex(OBJECT_CATEGORY))
                    obj.url = cursor.getString(cursor.getColumnIndex(OBJECT_URL))
                    if (!obj.isInList(objects)){
                        objects.add(obj)
                    }

                } while (cursor.moveToNext())
            }

        }
        return objects
    }
//    fun getObject(id: Int): Object? {
//        val selectQuery = "SELECT * FROM $OBJECT_TABLE_NAME WHERE $OBJECT_ID = $id"
//        val db = this.writableDatabase
//        val cursor = db.rawQuery(selectQuery, null)
//        return if(cursor.moveToFirst()){
//            val obj = Object()
//            obj.id = cursor.getInt(cursor.getColumnIndex(OBJECT_ID))
//            obj.name = cursor.getString(cursor.getColumnIndex(OBJECT_NAME))
//            obj.latitude = cursor.getString(cursor.getColumnIndex(OBJECT_LATITUDE)).toDouble()
//            obj.longitude = cursor.getString(cursor.getColumnIndex(OBJECT_LONGITUDE)).toDouble()
//            obj.url = cursor.getString(cursor.getColumnIndex(OBJECT_URL))
//            obj
//        } else null
//    }
}