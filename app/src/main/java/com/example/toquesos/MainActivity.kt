package com.example.toquesos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toquesos.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var emergencyContactsAdapter: EmergencyContactsAdapter
    private val emergencyContacts = mutableListOf<EmergencyContact>()

    private val gson = Gson()
    private val sharedPrefs by lazy {
        getSharedPreferences("emergency_prefs", Context.MODE_PRIVATE)
    }

    // Handler para manejar delays entre llamadas
    private val handler = Handler(Looper.getMainLooper())

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            setupBiometricAuthentication()
        } else {
            Toast.makeText(this, "Se necesitan permisos para usar la app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        requestPermissions()
        loadContacts()

        binding.btnAddContact.setOnClickListener {
            showAddContactDialog()
        }

        binding.btnActivateSos.setOnClickListener {
            if (emergencyContacts.isNotEmpty()) {
                activateBiometricSOS()
            } else {
                Toast.makeText(this, "Agrega al menos un contacto de emergencia", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        emergencyContactsAdapter = EmergencyContactsAdapter(emergencyContacts) { contact ->
            removeContact(contact)
        }
        binding.recyclerViewContacts.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = emergencyContactsAdapter
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.USE_BIOMETRIC
        )
        requestMultiplePermissions.launch(permissions)
    }

    private fun setupBiometricAuthentication() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> { }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "No hay sensor biométrico disponible", Toast.LENGTH_LONG).show()
                return
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Sensor biométrico no disponible", Toast.LENGTH_LONG).show()
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "No hay huellas registradas", Toast.LENGTH_LONG).show()
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Toast.makeText(this@MainActivity, "Error: $errString", Toast.LENGTH_SHORT).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                handleEmergency()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, "Huella no reconocida", Toast.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("🆘 EMERGENCIA SOS")
            .setSubtitle("Coloca tu dedo para activar la emergencia")
            .setDescription("Se contactará a tus contactos de emergencia")
            .setNegativeButtonText("Cancelar")
            .build()
    }

    private fun activateBiometricSOS() {
        if (::biometricPrompt.isInitialized) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(this, "Configurando autenticación biométrica...", Toast.LENGTH_SHORT).show()
            setupBiometricAuthentication()
        }
    }

    private fun handleEmergency() {
        Toast.makeText(this, "🆘 EMERGENCIA ACTIVADA", Toast.LENGTH_LONG).show()
        val location = getCurrentLocation()
        val emergencyMessage = buildEmergencyMessage(location)

        // OPCIÓN 1: Mostrar selector de contactos para llamar (RECOMENDADO)
        showCallContactSelector()

        // Enviar SMS a todos los contactos inmediatamente
        for (contact in emergencyContacts) {
            when (contact.type) {
                ContactType.SMS -> sendEmergencySMS(contact.phone, emergencyMessage)
                ContactType.BOTH -> sendEmergencySMS(contact.phone, emergencyMessage)
                ContactType.CALL -> {} // Solo manejamos llamadas en el selector
            }
        }
    }

    // OPCIÓN 1: Mostrar selector para que el usuario elija a quién llamar
    private fun showCallContactSelector() {
        val callContacts = emergencyContacts.filter {
            it.type == ContactType.CALL || it.type == ContactType.BOTH
        }

        if (callContacts.isEmpty()) return

        val contactNames = callContacts.map { "${it.name} (${it.phone})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("🆘 Selecciona contacto para llamar")
            .setItems(contactNames) { dialog, which ->
                makeEmergencyCall(callContacts[which].phone)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // OPCIÓN 2: Llamar a todos con delay (alternativa)
    private fun callAllContactsWithDelay() {
        val callContacts = emergencyContacts.filter {
            it.type == ContactType.CALL || it.type == ContactType.BOTH
        }

        callContacts.forEachIndexed { index, contact ->
            handler.postDelayed({
                // Usar ACTION_DIAL en lugar de ACTION_CALL para que el usuario confirme
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contact.phone}")
                }
                startActivity(intent)
            }, (index * 3000).toLong()) // 3 segundos entre cada llamada
        }
    }

    // OPCIÓN 3: Hacer solo la primera llamada automática y mostrar lista para las demás
    private fun handleCallsHybrid() {
        val callContacts = emergencyContacts.filter {
            it.type == ContactType.CALL || it.type == ContactType.BOTH
        }

        if (callContacts.isNotEmpty()) {
            // Llamar automáticamente al primer contacto
            makeEmergencyCall(callContacts[0].phone)

            // Si hay más contactos, mostrar lista para llamadas adicionales
            if (callContacts.size > 1) {
                handler.postDelayed({
                    showAdditionalCallsDialog(callContacts.drop(1))
                }, 2000) // Esperar 2 segundos antes de mostrar el diálogo
            }
        }
    }

    private fun showAdditionalCallsDialog(remainingContacts: List<EmergencyContact>) {
        val contactNames = remainingContacts.map { "${it.name} (${it.phone})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("🆘 Contactos adicionales")
            .setMessage("¿Deseas llamar a otros contactos?")
            .setItems(contactNames) { dialog, which ->
                makeEmergencyCall(remainingContacts[which].phone)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun getCurrentLocation(): String {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return "Ubicación no disponible"
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) {
                return "Lat: ${location.latitude}, Lng: ${location.longitude}\nGoogle Maps: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            }
        }
        return "Ubicación no disponible"
    }

    private fun buildEmergencyMessage(location: String): String {
        return """
            🆘 EMERGENCIA SOS 🆘
            
            Esta es una alerta de emergencia automática.
            
            Ubicación actual:
            $location
            
            Por favor, contacta inmediatamente o acude a la ubicación.
            
            Enviado desde Toque SOS
        """.trimIndent()
    }

    private fun makeEmergencyCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
            }
            try {
                startActivity(intent)
                Toast.makeText(this, "Llamando a $phoneNumber", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error al llamar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendEmergencySMS(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = getSystemService(SmsManager::class.java)
                val parts = smsManager.divideMessage(message)
                if (parts.size == 1) {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                } else {
                    smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                }
                Toast.makeText(this, "SMS enviado a $phoneNumber", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error enviando SMS: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddContactDialog() {
        val dialog = AddContactDialog { contact ->
            emergencyContacts.add(contact)
            emergencyContactsAdapter.notifyDataSetChanged()
            saveContacts()
        }
        dialog.show(supportFragmentManager, "AddContactDialog")
    }

    private fun removeContact(contact: EmergencyContact) {
        emergencyContacts.remove(contact)
        emergencyContactsAdapter.notifyDataSetChanged()
        saveContacts()
    }

    private fun saveContacts() {
        val json = gson.toJson(emergencyContacts)
        sharedPrefs.edit().putString("contacts", json).apply()
    }

    private fun loadContacts() {
        val json = sharedPrefs.getString("contacts", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<EmergencyContact>>() {}.type
            val loadedContacts: MutableList<EmergencyContact> = gson.fromJson(json, type)
            emergencyContacts.clear()
            emergencyContacts.addAll(loadedContacts)
            emergencyContactsAdapter.notifyDataSetChanged()
        }
    }
}