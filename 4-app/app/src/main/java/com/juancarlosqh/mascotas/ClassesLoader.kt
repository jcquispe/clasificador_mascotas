package com.juancarlosqh.mascotas

import android.content.Context
import org.json.JSONArray

object ClassesLoader {

    private var classes: List<String> = emptyList()

    fun load(context: Context) {
        val json = context.assets.open("classes.json")
            .bufferedReader()
            .use { it.readText() }

        val jsonArray = JSONArray(json)

        classes = List(jsonArray.length()) { i ->
            jsonArray.getString(i)
        }
    }

    fun get(): List<String> = classes
}