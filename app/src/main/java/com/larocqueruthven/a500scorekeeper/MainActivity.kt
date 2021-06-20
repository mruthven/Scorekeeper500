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
import java.time.Instant
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    class Player(name: String, score: Int) {
        var playerName = name
        var playerScore = score

        fun clear() {
            playerName = "name"
            playerScore = 0
        }
    }

    var players = arrayOf(Player("name", 0), Player("name", 0), Player("name", 0))
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
        updatePlayerNameData()
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
        players[2].clear()
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
        updatePlayerNameData()
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
        players[playerspinner.selectedItemPosition].playerScore += tableScore
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

    fun resetClicked(view: View) {
        for (player in players) {
            player.playerScore = 0
        }
        updateScores()
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

    private fun updatePlayerNameData() {
        val spinner = findViewById<Spinner>(R.id.playersspinner)
        val player1 = findViewById<TextView>(R.id.p1name).text.toString()
        val player2 = findViewById<TextView>(R.id.p2name).text.toString()
        val player3 = findViewById<TextView>(R.id.p3name).text.toString()
        players[0].playerName = player1
        players[1].playerName = player2
        if (numPlayers == 4) {
            players[2].playerName = player3
        }
        val playernames: Array<String>
        if (numPlayers == 3) {
            playernames = arrayOf(player1, player2, player3)
        } else {
            playernames = arrayOf(player1, player2)
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
        findViewById<TextView>(R.id.p1Score).text = players[0].playerScore.toString()
        findViewById<TextView>(R.id.p2score).text = players[1].playerScore.toString()
        findViewById<TextView>(R.id.p3score).text = players[2].playerScore.toString()
    }

    private fun sendScores() {
        val queue = Volley.newRequestQueue(this)
        val url = "https://yt2wj64lsk.execute-api.us-east-2.amazonaws.com/default/updateScoreHistory"
        val tournamentID = (System.currentTimeMillis() / 120000).toString() //tournamentID changes every 2 minutes...just for testing
        val gameID = (System.currentTimeMillis() % 30000).toString() //gameIDs can repeat every 30 seconds...just for testing
        var body = constructBody(players[0], tournamentID, gameID)
        var request = createRequest(Request.Method.PUT, body, url)
        queue.add(request)
        body = constructBody(players[1], tournamentID, gameID)
        request = createRequest(Request.Method.PUT, body, url)
        queue.add(request)
    }

    private fun createRequest(method: Int, body: JSONObject?, url: String): JsonObjectRequest {
        val request = object : JsonObjectRequest(method, url, body,
                Response.Listener { response ->
                    println(response.toString())
                },
                Response.ErrorListener { error ->
                    val thing = String(error.networkResponse.data, Charset.forName("UTF-8"))
                    println(thing)
                }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["x-api-key"] = getString(R.string.API_KEY)
                return headers
            }
        }
        return request
    }

    private fun constructBody(player: MainActivity.Player, tournamentID: String, gameID: String): JSONObject {
        val body = JSONObject()
        val pair1Player = player.playerName.substringBefore("/")
        val pair1Partner = player.playerName.substringAfter("/")
        body.put("tournamentIDgameID", tournamentID + "||" + gameID)
        body.put("playerName", pair1Player)
        body.put("partnerName", pair1Partner)
        body.put("gameScore", player.playerScore.toString())
        return body
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
                headers["x-api-key"] = getString(R.string.API_KEY)
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