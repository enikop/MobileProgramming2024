package com.example.budgettracker.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.budgettracker.MainActivity
import com.example.budgettracker.R
import com.example.budgettracker.adapter.BudgetTrackerAdapter
import com.example.budgettracker.database.AppDatabase
import com.example.budgettracker.dialog.TransactionDialog
import com.example.budgettracker.model.Transaction
import com.example.budgettracker.touch.TransactionTouchHelperCallback
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment(private var adapter: BudgetTrackerAdapter) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
    }

    private fun initRecyclerView() {
        activity?.runOnUiThread {
            recyclerShopping.adapter = adapter

            val callback = TransactionTouchHelperCallback(adapter)
            val touchHelper = ItemTouchHelper(callback)
            touchHelper.attachToRecyclerView(recyclerShopping)
        }
    }

    fun showEditItemDialog(itemToEdit: Transaction) {
        val editDialog = TransactionDialog()
        val bundle = Bundle()
        bundle.putSerializable(MainActivity.KEY_ITEM_TO_EDIT, itemToEdit)
        editDialog.arguments = bundle
        editDialog.show(parentFragmentManager, "TAG_ITEM_EDIT")
    }
}