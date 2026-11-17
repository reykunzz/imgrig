package com.example.imgrig

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Entry(var name: String, var data: ByteArray)

object Img {
    private const val SEC = 2048

    fun pad(d: ByteArray): ByteArray {
        val size = (d.size + SEC - 1) / SEC * SEC
        return if (size == d.size) d else d.copyOf(size)
    }

    fun read(b: ByteArray): MutableList<Entry> {
        require(b.size >= 8 && String(b, 0, 4) == "VER2") { "Bukan IMG VER2 (GTA SA)" }
        val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        val n = bb.getInt(4)
        val list = mutableListOf<Entry>()
        for (i in 0 until n) {
            val p = 8 + i * 32
            val off = (bb.getInt(p).toLong() and 0xFFFFFFFFL) * SEC
            val s1 = bb.getShort(p + 4).toInt() and 0xFFFF
            val s2 = bb.getShort(p + 6).toInt() and 0xFFFF
            val len = (if (s2 != 0) s2 else s1).toLong() * SEC
            var e = 0
            while (e < 24 && b[p + 8 + e].toInt() != 0) e++
            val name = String(b, p + 8, e)
            val end = minOf(off + len, b.size.toLong()).toInt()
            val start = minOf(off, b.size.toLong()).toInt()
            list.add(Entry(name, pad(b.copyOfRange(start, end))))
        }
        return list
    }

    fun write(list: List<Entry>): ByteArray {
        val n = list.size
        val hdr = (8 + 32 * n + SEC - 1) / SEC * SEC
        val h = ByteBuffer.allocate(hdr).order(ByteOrder.LITTLE_ENDIAN)
        h.put("VER2".toByteArray()); h.putInt(n)
        var sec = hdr / SEC
        for (e in list) {
            val s = e.data.size / SEC
            h.putInt(sec); h.putShort(s.toShort()); h.putShort(0)
            val nb = e.name.toByteArray()
            val nm = ByteArray(24)
            nb.copyInto(nm, 0, 0, minOf(23, nb.size))
            h.put(nm)
            sec += s
        }
        val out = ByteArrayOutputStream()
        out.write(h.array())
        for (e in list) out.write(e.data)
        return out.toByteArray()
    }

    /** Buang padding sektor untuk DFF/TXD berdasarkan header chunk RenderWare. */
    fun trim(name: String, d: ByteArray): ByteArray {
        val l = name.lowercase()
        if ((l.endsWith(".dff") || l.endsWith(".txd")) && d.size >= 12) {
            val sz = ByteBuffer.wrap(d, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val t = sz.toLong() + 12
            if (t in 12..d.size.toLong()) return d.copyOf(t.toInt())
        }
        return d
    }
}
