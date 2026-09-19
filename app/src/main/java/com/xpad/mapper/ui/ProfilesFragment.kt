package com.xpad.mapper.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.InputDevice
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.xpad.mapper.AxisOptions
import com.xpad.mapper.ControllerProfile
import com.xpad.mapper.DeviceDiag
import com.xpad.mapper.GamepadTestView
import com.xpad.mapper.KeyNames
import com.xpad.mapper.LogicalButton
import com.xpad.mapper.PadState
import com.xpad.mapper.ProfileStore
import com.xpad.mapper.R
import com.xpad.mapper.RootHelper
import com.xpad.mapper.databinding.FragmentProfilesBinding
import com.xpad.mapper.databinding.ItemProfileBinding

class ProfilesFragment : Fragment() {

    private var _b: FragmentProfilesBinding? = null
    private val b get() = _b!!

    private var adapter: ProfileAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentProfilesBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.rvProfiles.layoutManager = LinearLayoutManager(requireContext())
        adapter = ProfileAdapter { profile ->
            showEditor(profile)
        }
        b.rvProfiles.adapter = adapter

        b.btnNewProfile.setOnClickListener {
            val devices = gamepadDevices()
            if (devices.isEmpty()) {
                Snackbar.make(b.root, "Сначала подключи геймпад", Snackbar.LENGTH_LONG).show()
            } else {
                val names = devices.map { it.name }.toTypedArray()
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Создать профиль для...")
                    .setItems(names) { _, which ->
                        val dev = devices[which]
                        PadState.selectedDeviceId = dev.id
                        showEditor(ControllerProfile(dev.name, dev.id, dev.vendorId, dev.productId))
                    }
                    .show()
            }
        }

        b.tvRootInfo.text = if (RootHelper.isRootAvailable())
            "Root: доступен. Глобальный ремап через vHID-драйвер возможен, но работает не на всех прошивках."
        else
            "Root: не доступен. Android не позволяет приложению подменять кнопки твоего геймпада для всех игр сразу. Профили применяются в вкладке «Тест» (проверка раскладки) и в советах по настройке."

        refresh()
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val store = ProfileStore(requireContext())
        val all = store.loadAll().sortedByDescending { it.createdAt }
        adapter?.submit(all)
        b.tvEmpty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
        b.rvProfiles.visibility = if (all.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun gamepadDevices(): List<android.view.InputDevice> =
        InputDevice.getDeviceIds()
            .map { InputDevice.getDevice(it) }
            .filterNotNull()
            .filter {
                val s = it.sources
                s and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_DPAD) != 0
            }

    // ---------------- Редактор ----------------

    private fun showEditor(profile: ControllerProfile) {
        val store = ProfileStore(requireContext())
        var working = profile
        var captureFor: LogicalButton? = null

        val scope = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16))
        }

        val hint = TextView(requireContext()).apply {
            text = "Для кнопки нажми ⟳ и затем физическую кнопку на твоём геймпаде."
            setTextColor(0xFF9E9E9E.toInt())
            textSize = 13f
        }
        val captureInfo = TextView(requireContext()).apply {
            text = ""
            textSize = 14f
            gravity = Gravity.CENTER
        }
        scope.addView(hint)
        scope.addView(captureInfo)

        val padInput = GamepadTestView(requireContext())
        padInput.layoutParams = LinearLayout.LayoutParams(1, 1)
        scope.addView(padInput)

        // Список сопоставлений кнопок
        val btnSectionLabel = TextView(requireContext()).apply {
            text = "КНОПКИ (ремап):"
            textSize = 14f
            setPadding(dp(0), dp(12), dp(0), dp(4))
            setTextColor(0xFF66BB6A.toInt())
        }
        scope.addView(btnSectionLabel)

        val buttonsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun renderButtons() {
            buttonsContainer.removeAllViews()
            LogicalButton.entries.forEach { logical ->
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(0), dp(3), dp(0), dp(3))
                }
                val tv = TextView(requireContext()).apply {
                    text = String.format("%-3s", logical.labelRu)
                    textSize = 15f
                    setPadding(0, 0, dp(8), 0)
                }
                val mapped = working.buttonMap.entries.firstOrNull { it.value == logical }
                val cur = TextView(requireContext()).apply {
                    text = if (mapped != null) "← ${KeyNames.of(mapped.key)} (${mapped.key})"
                    else "← по умолчанию"
                    textSize = 13f
                    setTextColor(if (mapped != null) 0xFF80CBC4.toInt() else 0xFF9E9E9E.toInt())
                }
                val setBtn = MaterialButton(requireContext()).apply {
                    text = "⟳"
                    textSize = 14f
                    minWidth = dp(44)
                }
                val clearBtn = MaterialButton(requireContext()).apply {
                    text = "✕"
                    textSize = 14f
                    minWidth = dp(44)
                }
                setBtn.setOnClickListener {
                    captureFor = logical
                    captureInfo.text = "Нажми физическую кнопку → для «${logical.labelRu}»"
                    padInput.requestGamepadFocus()
                }
                clearBtn.setOnClickListener {
                    working = working.copy(buttonMap = working.buttonMap.filterValues { it != logical })
                    renderButtons()
                }
                row.addView(tv)
                row.addView(cur, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(setBtn)
                row.addView(clearBtn)
                buttonsContainer.addView(row)
            }
        }
        renderButtons()
        scope.addView(buttonsContainer)

        // Оси
        val axisSectionLabel = TextView(requireContext()).apply {
            text = "СТИКИ И ТРИГГЕРЫ:"
            textSize = 14f
            setPadding(dp(0), dp(14), dp(0), dp(4))
            setTextColor(0xFF66BB6A.toInt())
        }
        scope.addView(axisSectionLabel)

        val axisDefs = listOf(
            MotionEvent.AXIS_X to "Левый стик X",
            MotionEvent.AXIS_Y to "Левый стик Y",
            MotionEvent.AXIS_Z to "Правый стик X",
            MotionEvent.AXIS_RZ to "Правый стик Y",
            MotionEvent.AXIS_LTRIGGER to "Триггер L2",
            MotionEvent.AXIS_RTRIGGER to "Триггер R2"
        )
        val axisContainer = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        fun renderAxes() {
            axisContainer.removeAllViews()
            axisDefs.forEach { (axis, name) ->
                val opts = working.axisOptions[axis] ?: AxisOptions()
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(0), dp(6), dp(0), dp(2))
                }
                val nameTv = TextView(requireContext()).apply { text = name; textSize = 14f }
                row.addView(nameTv)

                val dzh = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(0), dp(2), dp(0), dp(2))
                }
                val dzLbl = TextView(requireContext()).apply {
                    text = "Дед-зона ${(opts.deadZone * 100).toInt()}%"
                    textSize = 13f
                    setPadding(0, 0, dp(8), 0)
                }
                val seek = android.widget.SeekBar(requireContext())
                seek.max = 50
                seek.progress = (opts.deadZone * 100).toInt().coerceIn(0, 50)
                seek.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                        dzLbl.text = "Дед-зона ${progress}%"
                        working = working.copy(
                            axisOptions = working.axisOptions + (axis to opts.copy(deadZone = progress / 100f))
                        )
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
                dzh.addView(dzLbl)
                dzh.addView(seek, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                row.addView(dzh)

                val optsRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                val iv = MaterialSwitch(requireContext()).apply {
                    text = "Инверсия"
                    isChecked = opts.invert
                }
                val ex = MaterialSwitch(requireContext()).apply {
                    text = "Кривая (экспонента)"
                    isChecked = opts.expCurve
                }
                iv.setOnCheckedChangeListener { _, checked ->
                    working = working.copy(
                        axisOptions = working.axisOptions + (axis to opts.copy(invert = checked))
                    )
                }
                ex.setOnCheckedChangeListener { _, checked ->
                    working = working.copy(
                        axisOptions = working.axisOptions + (axis to opts.copy(expCurve = checked))
                    )
                }
                optsRow.addView(iv)
                optsRow.addView(ex)
                row.addView(optsRow)
                axisContainer.addView(row)
            }
        }
        renderAxes()
        scope.addView(axisContainer)

        // ——— приём нажатий для обучения ремапа ———
        padInput.listener = object : GamepadTestView.Listener {
            override fun onButtonPress(keyCode: Int, action: Int) {
                if (captureFor != null && action == MotionEvent.ACTION_DOWN) {
                    val target = captureFor ?: return
                    working = working.copy(buttonMap = working.buttonMap + (keyCode to target))
                    captureFor = null
                    captureInfo.text = "Запомнено: ${KeyNames.of(keyCode)} → ${target.labelRu}"
                    renderButtons()
                }
            }

            override fun onAxisMove(event: MotionEvent, device: InputDevice) {}
        }
        padInput.requestGamepadFocus()

        // Scroll
        val scroll = android.widget.ScrollView(requireContext())
        scroll.addView(scope)
        val height = dp(420)
        scroll.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height)

        val out = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        out.addView(scroll)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Профиль: ${working.deviceName}")
            .setView(out)
            .setPositiveButton("Сохранить") { _, _ ->
                store.save(working)
                Snackbar.make(b.root, "Профиль сохранён", Snackbar.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Отмена", null)
            .setNeutralButton("Удалить", null)
            .create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        store.delete(working.deviceId)
                        refresh()
                        dismiss()
                    }
                }
            }
            .show()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroyView() { super.onDestroyView(); _b = null }

    inner class ProfileAdapter(
        private val onClick: (ControllerProfile) -> Unit
    ) : RecyclerView.Adapter<ProfileAdapter.VH>() {

        private val items = mutableListOf<ControllerProfile>()

        fun submit(list: List<ControllerProfile>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val vb = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(vb)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = items[position]
            val vb = holder.vb
            vb.tvName.text = p.deviceName
            vb.tvInfo.text = "Кнопок: ${p.buttonMap.size} · Настроек осей: ${p.axisOptions.size} · ID ${p.deviceId}"
            vb.root.setOnClickListener { onClick(p) }
        }

        inner class VH(val vb: ItemProfileBinding) : RecyclerView.ViewHolder(vb.root)
    }
}