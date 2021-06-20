package com.larocqueruthven.a500scorekeeper

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.nio.charset.Charset
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    var score = intArrayOf(0, 0, 0)
    var currentEdit: TextView? = null
    var numPlayers = 3

    val SCORE_TABLE = Array(6) { IntArray(10) }
    val TEN_SCORE = 250
    val SPADE_BASE = 40
    val CLUB_BASE = 60
    val DIAMOND_BASE = 80
    val HEART_BASE = 100
    val NOTRUMP_BASE = 120
    val CONTRACT_ESCALATOR = 100
    val NO_CONTRACT_TRICK_VALUE = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupSpinner(R.id.suitspinner, R.array.suits)
        setupSpinner(R.id.tricksspinner, R.array.tricks)
        setupSpinner(R.id.wonorlost, R.array.wonorlost)
        populatePlayerSpinner()
        populateScoreTable()
    }

    fun threepress(view: View) {
        var toShow = findViewById<TextView>(R.id.p3name)
        toShow.visibility = View.VISIBLE
        toShow = findViewById<TextView>(R.id.p3score)
        toShow.visibility = View.VISIBLE
        numPlayers = 3
    }

    fun fourpress(view: View) {
        var toShow = findViewById<TextView>(R.id.p3name)
        toShow.visibility = View.GONE
        toShow = findViewById<TextView>(R.id.p3score)
        toShow.visibility = View.GONE
        numPlayers = 4
    }

    fun textclicked(view: View) {
        currentEdit = view as TextView?
        val editbox: EditText = findViewById(R.id.editplayername)
        editbox.setText(currentEdit?.text)
        editbox.visibility = View.VISIBLE
        findViewById<Button>(R.id.savebutton).visibility = View.VISIBLE
    }

    fun textsave(view: View) {
        val editbox: EditText = findViewById(R.id.editplayername)
        currentEdit?.setText(editbox.text)
        currentEdit = null
        editbox.visibility = View.INVISIBLE
        view.visibility = View.INVISIBLE
        hideKeyboard(view)
        populatePlayerSpinner()
    }

    fun okClicked(view: View) {
        val tricksspinner = findViewById<Spinner>(R.id.tricksspinner)
        val suitspinner = findViewById<Spinner>(R.id.suitspinner)
        val playerspinner = findViewById<Spinner>(R.id.playersspinner)
        val wonspinner = findViewById<Spinner>(R.id.wonorlost)

        var tableScore = SCORE_TABLE[suitspinner.selectedItemPosition][tricksspinner.selectedItemPosition]
        if (wonspinner.selectedItemPosition == 1) {
            tableScore *= -1
        } else if (wonspinner.selectedItemPosition == 2) {
            tableScore = max(tableScore, TEN_SCORE)
        }
        score[playerspinner.selectedItemPosition] += tableScore
        updateScores()
        return
        //Find selected tricks, suit, player, won/lost
        //Determine points to add/subtract
        //Update player score
        //Reset spinners
    }

    fun uploadClicked(view: View) {
        sendScores()
    }

    private fun setupSpinner(spinnerId: Int, stringarray: Int) {
        val spinner: Spinner = findViewById(spinnerId)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
                this,
                stringarray,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }
    }

    private fun populatePlayerSpinner() {
        val spinner = findViewById<Spinner>(R.id.playersspinner)
        val player1 = findViewById<TextView>(R.id.p1name)
        val player2 = findViewById<TextView>(R.id.p2name)
        val player3 = findViewById<TextView>(R.id.p3name)
        val playernames: Array<String>
        if (numPlayers == 3) {
            playernames = arrayOf(player1.text.toString(), player2.text.toString(), player3.text.toString())
        } else {
            playernames = arrayOf(player1.text.toString(), player2.text.toString())
        }
        val adapter = ArrayAdapter(this,
                android.R.layout.simple_spinner_item, playernames)
        spinner.adapter = adapter
    }

    private fun populateScoreTable() {
        // BUILD TABLE
        //          |  6  |  7  |  8  |  9  |  10
        // SPADES   |  40 | 140 | 240 | 340 | 440
        // CLUBS    |  60 | 160 | 260 | 360 | 460
        // DIAMONDS |  80 | 180 | 280 | 380 | 480
        // HEARTS   | 100 | 200 | 300 | 400 | 500
        // NO TRUMP | 120 | 220 | 320 | 420 | 500
        fillContractRow(SPADE_BASE, SCORE_TABLE[0])
        fillContractRow(CLUB_BASE, SCORE_TABLE[1])
        fillContractRow(DIAMOND_BASE, SCORE_TABLE[2])
        fillContractRow(HEART_BASE, SCORE_TABLE[3])
        fillContractRow(NOTRUMP_BASE, SCORE_TABLE[4])
        // fill a row to make it easy to map the spinner locations to score values
        fillNonContractRow(SCORE_TABLE[5])
    }

    private fun fillContractRow(startNumber: Int, row: IntArray) {
        for (i in 0..4) {
            row[i] = CONTRACT_ESCALATOR * i + startNumber
        }
        // these are invalid values, but to build a single lookup
        // table powered by the spinner indexes, we need 10 item arrays
        // zero them out because these shouldn't impact the scores
        for (i in 5..9) {
            row[i] = 0
        }
    }

    private fun fillNonContractRow(row: IntArray) {
        // the spinners are built to make contract values easy to lookup, which means they start at
        // 6 and loop back to 1 in position 5, so we build it a bit weird here
        // there is probably a better way to do this
        for (i in 0..4) {
            row[i] = 60 + (i * NO_CONTRACT_TRICK_VALUE)
        }
        for (i in 5..9) {
            row[i] = 10 + ((i - 5) * NO_CONTRACT_TRICK_VALUE)
        }
    }

    private fun updateScores() {
        findViewById<TextView>(R.id.p1Score).text = score[0].toString()
        findViewById<TextView>(R.id.p2score).text = score[1].toString()
        findViewById<TextView>(R.id.p3score).text = score[2].toString()
    }

    private fun sendScores() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://yt2wj64lsk.execute-api.us-east-2.amazonaws.com/default/updateScoreHistory"
        val body = JSONObject()
        body.put("tournamentID", "12345")
        body.put("gameID", "666")
        body.put("playerName", "Mike")
        body.put("partnerName", "Doug")
        val request = object : JsonObjectRequest(Method.PUT, url, body,
                Response.Listener { response ->
                    println(response.toString())
                },
                Response.ErrorListener { error ->
                    val thing = String(error.networkResponse.data, Charset.forName("UTF-8"))
                    println(thing)
                }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["x-api-key"] = ""
                return headers
            }
        }
        queue.add(request)
    }

    private fun getScores() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://yt2wj64lsk.execute-api.us-east-2.amazonaws.com/default/updateScoreHistory?tournamentID=12345"
        val request = object : JsonObjectRequest(Method.GET, url, null,
                Response.Listener { response ->
                    println(response.toString())
                },
                Response.ErrorListener { error ->
                    val thing = String(error.networkResponse.data, Charset.forName("UTF-8"))
                    println(thing)
                }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["x-api-key"] = ""
                return headers
            }
        }
        queue.add(request)
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}