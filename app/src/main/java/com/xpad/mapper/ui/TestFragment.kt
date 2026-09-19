package com.xpad.mapper.ui

import android.content.Context
import android.hardware.input.InputManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.InputDevice
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.xpad.mapper.AxisUtil
import com.xpad.mapper.GamepadTestView
import com.xpad.mapper.KeyNames
import com.xpad.mapper.LogicalButton
import com.xpad.mapper.Mapper
import com.xpad.mapper.PadState
import com.xpad.mapper.ProfileStore
import com.xpad.mapper.R
import com.xpad.mapper.databinding.FragmentTestBinding

class TestFragment : Fragment() {

    private var _b: FragmentTestBinding? = null
    private val b get() = _b!!

    private var mapper = Mapper(null)
    private var deviceFilter: Int = -1

    private val inputDeviceListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = fillSpinner()
        override fun onInputDeviceRemoved(deviceId: Int) = fillSpinner()
        override fun onInputDeviceChanged(deviceId: Int) = fillSpinner()
    }

    private val logLines = ArrayDeque<String>()
    private var hatX = 0f
    private var hatY = 0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentTestBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.padView.reset()
        fillSpinner()

        b.spinnerDevice.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val dev = parent?.getItemAtPosition(position) as? DeviceEntry ?: return
                deviceFilter = dev.device.id
                PadState.selectedDeviceId = dev.device.id
                PadState.selectedDeviceName = dev.device.name
                val prof = ProfileStore(requireContext()).load(dev.device.id)
                mapper = Mapper(prof)
                b.tvActiveProfile.text = if (prof != null)
                    "Профиль: ${prof.deviceName}"
                else
                    "Профиль не назначен — стандартная раскладка"
                b.padInput.requestGamepadFocus()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        b.padInput.listener = object : GamepadTestView.Listener {
            override fun onButtonPress(keyCode: Int, action: Int) {
                handleButton(keyCode, action)
            }

            override fun onAxisMove(event: MotionEvent, device: InputDevice) {
                handleAxes(event, device)
            }
        }

        b.btnFocus.setOnClickListener { b.padInput.requestGamepadFocus() }
        b.btnClear.setOnClickListener {
            logLines.clear()
            b.padView.reset()
            b.tvLog.text = "Ждём события..."
        }

        b.padInput.requestGamepadFocus()
        b.tvHint.text = "Выбери свой геймпад в списке (даже если он виден как «клавиатура»). Жми кнопки — события идут в лог. Если кнопки прилетают как клавиши клавиатуры, ты увидишь «RAW … не сопоставлено»."
    }

    private fun fillSpinner() {
        val devices = externalDevices()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, devices)
        b.spinnerDevice.adapter = adapter
        val idx = devices.indexOfFirst { it.device.id == PadState.selectedDeviceId }
        if (idx >= 0) {
            b.tvEmptyTest.visibility = View.GONE
            b.spinnerDevice.setSelection(idx)
        } else if (devices.isNotEmpty()) {
            b.tvEmptyTest.visibility = View.GONE
            b.spinnerDevice.setSelection(0)
        } else {
            b.tvEmptyTest.visibility = View.VISIBLE
            b.tvActiveProfile.text = "Устройство ввода не найдено. Подключи геймпад по Bluetooth или USB — список обновится сам."
        }
    }

    /** Все внешние устройства ввода (включая «клавиатуры» — так Android видит многие геймпады). */
    private fun externalDevices(): List<DeviceEntry> =
        InputDevice.getDeviceIds()
            .map { InputDevice.getDevice(it) }
            .filterNotNull()
            .filter { dev ->
                val s = dev.sources
                val hasAny = s and (
                    InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK or
                        InputDevice.SOURCE_DPAD or InputDevice.SOURCE_KEYBOARD or
                        InputDevice.SOURCE_MOUSE
                    ) != 0
                hasAny && !dev.isVirtual
            }
            .sortedBy { it.name.lowercase() }
            .map { DeviceEntry(it) }

    private fun handleButton(keyCode: Int, action: Int) {
        val rawName = KeyNames.of(keyCode)
        val mapped = mapper.mapKey(keyCode) ?: LogicalButton.fromKeyCode(keyCode)
        if (action == MotionEvent.ACTION_DOWN) {
            if (mapped != null) {
                b.padView.setButton(mapped, true)
                log("RAW $rawName (код $keyCode) → $mapped")
            } else {
                log("RAW $rawName (код $keyCode) — не сопоставлено. Добавь ремап в профиле.")
            }
        } else {
            mapped?.let { b.padView.setButton(it, false) }
            log("$rawName — отпущено")
        }
    }

    private fun handleAxes(event: MotionEvent, device: InputDevice) {
        fun axisVal(axis: Int): Float {
            val range = device.getMotionRange(axis) ?: return 0f
            val raw = event.getAxisValue(axis)
            val norm = AxisUtil.normalize(range, raw)
            return mapper.processAxis(axis, norm)
        }

        val lx = axisVal(MotionEvent.AXIS_X)
        val ly = axisVal(MotionEvent.AXIS_Y)
        var rx = axisVal(MotionEvent.AXIS_Z)
        var ry = axisVal(MotionEvent.AXIS_RZ)
        if (rx == 0f && ry == 0f) {
            rx = axisVal(MotionEvent.AXIS_RX)
            ry = axisVal(MotionEvent.AXIS_RY)
        }
        val lt = axisVal(MotionEvent.AXIS_LTRIGGER)
        val rt = axisVal(MotionEvent.AXIS_RTRIGGER)
        val hx = axisVal(MotionEvent.AXIS_HAT_X)
        val hy = axisVal(MotionEvent.AXIS_HAT_Y)

        b.padView.setLeftStick(lx, ly)
        b.padView.setRightStick(rx, ry)
        b.padView.setTriggers(lt, rt)
        updateHat(hx, hy)
    }

    private fun updateHat(hx: Float, hy: Float) {
        if (hx == hatX && hy == hatY) return
        hatX = hx; hatY = hy
        val left = hx < -0.3f
        val right = hx > 0.3f
        val up = hy < -0.3f
        val down = hy > 0.3f
        b.padView.setButton(LogicalButton.DPAD_LEFT, left)
        b.padView.setButton(LogicalButton.DPAD_RIGHT, right)
        b.padView.setButton(LogicalButton.DPAD_UP, up)
        b.padView.setButton(LogicalButton.DPAD_DOWN, down)
    }

    private fun log(msg: String) {
        logLines.addLast(msg)
        if (logLines.size > 12) logLines.removeFirst()
        b.tvLog.text = logLines.joinToString("\n")
    }

    override fun onResume() {
        super.onResume()
        val im = requireContext().getSystemService(Context.INPUT_SERVICE) as InputManager
        im.registerInputDeviceListener(inputDeviceListener, null)
        b.padInput.requestGamepadFocus()
        fillSpinner()
    }

    override fun onPause() {
        super.onPause()
        val im = requireContext().getSystemService(Context.INPUT_SERVICE) as InputManager
        im.unregisterInputDeviceListener(inputDeviceListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    data class DeviceEntry(val device: InputDevice) {
        override fun toString(): String = device.name
    }
}