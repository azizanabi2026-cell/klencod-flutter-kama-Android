package com.example.trucklog

import android.net.Uri
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("log", MODE_PRIVATE) }
    private var rows = JSONArray()
    private var truck = ""
    private var cont = ""
    private var kind = "truck"
    private lateinit var photoUri: Uri
    private lateinit var table: TableLayout
    private lateinit var tvPending: TextView

    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) recognize() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        table = findViewById(R.id.table)
        tvPending = findViewById(R.id.tvPending)
        rows = JSONArray(prefs.getString("rows", "[]"))
        findViewById<Button>(R.id.btnTruck).setOnClickListener { shoot("truck") }
        findViewById<Button>(R.id.btnCont).setOnClickListener { shoot("cont") }
        findViewById<Button>(R.id.btnExport).setOnClickListener { export() }
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            AlertDialog.Builder(this).setTitle("مسح كل السجلات؟")
                .setPositiveButton("مسح") { _, _ -> rows = JSONArray(); truck = ""; cont = ""; save(); render() }
                .setNegativeButton("إلغاء", null).show()
        }
        render()
    }

    private fun shoot(k: String) {
        kind = k
        val f = File(cacheDir, "photo.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
        takePhoto.launch(photoUri)
    }

    private fun recognize() {
        val k = kind
        val img = InputImage.fromFilePath(this, photoUri)
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(img)
            .addOnSuccessListener { t ->
                val guess = if (k == "cont") findContainer(t.text) else findTruck(t.text)
                confirm(if (k == "cont") "رقم الحاوية" else "رقم الشاحنة", guess) { v ->
                    if (k == "cont") cont = v.uppercase() else truck = v.uppercase()
                    tryAdd()
                }
            }
            .addOnFailureListener { Toast.makeText(this, "تعذّرت قراءة الصورة", Toast.LENGTH_SHORT).show() }
    }

    // حاوية ISO 6346: 4 حروف + 6 أرقام + رقم تحقق
    private fun findContainer(s: String): String =
        Regex("([A-Z]{4})[\\s-]*(\\d{6})[\\s-]*(\\d)").find(s.uppercase())
            ?.groupValues?.drop(1)?.joinToString("") ?: ""

    // الشاحنة: السطر الأكثر أرقامًا (يمكن تصحيحه يدويًا)
    private fun findTruck(s: String): String =
        s.lines().map { it.trim() }.filter { l -> l.count { it.isDigit() } >= 3 }
            .maxByOrNull { l -> l.count { it.isDigit() } * 10 + l.length }?.uppercase() ?: ""

    private fun confirm(title: String, guess: String, onOk: (String) -> Unit) {
        val et = EditText(this).apply { setText(guess); setSingleLine(); hint = "اكتب الرقم إذا لم تُقرأ" }
        AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton("تأكيد") { _, _ ->
                val v = et.text.toString().trim()
                if (v.isNotEmpty()) onOk(v)
            }
            .setNegativeButton("إلغاء", null).show()
    }

    private fun tryAdd() {
        if (truck.isNotEmpty() && cont.isNotEmpty()) {
            val now = Date()
            rows.put(JSONObject().apply {
                put("truck", truck); put("cont", cont)
                put("date", SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(now))
                put("time", SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(now))
            })
            truck = ""; cont = ""; save()
        }
        render()
    }

    private fun save() = prefs.edit().putString("rows", rows.toString()).apply()

    private fun cell(t: String, bold: Boolean = false) = TextView(this).apply {
        text = t; setPadding(12, 14, 12, 14); gravity = Gravity.CENTER
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun render() {
        tvPending.text = "الشاحنة: ${truck.ifEmpty { "—" }}    الحاوية: ${cont.ifEmpty { "—" }}"
        table.removeAllViews()
        table.addView(TableRow(this).apply {
            listOf("الشاحنة", "الحاوية", "التاريخ", "الوقت").forEach { addView(cell(it, true)) }
        })
        for (i in rows.length() - 1 downTo 0) {
            val o = rows.getJSONObject(i)
            table.addView(TableRow(this).apply {
                listOf("truck", "cont", "date", "time").forEach { addView(cell(o.getString(it))) }
                setOnLongClickListener {
                    AlertDialog.Builder(this@MainActivity).setTitle("حذف هذا السطر؟")
                        .setPositiveButton("حذف") { _, _ -> rows.remove(i); save(); render() }
                        .setNegativeButton("إلغاء", null).show()
                    true
                }
            })
        }
    }

    private fun export() {
        if (rows.length() == 0) { Toast.makeText(this, "لا توجد سجلات", Toast.LENGTH_SHORT).show(); return }
        val sb = StringBuilder("\uFEFFرقم الشاحنة,رقم الحاوية,التاريخ,الوقت\n")
        for (i in 0 until rows.length()) {
            val o = rows.getJSONObject(i)
            sb.append("\"${o.getString("truck")}\",\"${o.getString("cont")}\",${o.getString("date")},${o.getString("time")}\n")
        }
        val f = File(cacheDir, "truck-container-log.csv").apply { writeText(sb.toString()) }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "تصدير السجل"))
    }
}
