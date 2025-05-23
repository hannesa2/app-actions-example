package io.spokestack.actions

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.SwitchCompat
import io.spokestack.actions.databinding.ActivityDeviceControlBinding
import io.spokestack.spokestack.SpokestackAdapter
import io.spokestack.spokestack.nlu.NLUResult
import io.spokestack.spokestack.tts.SynthesisRequest

/**
 * An activity that mimics control of a few smart home
 * devices, making the controls available via voice.
 */
class DeviceControlActivity : VoiceActivity() {

    private lateinit var binding: ActivityDeviceControlBinding
    private val deviceMap: HashMap<String, SwitchCompat> = HashMap()
    private val commandMap: HashMap<String, Boolean> = HashMap()

    override fun createListener(): SpokestackAdapter {
        return Listener()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceControlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        populateVoiceMaps()
    }

    override fun onResume() {
        super.onResume()
        setUiFromIntent(intent?.data)
    }

    private fun populateVoiceMaps() {
        // This is a somewhat naive way of mapping synonyms onto a
        // single canonical term.
        //
        // Other ways, depending on use case, include setting up a phonetic
        // search index or allowing users to configure their own aliases.
        //
        // Spokestack's NLU also lets you set up normalization like this
        // directly in slot configuration, but we've left it here so that
        // both App Action and Spokestack intents go through the same logic.
        deviceMap["office light"] = binding.officeLightSwitch
        deviceMap["office"] = binding.officeLightSwitch
        deviceMap["kitchen light"] = binding.kitchenLightSwitch
        deviceMap["kitchen"] = binding.kitchenLightSwitch
        deviceMap["office fan"] = binding.officeFanSwitch
        deviceMap["fan"] = binding.officeFanSwitch
        commandMap["on"] = true
        commandMap["off"] = false
    }

    private fun setUiFromIntent(data: Uri?) {
        if (data == null) {
            return
        }

        // if we know we've gotten here via voice command, respond via voice
        respond()

        val device = resolveDevice(data.getQueryParameter("device"))
        val on = resolveCommand(data.getQueryParameter("command"))

        runOnUiThread {
            device?.let { switch ->
                on?.let {
                    switch.isChecked = on
                }
            }
        }
    }

    private fun respond() {
        val synthesisRequest = SynthesisRequest.Builder("Got it!").build()
        spokestack.synthesize(synthesisRequest)
    }

    private fun resolveDevice(deviceSlot: String?): SwitchCompat? {
        if (deviceSlot == null) {
            return null
        }

        return deviceMap[deviceSlot]
    }

    private fun resolveCommand(commandSlot: String?): Boolean? {
        if (commandSlot == null) {
            return null
        }

        // default to "on"
        return commandMap[commandSlot] ?: true
    }

    /**
     * Voice activation listener
     *
     * This NLU listener only responds to a single intent, logging all
     * others. A real application should provide more user feedback
     * than this.
     */
    inner class Listener : SpokestackAdapter() {
        override fun nluResult(result: NLUResult) {
            if (result.intent == "command.control_device") {
                val dataUri = Uri.Builder()
                    .appendQueryParameter("device", result.slots["device"]?.rawValue)
                    .appendQueryParameter("command", result.slots["command"]?.rawValue)
                    .build()
                setUiFromIntent(dataUri)
            } else {
                Log.i(logTag, "Unsupported intent: ${result.intent}")
            }
        }
    }
}