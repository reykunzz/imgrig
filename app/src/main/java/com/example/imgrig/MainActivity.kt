package com.example.imgrig

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*

class MainActivity : Activity() {
    private var imgUri: Uri? = null
    private var entries = mutableListOf<Entry>()
    private var sel = -1
    private lateinit var list: ListView
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        status = TextView(this).apply { text = "Buka file player.img" }
        val bar = LinearLayout(this)
        fun btn(t: String, f: () -> Unit) {
            val bt = Button(this).apply { text = t; setOnClickListener { f() } }
            bar.addView(bt, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        btn("Buka") { pick(1) }
        btn("Simpan") { saveSame() }
        btn("Simpan sbg") { create(4, "player.img") }
        list = ListView(this)
        list.setOnItemClickListener { _, _, i, _ -> sel = i; menu() }
        root.addView(bar); root.addView(status); root.addView(list)
        setContentView(root)
    }

    private fun pick(code: Int) {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), code)
    }

    private fun create(code: Int, name: String) {
        if (entries.isEmpty()) { toast("Buka IMG dulu"); return }
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE, name), code)
    }

    private fun menu() {
        val e = entries[sel]
        AlertDialog.Builder(this).setTitle(e.name)
            .setItems(arrayOf("Ganti (isi dari file, nama tetap)", "Ekstrak", "Hapus")) { _, w ->
                when (w) {
                    0 -> pick(2)
                    1 -> create(3, e.name)
                    2 -> { entries.removeAt(sel); refresh() }
                }
            }.show()
    }

    private fun refresh() {
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,
            entries.map { "${it.name}  (${it.data.size / 1024} KB)" })
        status.text = "${entries.size} entri — jangan lupa Simpan"
    }

    private fun saveSame() {
        val u = imgUri ?: return toast("Buka IMG dulu")
        try {
            contentResolver.openOutputStream(u, "wt")!!.use { it.write(Img.write(entries)) }
            toast("Tersimpan")
        } catch (e: Exception) { toast("Gagal: ${e.message} — pakai Simpan sbg") }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, d: Intent?) {
        super.onActivityResult(req, res, d)
        val u = d?.data
        if (res != RESULT_OK || u == null) return
        try {
            when (req) {
                1 -> { imgUri = u; entries = Img.read(contentResolver.openInputStream(u)!!.use { it.readBytes() }); refresh() }
                2 -> { entries[sel].data = Img.pad(contentResolver.openInputStream(u)!!.use { it.readBytes() }); refresh() }
                3 -> contentResolver.openOutputStream(u)!!.use { it.write(Img.trim(entries[sel].name, entries[sel].data)) }
                4 -> contentResolver.openOutputStream(u, "wt")!!.use { it.write(Img.write(entries)) }
            }
            if (req == 3 || req == 4) toast("Selesai")
        } catch (e: Exception) { toast("Error: ${e.message}") }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
