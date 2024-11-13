package com.example.budgettracker.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.budgettracker.MainActivity
import com.example.budgettracker.R
import com.example.budgettracker.fragments.HomeFragment
import com.example.budgettracker.helpers.DateConverter
import com.example.budgettracker.model.Currency
import com.example.budgettracker.model.Transaction
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.android.synthetic.main.dialog_create.view.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TransactionDialog(fragment: HomeFragment) : DialogFragment() {

    private var transactionHandler: TransactionHandler = fragment
    private lateinit var etLabel: EditText
    private lateinit var etAmount: EditText
    private lateinit var etNote: EditText
    private lateinit var rgIncoming: RadioGroup
    private lateinit var rbIncoming: RadioButton
    private lateinit var rbOutgoing: RadioButton
    private lateinit var etDate: EditText
    private lateinit var spCurrency: Spinner
    private var dateConverter = DateConverter()

    interface TransactionHandler {
        fun transactionCreated(item: Transaction)

        fun transactionUpdated(item: Transaction)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())

        builder.setTitle("New transaction")

        initDialogContent(builder)


        builder.setPositiveButton("OK") { _, _ ->
            // keep it empty
        }
        return builder.create()
    }

    private fun initDialogContent(builder: AlertDialog.Builder) {

        val rootView = requireActivity().layoutInflater.inflate(R.layout.dialog_create, null)

        etLabel = rootView.etLabel
        etAmount = rootView.etAmount
        rgIncoming = rootView.rgIncoming
        rbIncoming = rootView.rbIncoming
        rbOutgoing = rootView.rbOutgoing
        etNote = rootView.etNote
        etDate = rootView.etDate
        etDate.setText(dateConverter.dateToString(LocalDate.now()))
        spCurrency = rootView.spCurrency
        val currencies = Currency.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCurrency.adapter = adapter
        etDate.setOnClickListener {
            showMaterialDatePicker { selectedDate ->
                etDate.setText(dateConverter.dateToString(selectedDate))
            }
        }
        builder.setView(rootView)

        val arguments = this.arguments
        if (arguments != null &&
            arguments.containsKey(MainActivity.KEY_ITEM_TO_EDIT)) {
            val item = arguments.getSerializable(
                MainActivity.KEY_ITEM_TO_EDIT) as Transaction

            etLabel.setText(item.label)
            etAmount.setText(item.amount.toString())
            rgIncoming.check(if(item.isIncoming) rbIncoming.id else rbOutgoing.id)
            etNote.setText(item.note)
            etDate.setText(dateConverter.dateToString(item.date))
            val selectionPosition = adapter.getPosition(item.currency.name)
            spCurrency.setSelection(selectionPosition)
            builder.setTitle("Edit transaction")
        }
    }

    private fun showMaterialDatePicker(onDateSelected: (LocalDate) -> Unit) {
        val selectedDate = dateConverter.fromString(etDate.text.toString())?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: MaterialDatePicker.todayInUtcMilliseconds()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select date")
            .setSelection(selectedDate)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // Convert the selected timestamp to LocalDate
            val newSelectedDate = Instant.ofEpochMilli(selection).atZone(ZoneId.systemDefault()).toLocalDate()
            onDateSelected(newSelectedDate)
        }

        datePicker.show(requireActivity().supportFragmentManager, "MATERIAL_DATE_PICKER")
    }


    override fun onResume() {
        super.onResume()

        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)

        positiveButton.setOnClickListener {
            var isValid = true
            if(etLabel.text.isEmpty() || etLabel.text.length > 20) {
                etLabel.error = "The label can contain a maximum of 20 characters and cannot be empty."
                isValid = false
            }
            if(etAmount.text.isEmpty() || etAmount.text.toString().toDouble() <= 0) {
                etAmount.error = "The amount must be a number above 0."
                isValid = false
            }
            if(etDate.text.isEmpty()) {
                etDate.error = "The date cannot be empty."
                isValid = false
            }
            if(isValid) {
                val arguments = this.arguments
                if (arguments != null && arguments.containsKey(MainActivity.KEY_ITEM_TO_EDIT)) {
                    handleItemEdit()
                } else {
                    handleItemCreate()
                }
                dialog.dismiss()
            }
        }
    }

    private fun handleItemCreate() {
        val selectedCurrencyName = spCurrency.selectedItem as String
        val selectedCurrency = Currency.valueOf(selectedCurrencyName)
        transactionHandler.transactionCreated(
            Transaction(
                null,
                etLabel.text.toString(),
                etAmount.text.toString().toDouble(),
                rbIncoming.isChecked,
                false,
                etNote.text.toString(), dateConverter.fromString(etDate.text?.toString()) ?: LocalDate.now(),
                selectedCurrency
            )
        )
    }

    private fun handleItemEdit() {
        val itemToEdit = arguments?.getSerializable(
            MainActivity.KEY_ITEM_TO_EDIT) as Transaction
        itemToEdit.label = etLabel.text.toString()
        itemToEdit.amount = etAmount.text.toString().toDouble()
        itemToEdit.isIncoming = rbIncoming.isChecked
        itemToEdit.note= etNote.text.toString()
        val selectedCurrencyName = spCurrency.selectedItem as String
        val selectedCurrency = Currency.valueOf(selectedCurrencyName)
        itemToEdit.currency = selectedCurrency
        itemToEdit.date = dateConverter.fromString(etDate.text?.toString()) ?: LocalDate.now()

        transactionHandler.transactionUpdated(itemToEdit)
    }
}