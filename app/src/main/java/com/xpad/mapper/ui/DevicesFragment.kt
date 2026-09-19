package com.xpad.mapper.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputDevice
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.xpad.mapper.DeviceDiag
import com.xpad.mapper.PadState
import com.xpad.mapper.R
import com.xpad.mapper.databinding.FragmentDevicesBinding
import com.xpad.mapper.databinding.ItemDeviceBinding
import java.util.Locale

class DevicesFragment : Fragment() {

    private var _b: FragmentDevicesBinding? = null
    private val b get() = _b!!

    private var adapter: DeviceAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _b = FragmentDevicesBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.btnRefresh.setOnClickListener { refresh() }
        b.rvDevices.layoutManager = LinearLayoutManager(requireContext())
        adapter = DeviceAdapter { device ->
            PadState.selectedDeviceId = device.id
            PadState.selectedDeviceName = device.name
            showDetail(device)
        }
        b.rvDevices.adapter = adapter
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val devices = InputDevice.getDeviceIds()
            .map { InputDevice.getDevice(it) }
            .filterNotNull()
            .filter {
                val s = it.sources
                s and (InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_JOYSTICK or InputDevice.SOURCE_DPAD) != 0
            }
            .sortedBy { it.name.lowercase() }

        val profileStore = com.xpad.mapper.ProfileStore(requireContext())

        val items = devices.map { dev ->
            val diag = DeviceDiag.diagnose(dev)
            DeviceItem(
                device = dev,
                diag = diag,
                hasProfile = profileStore.load(dev.id) != null,
                selected = dev.id == PadState.selectedDeviceId
            )
        }.sortedByDescending { it.selected == true }

        adapter?.submit(items)

        if (devices.isEmpty()) {
            b.tvEmpty.visibility = View.VISIBLE
            b.rvDevices.visibility = View.GONE
        } else {
            b.tvEmpty.visibility = View.GONE
            b.rvDevices.visibility = View.VISIBLE
        }
    }

    private fun showDetail(device: android.view.InputDevice) {
        val keys = intArrayOf(
            android.view.KeyEvent.KEYCODE_BUTTON_A,
            android.view.KeyEvent.KEYCODE_BUTTON_B,
            android.view.KeyEvent.KEYCODE_BUTTON_X,
            android.view.KeyEvent.KEYCODE_BUTTON_Y,
            android.view.KeyEvent.KEYCODE_BUTTON_L1,
            android.view.KeyEvent.KEYCODE_BUTTON_R1,
            android.view.KeyEvent.KEYCODE_BUTTON_START,
            android.view.KeyEvent.KEYCODE_BUTTON_SELECT
        )
        val supported = device.hasKeys(*keys)
        val sb = StringBuilder()
        sb.append("ID: ").append(device.id).append('\n')
        sb.append("Vendor/Product: ")
            .append(String.format(Locale.US, "0x%04X", device.vendorId))
            .append(" / ")
            .append(String.format(Locale.US, "0x%04X", device.productId))
            .append('\n')
        sb.append("Источники: ").append(DeviceDiag.sourcesLabel(device)).append('\n')
        sb.append('\n').append("Стандартные кнопки Xbox:")
            .append(if (supported.count { it } > 0) "\n" else " нет\n")
        val names = arrayOf("A", "B", "X", "Y", "L1", "R1", "Start", "Select")
        for (i in supported.indices) {
            if (supported[i]) sb.append("  ✓ ").append(names[i]).append('\n')
        }
        sb.append('\n').append(DeviceDiag.stdAxisCheck(device)).append('\n')
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(device.name)
            .setMessage(sb.toString().trimEnd())
            .setPositiveButton("Понятно", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }

    data class DeviceItem(
        val device: android.view.InputDevice,
        val diag: DeviceDiag.DiagResult,
        val hasProfile: Boolean,
        val selected: Boolean?
    )

    inner class DeviceAdapter(
        private val onClick: (android.view.InputDevice) -> Unit
    ) : RecyclerView.Adapter<DeviceAdapter.VH>() {

        private val items = mutableListOf<DeviceItem>()

        fun submit(newItems: List<DeviceItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val vb = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(vb)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val it = items[position]
            val vb = holder.vb
            vb.tvName.text = it.device.name
            vb.tvId.text = String.format(
                Locale.US, "ID %d · VID 0x%04X · PID 0x%04X",
                it.device.id, it.device.vendorId, it.device.productId
            )
            vb.tvSources.text = "Источники: " + DeviceDiag.sourcesLabel(it.device)
            vb.tvBadge.text = it.diag.badgeText
            vb.tvBadge.setBackgroundColor(it.diag.badgeColor)
            vb.tvVerdict.text = it.diag.verdict
            vb.tvTip.text = it.diag.tip
            vb.badgeProfile.visibility = if (it.hasProfile) View.VISIBLE else View.GONE
            vb.tvSelected.visibility = if (it.selected == true) View.VISIBLE else View.GONE
            vb.root.setOnClickListener { onClick(it.device) }
        }

        inner class VH(val vb: ItemDeviceBinding) : RecyclerView.ViewHolder(vb.root)
    }
}