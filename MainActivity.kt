package com.sonora.player

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

data class Track(val id: Long, val title: String, val artist: String, val uri: Uri)

class MainActivity : ComponentActivity() {
    private val requestCode = 410
    private val tracks = mutableListOf<Track>()
    private var player: MediaPlayer? = null
    private var currentIndex = -1
    private lateinit var listContainer: LinearLayout
    private lateinit var status: TextView
    private lateinit var nowPlaying: TextView
    private lateinit var playButton: Button

    private val bg = Color.rgb(15, 12, 29)
    private val panel = Color.rgb(35, 27, 58)
    private val accent = Color.rgb(76, 220, 204)
    private val white = Color.WHITE
    private val muted = Color.rgb(190, 183, 210)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        if (hasAudioPermission()) loadLibrary() else requestAudioPermission()
    }

    private fun buildScreen() {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 28, 20, 16)
            setBackgroundColor(bg)
        }

        val brand = TextView(this).apply {
            text = "♫  SONORA"
            textSize = 27f
            setTextColor(accent)
            setPadding(0, 0, 0, 8)
        }
        outer.addView(brand)

        val heading = TextView(this).apply {
            text = "Your world. Your soundtrack."
            textSize = 22f
            setTextColor(white)
        }
        outer.addView(heading)

        status = TextView(this).apply {
            text = "Checking music access…"
            textSize = 14f
            setTextColor(muted)
            setPadding(0, 10, 0, 14)
        }
        outer.addView(status)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 12)
        }
        val previous = makeButton("⏮") { playRelative(-1) }
        playButton = makeButton("▶") { togglePlayback() }
        val next = makeButton("⏭") { playRelative(1) }
        controls.addView(previous, weighted())
        controls.addView(playButton, weighted())
        controls.addView(next, weighted())
        outer.addView(controls)

        nowPlaying = TextView(this).apply {
            text = "Nothing playing"
            textSize = 15f
            setTextColor(accent)
            setPadding(0, 6, 0, 16)
        }
        outer.addView(nowPlaying)

        val listTitle = TextView(this).apply {
            text = "YOUR MUSIC"
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 4, 0, 10)
        }
        outer.addView(listTitle)

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listContainer)
        outer.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val rescan = makeButton("↻  Rescan music library") { loadLibrary() }
        outer.addView(rescan, LinearLayout.LayoutParams(-1, -2))
        setContentView(outer)
    }

    private fun weighted() = LinearLayout.LayoutParams(0, 52, 1f).apply {
        setMargins(4, 0, 4, 0)
    }

    private fun makeButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(white)
        setBackgroundColor(panel)
        setOnClickListener { action() }
    }

    private fun permissionName(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAudioPermission(): Boolean =
        checkSelfPermission(permissionName()) == PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        requestPermissions(arrayOf(permissionName()), requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == this.requestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadLibrary()
        } else {
            status.text = "Music access is off. Allow access to scan your songs."
            Toast.makeText(this, "Allow music access to use your library", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadLibrary() {
        if (!hasAudioPermission()) {
            requestAudioPermission()
            return
        }

        tracks.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown title"
                    val artist = cursor.getString(artistCol) ?: "Unknown artist"
                    tracks.add(
                        Track(
                            id,
                            title,
                            artist,
                            ContentUris.withAppendedId(collection, id)
                        )
                    )
                }
            }
            renderTracks()
            status.text = "${tracks.size} songs found on this device"
        } catch (e: SecurityException) {
            status.text = "Music permission is required to scan your library."
        }
    }

    private fun renderTracks() {
        listContainer.removeAllViews()
        if (tracks.isEmpty()) {
            val empty = TextView(this).apply {
                text = "No music was found. Add audio files to your phone, then tap Rescan."
                textSize = 15f
                setTextColor(muted)
                setPadding(12, 18, 12, 18)
            }
            listContainer.addView(empty)
            return
        }
        tracks.forEachIndexed { index, track ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 12, 14, 12)
                setBackgroundColor(panel)
                val params = LinearLayout.LayoutParams(-1, -2)
                params.setMargins(0, 0, 0, 8)
                layoutParams = params
                isClickable = true
                isFocusable = true
                setOnClickListener { playAt(index) }
            }
            row.addView(TextView(this).apply {
                text = track.title
                textSize = 16f
                setTextColor(white)
            })
            row.addView(TextView(this).apply {
                text = track.artist
                textSize = 12f
                setTextColor(muted)
            })
            listContainer.addView(row)
        }
    }

    private fun playAt(index: Int) {
        if (index !in tracks.indices) return
        try {
            player?.release()
            currentIndex = index
            player = MediaPlayer().apply {
                setDataSource(this@MainActivity, tracks[index].uri)
                setOnCompletionListener { playRelative(1) }
                prepare()
                start()
            }
            nowPlaying.text = "Playing: ${tracks[index].title}"
            playButton.text = "Ⅱ"
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't play this audio file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun togglePlayback() {
        val p = player
        if (p == null) {
            if (tracks.isNotEmpty()) playAt(0)
            return
        }
        if (p.isPlaying) {
            p.pause()
            playButton.text = "▶"
        } else {
            p.start()
            playButton.text = "Ⅱ"
        }
    }

    private fun playRelative(delta: Int) {
        if (tracks.isEmpty()) return
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + delta + tracks.size) % tracks.size
        playAt(nextIndex)
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
