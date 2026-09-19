package com.xpad.mapper

import android.content.Context
import android.view.InputDevice
import org.json.JSONArray
import org.json.JSONObject

/** Хранилище профилей в SharedPreferences (JSON). */
class ProfileStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("xpad_profiles", Context.MODE_PRIVATE)
    private val lastDevice = prefs.getInt(KEY_LAST_DEVICE, -1)

    fun save(profile: ControllerProfile) {
        val all = loadAll().filter { it.deviceId != profile.deviceId } + profile
        persist(all, profile.deviceId)
    }

    fun load(deviceId: Int): ControllerProfile? = loadAll().firstOrNull { it.deviceId == deviceId }

    fun delete(deviceId: Int) {
        persist(loadAll().filter { it.deviceId != deviceId }, lastDevice)
    }

    fun lastSelectedDevice(): Int = lastDevice

    fun loadAll(): List<ControllerProfile> {
        val json = prefs.getString(KEY_DATA, "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i -> fromJson(arr.getJSONObject(i)) }
        }.getOrDefault(emptyList())
    }

    private fun persist(list: List<ControllerProfile>, lastId: Int) {
        val arr = JSONArray()
        list.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_DATA, arr.toString()).putInt(KEY_LAST_DEVICE, lastId).apply()
    }

    private fun toJson(p: ControllerProfile): JSONObject {
        val o = JSONObject()
        o.put("deviceName", p.deviceName)
        o.put("deviceId", p.deviceId)
        o.put("vendor", p.vendor)
        o.put("product", p.product)
        o.put("createdAt", p.createdAt)

        val bm = JSONObject()
        p.buttonMap.forEach { (k, b) -> bm.put(k.toString(), b.name) }
        o.put("buttonMap", bm)

        val am = JSONObject()
        p.axisOptions.forEach { (axis, opt) ->
            am.put(axis.toString(), JSONObject().apply {
                put("deadZone", opt.deadZone.toDouble())
                put("invert", opt.invert)
                put("expCurve", opt.expCurve)
            })
        }
        o.put("axisOptions", am)
        return o
    }

    private fun fromJson(o: JSONObject): ControllerProfile? = runCatching {
        val bm = mutableMapOf<Int, LogicalButton>()
        val bj = o.optJSONObject("buttonMap") ?: JSONObject()
        bj.keys().forEach { k ->
            LogicalButton.fromConstName(bj.getString(k))?.let { bm[k.toInt()] = it }
        }
        val am = mutableMapOf<Int, AxisOptions>()
        val aj = o.optJSONObject("axisOptions") ?: JSONObject()
        aj.keys().forEach { k ->
            val j = aj.getJSONObject(k)
            am[k.toInt()] = AxisOptions(
                deadZone = j.optDouble("deadZone", 0.12).toFloat(),
                invert = j.optBoolean("invert", false),
                expCurve = j.optBoolean("expCurve", false)
            )
        }
        ControllerProfile(
            deviceName = o.getString("deviceName"),
            deviceId = o.getInt("deviceId"),
            vendor = o.optInt("vendor"),
            product = o.optInt("product"),
            buttonMap = bm,
            axisOptions = am,
            createdAt = o.optLong("createdAt", 0L)
        )
    }.getOrNull()

    companion object {
        private const val KEY_DATA = "profiles_json"
        private const val KEY_LAST_DEVICE = "last_device"

        fun findMatchById(device: InputDevice): ControllerProfile? = null // не используется, см. load()
    }
}