
package com.example.toquesos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.toquesos.databinding.ItemContactBinding

class EmergencyContactsAdapter(
    private val contacts: List<EmergencyContact>,
    private val onRemoveClick: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<EmergencyContactsAdapter.ContactViewHolder>() {

    class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]

        holder.binding.apply {
            textViewName.text = contact.name
            textViewPhone.text = contact.phone  // Corregido: era phoneNumber
            textViewType.text = when (contact.type) {  // Corregido: era contactType
                ContactType.CALL -> "📞 Llamar"
                ContactType.SMS -> "💬 SMS"
                ContactType.BOTH -> "📞💬 Ambos"
            }

            buttonRemove.setOnClickListener {
                onRemoveClick(contact)
            }
        }
    }

    override fun getItemCount() = contacts.size
}