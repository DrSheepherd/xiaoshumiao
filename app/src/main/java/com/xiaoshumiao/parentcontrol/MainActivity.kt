package com.xiaoshumiao.parentcontrol

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.xiaoshumiao.parentcontrol.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            proceedLockAfterNotif()
        } else {
            Toast.makeText(this, "需要通知权限以在锁定时保持限制生效", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.seekVolume.setOnSeekBarChangeListener(
            SimpleSeekListener { binding.textVolumePct.text = "${binding.seekVolume.progress}%" }
        )
        binding.seekBrightness.setOnSeekBarChangeListener(
            SimpleSeekListener { binding.textBrightnessPct.text = "${binding.seekBrightness.progress}%" }
        )
        binding.textVolumePct.text = "${binding.seekVolume.progress}%"
        binding.textBrightnessPct.text = "${binding.seekBrightness.progress}%"
        binding.btnLock.setOnClickListener { onLockClicked() }
        binding.btnUnlock.setOnClickListener { showUnlockDialog() }
        refreshUiState()
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.isLocked(this)) {
            LockEnforcementService.start(this)
        }
        refreshUiState()
    }

    private fun refreshUiState() {
        val locked = Prefs.isLocked(this)
        if (locked) {
            binding.titleText.setText(R.string.title_locked)
            binding.panelUnlocked.visibility = android.view.View.GONE
            binding.panelLocked.visibility = android.view.View.VISIBLE
            val v = Prefs.getMaxVolumePct(this)
            val b = Prefs.getMaxBrightnessPct(this)
            binding.textLockedHint.text =
                "当前上限：最大媒体音量 ${v}%，最大亮度 ${b}%。\n媒体音量调高若超过上限会自动压回；铃声、通知、闹钟音量不受限制。"
        } else {
            binding.titleText.setText(R.string.title_unlocked)
            binding.panelUnlocked.visibility = android.view.View.VISIBLE
            binding.panelLocked.visibility = android.view.View.GONE
            binding.seekVolume.progress = Prefs.getMaxVolumePct(this)
            binding.seekBrightness.progress = Prefs.getMaxBrightnessPct(this)
            binding.textVolumePct.text = "${binding.seekVolume.progress}%"
            binding.textBrightnessPct.text = "${binding.seekBrightness.progress}%"
        }
    }

    private fun onLockClicked() {
        val vol = binding.seekVolume.progress.coerceIn(1, 100)
        val bri = binding.seekBrightness.progress.coerceIn(1, 100)
        Prefs.setLimits(this, vol, bri)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        proceedLockAfterNotif()
    }

    private fun proceedLockAfterNotif() {
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, R.string.msg_need_write_settings, Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            return
        }
        Prefs.setLocked(this, true)
        LockEnforcementService.start(this)
        LimitHelper.clampMediaVolume(this, Prefs.getMaxVolumePct(this))
        LimitHelper.clampBrightness(this, Prefs.getMaxBrightnessPct(this))
        Toast.makeText(this, R.string.msg_lock_ok, Toast.LENGTH_SHORT).show()
        refreshUiState()
    }

    private fun showUnlockDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.hint_unlock_password)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.btn_unlock)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                if (input.text.toString() == UNLOCK_PASSWORD) {
                    Prefs.setLocked(this, false)
                    LockEnforcementService.stop(this)
                    Toast.makeText(this, "已解锁", Toast.LENGTH_SHORT).show()
                    refreshUiState()
                } else {
                    Toast.makeText(this, R.string.msg_wrong_password, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        private const val UNLOCK_PASSWORD = "4412"
    }
}

private class SimpleSeekListener(private val onChange: () -> Unit) :
    android.widget.SeekBar.OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
        onChange()
    }

    override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
    override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
}
