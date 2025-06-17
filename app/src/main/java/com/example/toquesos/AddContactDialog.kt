package com.example.toquesos

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.toquesos.databinding.DialogAddContactBinding

class AddContactDialog(
    private val onContactAdded: (EmergencyContact) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogAddContactBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogAddContactBinding.inflate(layoutInflater)

        val contactTypes = arrayOf("Llamar", "SMS", "Ambos")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, contactTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerContactType.adapter = adapter

        return AlertDialog.Builder(requireContext())
            .setTitle("Agregar Contacto de Emergencia")
            .setView(binding.root)
            .setPositiveButton("Agregar") { _, _ ->
                val name = binding.editTextName.text.toString().trim()
                val phone = binding.editTextPhone.text.toString().trim()
                val typePosition = binding.spinnerContactType.selectedItemPosition

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val contactType = when (typePosition) {
                        0 -> ContactType.CALL
                        1 -> ContactType.SMS
                        else -> ContactType.BOTH
                    }

                    val contact = EmergencyContact(name, phone, contactType)
                    onContactAdded(contact)
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
    }
}
