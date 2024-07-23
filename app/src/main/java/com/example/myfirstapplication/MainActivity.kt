package com.example.myfirstapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myfirstapplication.Screens.Photos.PhotoSelectActivity


import org.json.JSONArray

import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var userAdapter: UserAdapter
    private lateinit var recyclerView: RecyclerView




    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.rvTodoItems)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.inflateMenu(R.menu.main_menu)







        fetchUsers()
    }

    private fun fetchUsers() {
        thread {
            val url = URL("https://jsonplaceholder.typicode.com/users")
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    val response = bufferedReader.use { it.readText() }

                    val users = parseJson(response)
                    runOnUiThread {
                        userAdapter = UserAdapter(users)
                        recyclerView.adapter = userAdapter
                    }
                } else {
                    Log.e("MainActivity", "Failed to fetch data, response code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching data", e)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseJson(response: String): List<UserModel> {
        val users = mutableListOf<UserModel>()
        val jsonArray = JSONArray(response)

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val addressObject = jsonObject.getJSONObject("address")
            val companyObject = jsonObject.getJSONObject("company")

            val user = UserModel(
                id = jsonObject.getInt("id"),
                name = jsonObject.getString("name"),
                username = jsonObject.getString("username"),
                email = jsonObject.getString("email"),
                address = Address(
                    street = addressObject.getString("street"),
                    suite = addressObject.getString("suite"),
                    city = addressObject.getString("city"),
                    zipcode = addressObject.getString("zipcode")
                ),
                company = Company(
                    name = companyObject.getString("name")
                )
            )
            users.add(user)
        }
        return users
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_select_photo -> {
                val intent = Intent(this, PhotoSelectActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



}
